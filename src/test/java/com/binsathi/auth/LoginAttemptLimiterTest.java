package com.binsathi.auth;

import com.binsathi.common.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {

    @Test
    void blocksAfterTooManyFailuresForThatEmailOnly() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(3, 10);
        for (int i = 0; i < 3; i++) {
            limiter.checkAllowed("a@test.local");
            limiter.recordFailure("a@test.local");
        }

        assertEquals(429, assertThrows(ApiException.class, () -> limiter.checkAllowed("a@test.local")).getStatus());
        assertDoesNotThrow(() -> limiter.checkAllowed("b@test.local"));
    }

    @Test
    void successfulLoginClearsTheFailures() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(2, 10);
        limiter.recordFailure("a@test.local");
        limiter.recordFailure("a@test.local");

        limiter.reset("a@test.local");

        assertDoesNotThrow(() -> limiter.checkAllowed("a@test.local"));
    }
}