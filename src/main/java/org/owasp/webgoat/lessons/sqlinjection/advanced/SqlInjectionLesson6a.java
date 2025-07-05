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
  }

  public AttackResult injectableQuery(String accountName) {
    String query = "SELECT * FROM user_data WHERE last_name = ?";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY)) {

      statement.setString(1, accountName);
      boolean usedUnion = this.unionQueryChecker(accountName);

      try (ResultSet results = statement.executeQuery()) {
        if (!results.first()) {
          return failed(this)
              .feedback("sql-injection.advanced.6a.no.results")
              .output(YOUR_QUERY_WAS + query)
              .build();
        }

        ResultSetMetaData resultsMetaData = results.getMetaData();
        StringBuilder output = new StringBuilder();
        output.append(SqlInjectionLesson5a.writeTable(results, resultsMetaData));
        results.last();

        // Verification logic moved here from the old helper methods
        if (!(output.toString().contains("dave") && output.toString().contains("passW0rD"))) {
          return failed(this).output(output.toString() + YOUR_QUERY_WAS + query).build();
        }

        String appendingWhenSucceded = this.appendSuccededMessage(usedUnion);
        output.append(appendingWhenSucceded);
        return success(this)
            .feedback("sql-injection.advanced.6a.success")
            .feedbackArgs(output.toString())
            .output(" Your query was: " + query)
            .build();
      }
    } catch (Exception e) {
      return failed(this)
          .output(this.getClass().getName() + " : " + e.getMessage() + YOUR_QUERY_WAS + query)
          .build();
    }
  }

  /**
   * Securely checks if the input string contains the word "UNION" without using a
   * vulnerable
   * regular expression.
   */
  private boolean unionQueryChecker(String accountName) {
    int nullCharIndex = accountName.indexOf('\u0000');
    if (nullCharIndex == -1) {
      return false;
    }
    // Use simple, fast string methods instead of a vulnerable regex to prevent
    // ReDoS.
    String sub = accountName.substring(0, nullCharIndex).toUpperCase();
    // This correctly simulates checking for the whole word "UNION".
    return sub.equals("UNION") || sub.startsWith("UNION ") || sub.endsWith(" UNION") || sub.contains(" UNION ");
  }

  private String appendSuccededMessage(boolean isUsedUnion) {
    String appendingWhenSucceded = "Well done! Can you also figure out a solution, by ";
    appendingWhenSucceded += isUsedUnion ? "appending a new SQL Statement?" : "using a UNION?";
    return appendingWhenSucceded;
  }
}
