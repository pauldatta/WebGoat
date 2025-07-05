/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.xss.mitigation;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;

import com.google.re2j.Pattern;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints(value = {"xss-mitigation-4-hint1"})
public class CrossSiteScriptingLesson4 implements AssignmentEndpoint {

  @PostMapping("/CrossSiteScripting/attack4")
  @ResponseBody
  public AttackResult completed(@RequestParam String editor2) {

    String editor = com.google.re2j.Pattern.compile("<[^>]*>").matcher(editor2).replaceAll("");

    if ((Pattern.compile("Policy.getInstance(\"antisamy-slashdot.xml\"").matcher(editor).find()
            || Pattern.compile(".scan(newComment, \"antisamy-slashdot.xml\"").matcher(editor).find()
            || Pattern.compile(".scan(newComment, new File(\"antisamy-slashdot.xml\")").matcher(editor).find())
        && Pattern.compile("new AntiSamy()").matcher(editor).find()
        && Pattern.compile(".scan(newComment,").matcher(editor).find()
        && Pattern.compile("CleanResults").matcher(editor).find()
        && Pattern.compile("MyCommentDAO.addComment(threadID, userID").matcher(editor).find()
        && Pattern.compile(".getCleanHTML())").matcher(editor).find()) {
      return success(this).feedback("xss-mitigation-4-success").build();
    } else {
      return failed(this).feedback("xss-mitigation-4-failed").build();
    }
  }
}
