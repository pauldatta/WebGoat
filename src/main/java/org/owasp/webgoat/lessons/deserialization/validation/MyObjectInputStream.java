/*
 * SPDX-FileCopyrightText: Copyright © 2014 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.deserialization.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.List;
import org.dummy.insecure.framework.VulnerableTaskHolder;

public class MyObjectInputStream extends ObjectInputStream {

  private static final List<String> WHITELIST = new ArrayList<>();

  static {
    WHITELIST.add(VulnerableTaskHolder.class.getName());
    WHITELIST.add("java.lang.Integer");
  }

  public MyObjectInputStream(InputStream in) throws IOException {
    super(in);
  }

  protected Class<?> resolveClass(ObjectStreamClass desc)
      throws IOException, ClassNotFoundException {
    if (!WHITELIST.contains(desc.getName())) {
      throw new InvalidClassException("Unauthorized deserialization attempt", desc.getName());
    }
    return super.resolveClass(desc);
  }
}
