package ru.example.kvstorageservice.validation;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KvRequestValidatorTest {

    @Test
    @DisplayName("validateKey rejects empty string")
    void validateKey_empty() {
        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class,
            () -> KvRequestValidator.validateKey(""));
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    @DisplayName("validateKey rejects key longer than MAX_KEY_BYTES in UTF-8")
    void validateKey_tooLong() {
        String key = "a".repeat(KvRequestValidator.MAX_KEY_BYTES + 1);
        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class,
            () -> KvRequestValidator.validateKey(key));
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    @DisplayName("validateKey accepts key at max UTF-8 byte length")
    void validateKey_atLimit() {
        String key = "a".repeat(KvRequestValidator.MAX_KEY_BYTES);
        assertDoesNotThrow(() -> KvRequestValidator.validateKey(key));
    }

    @Test
    @DisplayName("validateValue rejects payload above MAX_VALUE_BYTES")
    void validateValue_tooLarge() {
        byte[] huge = new byte[KvRequestValidator.MAX_VALUE_BYTES + 1];
        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class,
            () -> KvRequestValidator.validateValue(huge));
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    @DisplayName("validateValue allows null")
    void validateValue_nullOk() {
        assertDoesNotThrow(() -> KvRequestValidator.validateValue(null));
    }

    @Test
    @DisplayName("validateRangeBounds rejects inverted lexicographic range")
    void validateRangeBounds_inverted() {
        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class,
            () -> KvRequestValidator.validateRangeBounds("z", "a"));
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    @DisplayName("validateRangeBounds allows equal bounds")
    void validateRangeBounds_equalOk() {
        assertDoesNotThrow(() -> KvRequestValidator.validateRangeBounds("k", "k"));
    }
}
