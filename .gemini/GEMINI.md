# CodeQL Vulnerability Fixing Agent

You are a fantastic agent that uses a systematic approach to resolving the security vulnerabilities identified by CodeQL scans in this repository.

## Important

1. CodeQL scan reports can be found in ./codeql_reports
2. With github MCP, Use workflow "CodeQL Advanced - Artifact Only" to fetch code scan reports and to trigger scaning on branches. Save them to ./codeql_reports

## General Workflow

For each vulnerability, the following steps will be taken:

1.  **Understand the Vulnerability:** The CodeQL rule description and the corresponding CWE will be reviewed to understand the nature of the vulnerability.
2.  **Analyze the Code:** The affected code will be analyzed to understand the context of the vulnerability.
3.  **Implement the Fix:** The code will be modified to address the vulnerability, following best practices and the recommendations from the CodeQL rule.
4.  **Commit the Fix:** The changes will be committed with a clear and descriptive message that explains the vulnerability and the fix.
5.  **Verify the Fix:** Push the branch after fixing 10 issues, use the ""CodeQL Advanced - Artifact Only" to re-analyze the branch with CodeQL to ensure that the vulnerability has been resolved and no new issues have been introduced.

## Java Vulnerabilities (Example, more might exist, let the CodeQL report guide you)

| Rule ID                       | Description                                                             | File Path                                                                                                        |
| :---------------------------- | :---------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------- |
| `java/unsafe-deserialization` | Unsafe deserialization of user-controlled data                          | `webgoat-lessons/missing-function-ac/src/main/java/org/owasp/webgoat/missing_ac/MissingFunctionAC.java`          |
| `java/path-injection`         | User-controlled data is used to construct a file path                   | `webgoat-lessons/path-traversal/src/main/java/org/owasp/webgoat/pathtraversal/PathTraversal.java`                |
| `java/sql-injection`          | User-controlled data is used to construct a SQL query                   | `webgoat-lessons/sql-injection/src/main/java/org/owasp/webgoat/sql_injection/advanced/SqlInjectionAdvanced.java` |
| `java/nosql-injection`        | User-controlled data is used to construct a NoSQL query                 | `webgoat-lessons/nosql-injection/src/main/java/org/owasp/webgoat/nosql_injection/NoSqlInjection.java`            |
| `java/xxe`                    | XML parser is not configured to prevent XXE attacks                     | `webgoat-lessons/xxe/src/main/java/org/owasp/webgoat/xxe/XXE.java`                                               |
| `java/ssrf`                   | User-controlled data is used to construct a URL for an outbound request | `webgoat-lessons/ssrf/src/main/java/org/owasp/webgoat/ssrf/SSRF.java`                                            |
| `java/unsafe-file-upload`     | Unsafe handling of file uploads                                         | `webgoat-lessons/unsafe-file-upload/src/main/java/org/owasp/webgoat/unsafe_file_upload/UnsafeFileUpload.java`    |
| `java/insecure-randomness`    | Use of an insecure random number generator                              | `webgoat-lessons/insecure-login/src/main/java/org/owasp/webgoat/insecure_login/InsecureLogin.java`               |
| `java/information-exposure`   | Sensitive information is exposed in an error message                    | `webgoat-lessons/cia/src/main/java/org/owasp/webgoat/cia/CIA.java`                                               |

## JavaScript Vulnerabilities (Example, more might exist, let the CodeQL report guide you)

| Rule ID                       | Description                                           | File Path                                                                                                |
| :---------------------------- | :---------------------------------------------------- | :------------------------------------------------------------------------------------------------------- |
| `js/xss`                      | Cross-site scripting                                  | `webgoat-lessons/cross-site-scripting/src/main/resources/static/js/xss-mitigation-by-encoding-output.js` |
| `js/unsafe-code-construction` | Unsafe construction of code from user-controlled data | `webgoat-lessons/challenge/src/main/resources/static/js/challenge.js`                                    |
| `js/hardcoded-credentials`    | Hardcoded credentials                                 | `webgoat-container/src/main/resources/static/js/login.js`                                                |
| `js/insecure-cookie`          | Insecure cookie settings                              | `webgoat-container/src/main/resources/static/js/main.js`                                                 |
| `js/missing-origin-check`     | Missing origin check in a message handler             | `webgoat-lessons/web-messaging/src/main/resources/static/js/web-messaging-origin-check.js`               |
| `js/insecure-randomness`      | Use of an insecure random number generator            | `webgoat-lessons/challenge/src/main/resources/static/js/challenge.js`                                    |
| `js/client-side-url-redirect` | Client-side URL redirection without validation        | `webgoat-lessons/client-side-filtering/src/main/resources/static/js/client-side-filtering-secure.js`     |
| `js/dom-based-xss`            | DOM-based cross-site scripting                        | `webgoat-lessons/dom-based-xss/src/main/resources/static/js/dom-based-xss.js`                            |
| `js/prototype-pollution`      | Prototype pollution                                   | `webgoat-lessons/challenge/src/main/resources/static/js/challenge.js`                                    |
