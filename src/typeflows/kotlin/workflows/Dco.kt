package workflows

import io.typeflows.github.workflow.Job
import io.typeflows.github.workflow.Permission.Contents
import io.typeflows.github.workflow.PermissionLevel.Read
import io.typeflows.github.workflow.Permissions
import io.typeflows.github.workflow.PullRequestType.Opened
import io.typeflows.github.workflow.PullRequestType.Reopened
import io.typeflows.github.workflow.PullRequestType.Synchronize
import io.typeflows.github.workflow.RunsOn.Companion.UBUNTU_LATEST
import io.typeflows.github.workflow.Workflow
import io.typeflows.github.workflow.step.RunCommand
import io.typeflows.github.workflow.step.marketplace.Checkout
import io.typeflows.github.workflow.trigger.PullRequest
import io.typeflows.util.Builder
import org.http4k.typeflows.GithubActionConstants.CHECKOUT

class Dco : Builder<Workflow> {
    override fun build() = Workflow("dco") {
        displayName = "DCO"

        on += PullRequest {
            types += listOf(Opened, Synchronize, Reopened)
        }

        permissions = Permissions(Contents to Read)

        jobs += Job("dco", UBUNTU_LATEST) {
            // pinned: this is the required status check name in branch protection
            name = "DCO"
            timeoutMinutes = 5

            steps += Checkout(CHECKOUT) {
                // need every commit in the PR range, not just the tip
                fetchDepth = 0
                persistCredentials = false
            }

            steps += RunCommand(CHECK_SIGN_OFF) {
                name = "Check sign-off"
                env["PR_BASE"] = $$"${{ github.event.pull_request.base.sha }}"
                env["PR_HEAD"] = $$"${{ github.event.pull_request.head.sha }}"
                env["PR_BASE_REF"] = $$"${{ github.event.pull_request.base.ref }}"
            }
        }
    }
}

private val CHECK_SIGN_OFF = $$"""
    set -euo pipefail

    # Authors exempt from sign-off. Edit this list to add/remove bots.
    BOT_AUTHORS="dependabot[bot] github-actions[bot] renovate[bot]"

    if git rev-parse -q --verify HEAD^2 >/dev/null; then
      RANGE_START=$(git rev-parse HEAD^1)
      RANGE_END=$(git rev-parse HEAD^2)
    else
      RANGE_START=$(git merge-base "$PR_BASE" "$PR_HEAD")
      RANGE_END="$PR_HEAD"
    fi

    is_bot() {
      for bot in $BOT_AUTHORS; do
        [ "$1" = "$bot" ] && return 0
        # quoting $bot keeps "[bot]" literal rather than a glob character class
        case "$2" in *"$bot"*) return 0 ;; esac
      done
      return 1
    }

    failed=""
    n=0

    # --no-merges: a merge commit has >1 parent and carries no meaningful author sign-off
    for sha in $(git rev-list --no-merges "$RANGE_START..$RANGE_END"); do
      n=$((n + 1))
      author_name=$(git show -s --format='%an' "$sha")
      author_email=$(git show -s --format='%ae' "$sha")

      if is_bot "$author_name" "$author_email"; then continue; fi

      signoffs=$(git show -s --format='%(trailers:key=Signed-off-by,valueonly)' "$sha")

      if [ -z "$signoffs" ]; then
        failed="${failed}  ${sha}  ${author_email}  - no Signed-off-by trailer
    "
        continue
      fi

      lower=$(printf '%s' "$author_email" | tr '[:upper:]' '[:lower:]')
      if printf '%s\n' "$signoffs" | grep -oE '<[^>]+>' | tr -d '<>' \
           | tr '[:upper:]' '[:lower:]' | grep -qxF "$lower"; then
        continue
      fi

      failed="${failed}  ${sha}  ${author_email}  - signed off by $(printf '%s' "$signoffs" | tr '\n' ';'), not the commit author
    "
    done

    if [ -z "$failed" ]; then
      echo "All $n non-merge commit(s) carry a valid Signed-off-by trailer."
      exit 0
    fi

    cat <<EOF
    ::error::One or more commits are missing a valid Signed-off-by trailer.

    The following commits failed the DCO check:

    $failed
    Every commit needs a trailer matching its own author:

      Signed-off-by: Your Name <your@email.example>

    First, check git is configured with the name and email you want on record:

      git config user.name  "Your Name"
      git config user.email "your@email.example"

    Then sign off the commits you already have.

      For a single commit:
        git commit --amend --signoff

      For more than one:
        git rebase --signoff origin/$PR_BASE_REF

    Finally, force-push the branch (this rewrites history on your branch only):

        git push --force-with-lease

    From now on use 'git commit -s' to sign off as you go.
    See CONTRIBUTING.md and https://developercertificate.org for what you are certifying.
    EOF
    exit 1
""".trimIndent()
