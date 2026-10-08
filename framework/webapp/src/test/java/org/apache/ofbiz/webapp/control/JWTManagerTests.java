/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ofbiz.webapp.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;

import org.apache.ofbiz.service.ModelService;
import org.junit.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

public class JWTManagerTests {
    private static final String STRONG_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private static String signedToken(String key, String userLoginId) {
        return JWT.create()
                .withIssuer("ApacheOFBiz")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000L))
                .withClaim("userLoginId", userLoginId)
                .sign(Algorithm.HMAC512(key));
    }

    @Test
    public void isUsableKeyRejectsEmptyKeys() {
        assertFalse(JWTManager.isUsableKey(null));
        assertFalse(JWTManager.isUsableKey(""));
        assertFalse(JWTManager.isUsableKey("   "));
    }

    @Test
    public void isUsableKeyRejectsShippedPlaceholder() {
        assertFalse(JWTManager.isUsableKey(JWTManager.JWT_KEY_PLACEHOLDER));
        assertFalse(JWTManager.isUsableKey(" security.token.key "));
    }

    @Test
    public void isUsableKeyRejectsShortKeys() {
        assertFalse(JWTManager.isUsableKey("short-secret"));
        assertFalse(JWTManager.isUsableKey(STRONG_KEY.substring(0, JWTManager.JWT_KEY_MIN_LENGTH - 1)));
    }

    @Test
    public void isUsableKeyAcceptsLongRandomKeys() {
        assertTrue(JWTManager.isUsableKey(STRONG_KEY.substring(0, JWTManager.JWT_KEY_MIN_LENGTH)));
        assertTrue(JWTManager.isUsableKey(STRONG_KEY));
    }

    @Test
    public void validateTokenRejectsTokenForgedWithShippedPlaceholderKey() {
        String forged = signedToken(JWTManager.JWT_KEY_PLACEHOLDER, "admin");

        Map<String, Object> result = JWTManager.validateToken(forged, JWTManager.JWT_KEY_PLACEHOLDER);

        assertTrue(result.containsKey(ModelService.ERROR_MESSAGE));
        assertFalse(result.containsKey("userLoginId"));
    }

    @Test
    public void validateTokenRejectsEmptyKey() {
        String token = signedToken(STRONG_KEY, "admin");

        assertTrue(JWTManager.validateToken(token, null).containsKey(ModelService.ERROR_MESSAGE));
        assertTrue(JWTManager.validateToken(token, "").containsKey(ModelService.ERROR_MESSAGE));
    }

    @Test
    public void validateTokenRejectsTokenSignedWithAnotherKey() {
        String token = signedToken(STRONG_KEY + "x", "admin");

        Map<String, Object> result = JWTManager.validateToken(token, STRONG_KEY);

        assertTrue(result.containsKey(ModelService.ERROR_MESSAGE));
    }

    @Test
    public void validateTokenReturnsClaimsForTokenSignedWithConfiguredKey() {
        String token = signedToken(STRONG_KEY, "admin");

        Map<String, Object> result = JWTManager.validateToken(token, STRONG_KEY);

        assertFalse(result.containsKey(ModelService.ERROR_MESSAGE));
        assertEquals("admin", result.get("userLoginId"));
    }
}
