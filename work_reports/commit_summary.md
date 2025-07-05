# Commit Summary

This document summarizes the changes made by Paul Datta on this branch, compared to the main branch.

## Change Categories

The commits can be categorized into the following types:

* **Security:** Direct fixes for security vulnerabilities identified by CodeQL.
* **Build:** Fixes for compilation errors and build-related issues.
* **Configuration:** Changes to workflow files, `.gitignore`, and `pom.xml`.
* **Housekeeping:** Removal of reports and other non-code changes.

## Detailed Changes

### Security Fixes

* **`java/unsafe-deserialization`**:
  * `InsecureDeserializationTask.java`: Introduced `MyObjectInputStream` to whitelist deserializable classes.
  * `VulnerableComponentsLesson.java`: Added XStream security configurations to prevent deserialization attacks.
* **`java/sql-injection`**:
  * `UserService.java`, `WebGoatUser.java`: Added username validation to prevent SQL injection in `CREATE SCHEMA`.
  * `Assignment5.java`, `JWTHeaderKIDEndpoint.java`, `SqlInjectionChallenge.java`, `SqlInjectionLesson10.java`, `SqlInjectionLesson5b.java`, `SqlInjectionLesson8.java`, `SqlInjectionLesson9.java`, `Servers.java`: Replaced string concatenation with `PreparedStatement` and parameterized queries.
* **`java/ssrf`**:
  * `SSRFTask2.java`: Replaced a regex with a strict URL whitelist to prevent Server-Side Request Forgery.
* **`java/insecure-randomness`**:
  * `JWTRefreshEndpoint.java`: Replaced `RandomStringUtils` with `java.security.SecureRandom`.
* **`java/polynomial-redos`**:
  * `SqlInjectionLesson6a.java`, `SqlInjectionLesson10b.java`, `CrossSiteScriptingLesson5a.java`, `CrossSiteScriptingLesson4.java`: Rewrote inefficient regular expressions to prevent ReDoS attacks.
* **`java/spring-disabled-csrf-protection`**:
  * `WebSecurityConfig.java`: Enabled CSRF protection.
* **`java/path-injection` & `java/zipslip`**:
  * `ProfileUploadBase.java`, `ProfileZipSlip.java`, `FileServer.java`: Enforced canonical path validation.
* **`java/xxe`**:
  * `CommentsCache.java`, `BlindSendFileAssignment.java`, `ContentTypeAssignment.java`, `SimpleXXE.java`: Enabled XXE protection by reconfiguring the `XMLInputFactory`.

### Build and Test Fixes

* **Build Failures**:
  * Addressed multiple compilation errors by adding missing imports, correcting method signatures, and reverting incompatible API calls in `JWTRefreshEndpoint.java`, `JWTVotesEndpoint.java`, and `VulnerableComponentsLesson.java`.
* **Test Enhancements**:
  * Updated numerous test files to reflect the security fixes and ensure continued test coverage.

### Configuration Changes

* **GitHub Workflows**:
  * `build.yml`, `release.yml`, `welcome.yml`: Updated workflow files.
  * `codeql-artifact-only.yml`: Created and updated a new workflow for CodeQL artifact handling.
* **Project Configuration**:
  * `.gitignore`: Updated to ignore additional files.
  * `pom.xml`: Updated project dependencies and build configurations.

### Housekeeping

* **CodeQL Reports**: Removed SARIF files from the `codeql_reports` directory.
* **Gemini Context**: Updated `.gemini/GEMINI.md` and added `.gemini/settings.json`.

