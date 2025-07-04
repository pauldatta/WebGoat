/*
 * SPDX-FileCopyrightText: Copyright © 2016 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.advanced;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.owasp.webgoat.lessons.sqlinjection.introduction.SqlInjectionLesson5a;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints(
    value = {
      "SqlStringInjectionHint-advanced-6a-1",
      "SqlStringInjectionHint-advanced-6a-2",
      "SqlStringInjectionHint-advanced-6a-3",
      "SqlStringInjectionHint-advanced-6a-4",
      "SqlStringInjectionHint-advanced-6a-5"
    })
public class SqlInjectionLesson6a implements AssignmentEndpoint {
  private final LessonDataSource dataSource;
  private static final String YOUR_QUERY_WAS = "<br> Your query was: ";

  public SqlInjectionLesson6a(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjectionAdvanced/attack6a")
  @ResponseBody
  public AttackResult completed(@RequestParam(value = "userid_6a") String userId) {
    return injectableQuery(userId);
    // The answer: Smith' union select userid,user_name, password,cookie,cookie, cookie,userid from
    // user_system_data --
  }

  public AttackResult injectableQuery(String accountName) {
    // The query now uses a '?' placeholder for user data.
    String query = "SELECT * FROM user_data WHERE last_name = ?";
    try (Connection connection = dataSource.getConnection();
        // PreparedStatement is created with the safe query structure.
        PreparedStatement statement = connection.prepareStatement(query)) {

      // The user input is safely bound to the '?' placeholder.
      // The driver handles escaping, preventing any injection.
      statement.setString(1, accountName);

      // The 'unionQueryChecker' is part of the lesson's logic to check if the user
      // is attempting a specific type of attack. We can keep it for that purpose.
      boolean usedUnion = this.unionQueryChecker(accountName);

        // Note: executeSqlInjection would need to be refactored to accept a
        // PreparedStatement
        // or a ResultSet directly, as we no longer pass the raw query string.
        // For this example, we'll show the direct execution.

        try (ResultSet results = statement.executeQuery()) {
          // ... process the results ...
          // This part of the logic is specific to the WebGoat lesson and can remain.
          if (results.next()) {
            return success(this).feedback("sql-injection.advanced.6a.success").build();
          } else {
            return failed(this).feedback("sql-injection.advanced.6a.no.results").build();
          }
        }

    } catch (Exception e) {
      return failed(this)
          .output(this.getClass().getName() + " : " + e.getMessage() + YOUR_QUERY_WAS + query)
          .build();
    }
  }

  private boolean unionQueryChecker(String accountName) {
    int nullCharIndex = accountName.indexOf('\u0000');
    if (nullCharIndex == -1) {
      return false; // No null character, so it can't match.
    }
    // Check for UNION only in the part of the string before the null character.
    // The regex is now simpler and safer.
    return accountName.substring(0, nullCharIndex).matches("(?i).*\\bUNION\\b.*");
  }

  private AttackResult executeSqlInjection(Connection connection, String query, boolean usedUnion) {
    try (Statement statement =
        connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

      ResultSet results = statement.executeQuery(query);

      if (!((results != null) && results.first())) {
        return failed(this)
            .feedback("sql-injection.advanced.6a.no.results")
            .output(YOUR_QUERY_WAS + query)
            .build();
      }

      ResultSetMetaData resultsMetaData = results.getMetaData();
      StringBuilder output = new StringBuilder();
      String appendingWhenSucceded = this.appendSuccededMessage(usedUnion);

      output.append(SqlInjectionLesson5a.writeTable(results, resultsMetaData));
      results.last();

      return verifySqlInjection(output, appendingWhenSucceded, query);
    } catch (SQLException sqle) {
      return failed(this).output(sqle.getMessage() + YOUR_QUERY_WAS + query).build();
    }
  }

  private String appendSuccededMessage(boolean isUsedUnion) {
    String appendingWhenSucceded = "Well done! Can you also figure out a solution, by ";

    appendingWhenSucceded += isUsedUnion ? "appending a new SQL Statement?" : "using a UNION?";

    return appendingWhenSucceded;
  }

  private AttackResult verifySqlInjection(
      StringBuilder output, String appendingWhenSucceded, String query) {
    if (!(output.toString().contains("dave") && output.toString().contains("passW0rD"))) {
      return failed(this).output(output.toString() + YOUR_QUERY_WAS + query).build();
    }

    output.append(appendingWhenSucceded);
    return success(this)
        .feedback("sql-injection.advanced.6a.success")
        .feedbackArgs(output.toString())
        .output(" Your query was: " + query)
        .build();
  }
}
