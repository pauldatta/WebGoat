/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.deserialization.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.List;

public class SafeObjectInputStream extends ObjectInputStream {

  private static final List<String> WHITELIST =
      Arrays.asList(
          "org.dummy.insecure.framework.VulnerableTaskHolder",
          "java.lang.String",
          "java.util.Date");

  public SafeObjectInputStream(InputStream in) throws IOException {
    super(in);
  }

  @Override
  protected Class<?> resolveClass(ObjectStreamClass desc)
      throws IOException, ClassNotFoundException {
    if (!WHITELIST.contains(desc.getName())) {
      throw new ClassNotFoundException(desc.getName() + " not found");
    }
    return super.resolveClass(desc);
  }
}
