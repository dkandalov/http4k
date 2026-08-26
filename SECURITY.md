# Security Policy

## Reporting a Vulnerability

Please report potential security vulnerabilities in the http4k ecosystem privately — do **not**
open a public issue or pull request, as that discloses the vulnerability before a fix is available.

Report either through GitHub's
[private vulnerability reporting](https://github.com/http4k/http4k/security/advisories/new), or as
described in our [Coordinated Vulnerability Disclosure policy](https://www.http4k.org/cvd-policy/),
which explains how to report, what to expect, and our disclosure timelines.

## Vulnerability identifiers: advisories and CVEs

For every confirmed vulnerability we publish a GitHub Security Advisory (GHSA) against the affected
repository. The GHSA is our canonical disclosure record: it is carried by the GitHub Advisory
Database, OSV, Dependabot and the major SCA tools, so downstream consumers are notified and can
remediate whether or not a CVE identifier has also been assigned.

We request a CVE identifier where assignment is warranted and adds value to consumers. That is
assessed per advisory, taking into account:

- whether the issue is reachable by an untrusted party against a supported configuration;
- the assessed severity (CVSS v3.1);
- whether the change fixes a defect, or hardens a default that requires a specific application-side
  configuration before it has any effect;
- whether downstream consumers need a CVE identifier to track remediation in their own tooling.

Where we decide not to request a CVE, the reasoning is recorded on the advisory. 

### Requests to assign a CVE to an already-published advisory

We receive requests asking us to request a CVE for an advisory that is already public, frequently in
bulk and without new technical content. For clarity on how these are handled:

- Requests containing no new technical information are closed as duplicates of the existing advisory,
  without individual re-assessment.
- Requests that demonstrate impact we had not accounted for — a reproduction, a working attack path
  against a supported configuration, or a materially different impact assessment — are very welcome.
  We will reassess promptly and request a CVE where the reassessment warrants it.
- Credit on an advisory is given for discovery of a vulnerability, or for material contribution to
  its analysis or remediation. We do not add attribution for administrative or coordination requests
  relating to advisories that are already public.

Nothing in this policy prevents a third party from seeking a CVE identifier directly from MITRE or
another CNA; we simply do not undertake to request one on their behalf.

## Verifying releases

See [VERIFYING.md](VERIFYING.md) for instructions on verifying the integrity, authenticity, and
signer identity of released artifacts.

## Security expectations for users

http4k is an HTTP toolkit; the security of an application built with it depends on how it is used.
Users **can** expect that:

- http4k implements no cryptography of its own — TLS and related primitives are delegated to the
  JVM / standard client stacks, which perform certificate verification by default.
- Inputs are parsed through typed Lenses that validate presence and type (an allowlist model),
  making malformed input straightforward to reject.
- Released artifacts are signed (see [VERIFYING.md](VERIFYING.md)) and the build/release pipeline is
  hardened (see [SECURITY-DESIGN.md](SECURITY-DESIGN.md)).

Users **cannot** expect http4k to:

- Secure an application automatically — authentication, authorisation, validation of business data,
  secrets management, and safe deployment remain the application's responsibility.
- Make guarantees about third-party server/client backends or user-supplied handlers and filters.
- Protect against vulnerabilities introduced in application code or by misconfiguration.

## Dependency management

We continuously monitor our dependencies for known vulnerabilities and licence issues using
Dependabot alerts and by submitting the dependency graph on every build
(`.github/workflows/security-dependabot.yml`).

**Remediation threshold.** Security fixes in third-party dependencies are applied as soon as a
fixed version is released upstream, prioritised by severity — critical/high findings are addressed
with the highest priority. Our normal approach is to fix-forward.

**Releases.** A release is not cut while there are unresolved dependency vulnerabilities above the
remediation threshold, or unresolved licence violations.

## Secrets and credentials

All secrets and credentials used by the project (publishing credentials, signing keys, API tokens)
are stored exclusively as **GitHub encrypted secrets** — Actions secrets and, for release
credentials, environment-scoped secrets — and are never committed to source control. Access is
restricted to the
[@http4k/core-maintainers](https://github.com/orgs/http4k/teams/core-maintainers) team on a
least-privilege basis (see [GOVERNANCE.md](GOVERNANCE.md)). Secrets are rotated when the maintainer
team changes or when exposure is suspected.
