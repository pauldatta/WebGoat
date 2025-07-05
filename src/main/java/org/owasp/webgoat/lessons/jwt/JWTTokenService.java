/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JWTTokenService {

  private final String key;
  private final SignatureAlgorithm signatureAlgorithm;
  private final SecretKeySpec secretKeySpec;

  public JWTTokenService(@Value("${webgoat.jwt.key}") String key) {
    this.key = key;
    this.signatureAlgorithm = SignatureAlgorithm.HS512;
    this.secretKeySpec = new SecretKeySpec(key.getBytes(), signatureAlgorithm.getJcaName());
  }

  public String createToken(Map<String, Object> claims) {
    return Jwts.builder()
        .setIssuedAt(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toDays(10)))
        .setClaims(claims)
        .signWith(signatureAlgorithm, secretKeySpec)
        .compact();
  }

  public Jws<Claims> parseToken(String token) {
    return Jwts.parser().setSigningKey(secretKeySpec).parseClaimsJws(token);
  }
}
