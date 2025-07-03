/*
 * SPDX-FileCopyrightText: Copyright © 2016 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.sqlinjection.introduction;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import java.io.IOException;
import java.sql.*;
import org.owasp.webgoat.container.LessonDataSource;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints(
    value = {
      "SqlStringInjectionHint5b1",
      "SqlStringInjectionHint5b2",
      "SqlStringInjectionHint5b3",
      "SqlStringInjectionHint5b4"
    })
public class SqlInjectionLesson5b implements AssignmentEndpoint {

  private final LessonDataSource dataSource;

  public SqlInjectionLesson5b(LessonDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostMapping("/SqlInjection/assignment5b")
  @ResponseBody
  public AttackResult completed(@RequestParam String userid, @RequestParam String login_count)
      throws IOException {
    return injectableQuery(login_count, userid);
  }

  protected AttackResult injectableQuery(String login_count, String accountName) {
    String queryString = "SELECT * From user_data WHERE Login_Count = ? and userid= ?";
    try (Connection connection = dataSource.getConnection()) {
      PreparedStatement query =
          connection.prepareStatement(
              queryString, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

      int count = 0;
      try {
        count = Integer.parseInt(login_count);
      } catch (Exception e) {
        return failed(this)
            .output(
                "Could not parse: "
                    + login_count
                    + " to a number"
                    + "<br> Your query was: "
                    + queryString.replaceFirst("\\?", login_count).replaceFirst("\\?", accountName))
            .build();
      }

      query.setInt(1, count);
      query.setString(2, accountName);
      // String query = "SELECT * FROM user_data WHERE Login_Count = " + login_count + " and userid
      // = " + accountName, ;
      try {
        ResultSet results = query.executeQuery();

        if ((results != null) && (results.first() == true)) {
          ResultSetMetaData resultsMetaData = results.getMetaData();
          StringBuilder output = new StringBuilder();

          output.append(SqlInjectionLesson5a.writeTable(results, resultsMetaData));
          results.last();

          // If they get back more than one user they succeeded
          if (results.getRow() >= 6) {
            return success(this)
                .feedback("sql-injection.5b.success")
                .output(
                    "Your query was: "
                        + queryString
                            .replaceFirst("\\?", login_count)
                            .replaceFirst("\\?", accountName))
                .feedbackArgs(output.toString())
                .build();
          } else {
            return failed(this)
                .output(
                    output.toString()
                        + "<br> Your query was: "
                        + queryString
                            .replaceFirst("\\?", login_count)
                            .replaceFirst("\\?", accountName))
                .build();
          }

        } else {
          return failed(this)
              .feedback("sql-injection.5b.no.results")
              .output(
                  "Your query was: "
                      + queryString.replaceFirst("\\?", login_count).replaceFirst("\\?", accountName))
              .build();
        }
      } catch (SQLException sqle) {

        return failed(this)
            .output(
                sqle.getMessage()
                    + "<br> Your query was: "
                    + queryString.replaceFirst("\\?", login_count).replaceFirst("\\?", accountName))
            .build();
      }
    } catch (Exception e) {
      return failed(this)
          .output(
              this.getClass().getName()
                  + " : "
                  + e.getMessage()
                  + "<br> Your query was: "
                  + queryString.replaceFirst("\\?", login_count).replaceFirst("\\?", accountName))
          .build();
    }
  }
}
