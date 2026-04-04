package ru.example.kvstorageservice.validation;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;


public final class KvRequestValidator {

    public static final int MAX_KEY_BYTES = 8 * 1024;
    public static final int MAX_VALUE_BYTES = 10 * 1024 * 1024;

    private KvRequestValidator() {
    }

    public static void validateKey(String key) {
        if (StringUtils.isEmpty(key)) {
            throw invalidArgument("key must be non-empty");
        }
        int keyBytes = key.getBytes(StandardCharsets.UTF_8).length;
        if (keyBytes > MAX_KEY_BYTES) {
            throw invalidArgument(
                "key exceeds maximum length of " + MAX_KEY_BYTES + " bytes (UTF-8), got " + keyBytes);
        }
    }

    public static void validateValue(byte[] value) {
        if (value != null && value.length > MAX_VALUE_BYTES) {
            throw invalidArgument(
                "value exceeds maximum size of " + MAX_VALUE_BYTES + " bytes, got " + value.length);
        }
    }

    public static void validateRangeBounds(String keySince, String keyTo) {
        if (keySince != null && keyTo != null && keySince.compareTo(keyTo) > 0) {
            throw invalidArgument("key_since must be less than or equal to key_to lexicographically");
        }
    }

    private static StatusRuntimeException invalidArgument(String description) {
        return Status.INVALID_ARGUMENT.withDescription(description).asRuntimeException();
    }
}
