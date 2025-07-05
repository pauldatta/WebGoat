# CodeQL Vulnerability Fixing Agent

You are a fantastic agent that uses a systematic approach to resolving the security vulnerabilities identified by CodeQL scans in this repository.

## Important

1. CodeQL scan reports can be found in ./codeql_reports
2. With github MCP, Use workflow "CodeQL Advanced - Artifact Only" to fetch code scan reports and to trigger scaning on branches. Save them to ./codeql_reports

## General Workflow

1. Parse and Prioritize: It began by inspecting the *.sarif report, identifying all unique vulnerability types (ruleId).
# Simplified example of the command used
python3 -c "import json; f=open('java.sarif'); data=json.load(f); rules=set(); [rules.add(r['ruleId']) for run in data['runs'] for r in run['results']]; print(rules)"
1. Isolate and Conquer: Tackle one vulnerability class at a time, first querying the SARIF file for all instances of a specific rule (e.g., java/unsafe-deserialization).
1. Analyze and Fix: For each instance, read the affected source file to understand the context before applying a precise, best-practice fix.
1. Iterate: Once all instances of a class were fixed, it would re-query the SARIF data for the next vulnerability type and repeat the process until the reports were clean.

## Downloading Artifacts
1. Use list_workflow_run_artifacts (github MCP Server) using the user provided run id within the url
1. Use  download_workflow_run_artifact (github MCP Server) and then curl to download the report
1. unzip the report into the codeql_reports directory


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


Tool CodeQL
Rule ID java/path-injection
Query Uncontrolled data used in path expression

Description
Accessing paths controlled by users can allow an attacker to access unexpected resources. This can result in sensitive information being revealed or deleted, or an attacker being able to influence behavior by modifying unexpected files.

Paths that are naively constructed from data controlled by a user may be absolute paths, or may contain unexpected special characters such as "..". Such a path could point anywhere on the file system.

Recommendation
Validate user input before using it to construct a file path.

Common validation methods include checking that the normalized path is relative and does not contain any ".." components, or checking that the path is contained within a safe folder. The method you should use depends on how the path is used in the application, and whether the path should be a single path component.

If the path should be a single path component (such as a file name), you can check for the existence of any path separators ("/" or "\"), or ".." sequences in the input, and reject the input if any are found.

Note that removing "../" sequences is not sufficient, since the input could still contain a path separator followed by "..". For example, the input ".../...//" would still result in the string "../" if only "../" sequences are removed.

Finally, the simplest (but most restrictive) option is to use an allow list of safe patterns and make sure that the user input matches one of these patterns.

Example
In this example, a file name is read from a java.net.Socket and then used to access a file and send it back over the socket. However, a malicious user could enter a file name anywhere on the file system, such as "/etc/passwd" or "../../../etc/passwd".

public void sendUserFile(Socket sock, String user) {
	BufferedReader filenameReader = new BufferedReader(
			new InputStreamReader(sock.getInputStream(), "UTF-8"));
	String filename = filenameReader.readLine();
	// BAD: read from a file without checking its path
	BufferedReader fileReader = new BufferedReader(new FileReader(filename));
	String fileLine = fileReader.readLine();
	while(fileLine != null) {
		sock.getOutputStream().write(fileLine.getBytes());
		fileLine = fileReader.readLine();
	}
}
If the input should only be a file name, you can check that it doesn't contain any path separators or ".." sequences.

public void sendUserFileGood(Socket sock, String user) {
	BufferedReader filenameReader = new BufferedReader(
			new InputStreamReader(sock.getInputStream(), "UTF-8"));
	String filename = filenameReader.readLine();
	// GOOD: ensure that the filename has no path separators or parent directory references
	if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
		throw new IllegalArgumentException("Invalid filename");
	}
	BufferedReader fileReader = new BufferedReader(new FileReader(filename));
	String fileLine = fileReader.readLine();
	while(fileLine != null) {
		sock.getOutputStream().write(fileLine.getBytes());
		fileLine = fileReader.readLine();
	}
}
If the input should be within a specific directory, you can check that the resolved path is still contained within that directory.

public void sendUserFileGood(Socket sock, String user) {
	BufferedReader filenameReader = new BufferedReader(
			new InputStreamReader(sock.getInputStream(), "UTF-8"));
	String filename = filenameReader.readLine();

	Path publicFolder = Paths.get("/home/" + user + "/public").normalize().toAbsolutePath();
	Path filePath = publicFolder.resolve(filename).normalize().toAbsolutePath();

	// GOOD: ensure that the path stays within the public folder
	if (!filePath.startsWith(publicFolder + File.separator)) {
		throw new IllegalArgumentException("Invalid filename");
	}
	BufferedReader fileReader = new BufferedReader(new FileReader(filePath.toString()));
	String fileLine = fileReader.readLine();
	while(fileLine != null) {
		sock.getOutputStream().write(fileLine.getBytes());
		fileLine = fileReader.readLine();
	}
}

Tool CodeQL
Rule ID java/polynomial-redos
Query Polynomial regular expression used on uncontrolled data
Description
Some regular expressions take a long time to match certain input strings to the point where the time it takes to match a string of length n is proportional to nk or even 2n. Such regular expressions can negatively affect performance, or even allow a malicious user to perform a Denial of Service ("DoS") attack by crafting an expensive input string for the regular expression to match.

The regular expression engine provided by Java uses a backtracking non-deterministic finite automata to implement regular expression matching. While this approach is space-efficient and allows supporting advanced features like capture groups, it is not time-efficient in general. The worst-case time complexity of such an automaton can be polynomial or even exponential, meaning that for strings of a certain shape, increasing the input length by ten characters may make the automaton about 1000 times slower.

Typically, a regular expression is affected by this problem if it contains a repetition of the form r* or r+ where the sub-expression r is ambiguous in the sense that it can match some string in multiple ways. More information about the precise circumstances can be found in the references.

Note that Java versions 9 and above have some mitigations against ReDoS; however they aren't perfect and more complex regular expressions can still be affected by this problem.

Recommendation
Modify the regular expression to remove the ambiguity, or ensure that the strings matched with the regular expression are short enough that the time-complexity does not matter. Alternatively, an alternate regex library that guarantees linear time execution, such as Google's RE2J, may be used.

Example
Consider this use of a regular expression, which removes all leading and trailing whitespace in a string:

Pattern.compile("^\\s+|\\s+$").matcher(text).replaceAll("") // BAD
The sub-expression "\\s+$" will match the whitespace characters in text from left to right, but it can start matching anywhere within a whitespace sequence. This is problematic for strings that do not end with a whitespace character. Such a string will force the regular expression engine to process each whitespace sequence once per whitespace character in the sequence.

This ultimately means that the time cost of trimming a string is quadratic in the length of the string. So a string like "a b" will take milliseconds to process, but a similar string with a million spaces instead of just one will take several minutes.

Avoid this problem by rewriting the regular expression to not contain the ambiguity about when to start matching whitespace sequences. For instance, by using a negative look-behind ("^\\s+|(?<!\\s)\\s+$"), or just by using the built-in trim method (text.trim()).

Note that the sub-expression "^\\s+" is not problematic as the ^ anchor restricts when that sub-expression can start matching, and as the regular expression engine matches from left to right.

Example
As a similar, but slightly subtler problem, consider the regular expression that matches lines with numbers, possibly written using scientific notation:

"^0\\.\\d+E?\\d+$""
The problem with this regular expression is in the sub-expression \d+E?\d+ because the second \d+ can start matching digits anywhere after the first match of the first \d+ if there is no E in the input string.

This is problematic for strings that do not end with a digit. Such a string will force the regular expression engine to process each digit sequence once per digit in the sequence, again leading to a quadratic time complexity.

To make the processing faster, the regular expression should be rewritten such that the two \d+ sub-expressions do not have overlapping matches: "^0\\.\\d+(E\\d+)?$".

Example
Sometimes it is unclear how a regular expression can be rewritten to avoid the problem. In such cases, it often suffices to limit the length of the input string. For instance, the following regular expression is used to match numbers, and on some non-number inputs it can have quadratic time complexity:

Pattern.matches("^(\\+|-)?(\\d+|(\\d*\\.\\d*))?(E|e)?([-+])?(\\d+)?$", str);
It is not immediately obvious how to rewrite this regular expression to avoid the problem. However, you can mitigate performance issues by limiting the length to 1000 characters, which will always finish in a reasonable amount of time.

if (str.length() > 1000) {
    throw new IllegalArgumentException("Input too long");
}

Pattern.matches("^(\\+|-)?(\\d+|(\\d*\\.\\d*))?(E|e)?([-+])?(\\d+)?$", str);
