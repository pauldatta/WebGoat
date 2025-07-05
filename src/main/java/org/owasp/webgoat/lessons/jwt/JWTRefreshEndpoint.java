/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.jwt;

import static org.owasp.webgoat.container.assignments.AttackResultBuilder.failed;
import static org.owasp.webgoat.container.assignments.AttackResultBuilder.success;
import static org.springframework.http.ResponseEntity.ok;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.owasp.webgoat.container.assignments.AssignmentEndpoint;
import org.owasp.webgoat.container.assignments.AssignmentHints;
import org.owasp.webgoat.container.assignments.AttackResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AssignmentHints({
  "jwt-refresh-hint1",
  "jwt-refresh-hint2",
  "jwt-refresh-hint3",
  "jwt-refresh-hint4"
})
public class JWTRefreshEndpoint implements AssignmentEndpoint {

  @Value("${webgoat.jwt.password}")
  private String password;

  private final JWTTokenService jwtTokenService;
  private static final List<String> validRefreshTokens = new ArrayList<>();
  private static final SecureRandom secureRandom = new SecureRandom();
  private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

  public JWTRefreshEndpoint(JWTTokenService jwtTokenService) {
    this.jwtTokenService = jwtTokenService;
  }

  @PostMapping(
      value = "/JWT/refresh/login",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity follow(@RequestBody(required = false) Map<String, Object> json) {
    if (json == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    String user = (String) json.get("user");
    String password = (String) json.get("password");

    if ("Jerry".equalsIgnoreCase(user) && this.password.equals(password)) {
      return ok(createNewTokens(user));
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  private Map<String, Object> createNewTokens(String user) {
    Map<String, Object> claims = Map.of("admin", "false", "user", user);
    String token = jwtTokenService.createToken(claims);
    Map<String, Object> tokenJson = new HashMap<>();
    byte[] randomBytes = new byte[24];
    secureRandom.nextBytes(randomBytes);
    String refreshToken = base64Encoder.encodeToString(randomBytes);
    validRefreshTokens.add(refreshToken);
    tokenJson.put("access_token", token);
    tokenJson.put("refresh_token", refreshToken);
    return tokenJson;
  }

  @PostMapping("/JWT/refresh/checkout")
  @ResponseBody
  public ResponseEntity<AttackResult> checkout(
      @RequestHeader(value = "Authorization", required = false) String token) {
    if (token == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      Jws<Claims> jws = jwtTokenService.parseToken(token.replace("Bearer ", ""));
      Claims claims = jws.getBody();
      String user = (String) claims.get("user");
      if ("Tom".equals(user)) {
        if ("none".equals(jws.getHeader().get("alg"))) {
          return ok(success(this).feedback("jwt-refresh-alg-none").build());
        }
        return ok(success(this).build());
      }
      return ok(failed(this).feedback("jwt-refresh-not-tom").feedbackArgs(user).build());
    } catch (ExpiredJwtException e) {
      return ok(failed(this).output(e.getMessage()).build());
    } catch (JwtException e) {
      return ok(failed(this).feedback("jwt-invalid-token").build());
    }
  }

  @PostMapping("/JWT/refresh/newToken")
  @ResponseBody
  public ResponseEntity newToken(
      @RequestHeader(value = "Authorization", required = false) String token,
      @RequestBody(required = false) Map<String, Object> json) {
    if (token == null || json == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String user;
    String refreshToken;
    try {
      Jws<Claims> jws = jwtTokenService.parseToken(token.replace("Bearer ", ""));
      user = (String) jws.getBody().get("user");
      refreshToken = (String) json.get("refresh_token");
    } catch (ExpiredJwtException e) {
      user = (String) e.getClaims().get("user");
      refreshToken = (String) json.get("refresh_token");
    }

    if (user == null || refreshToken == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } else if (validRefreshTokens.contains(refreshToken)) {
      validRefreshTokens.remove(refreshToken);
      return ok(createNewTokens(user));
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }
}
