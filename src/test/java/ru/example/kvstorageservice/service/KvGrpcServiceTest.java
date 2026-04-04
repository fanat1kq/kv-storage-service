package ru.example.kvstorageservice.service;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.example.kvstorageservice.BaseIntegrationTest;
import ru.example.kvstorageservice.grpc.CountRequest;
import ru.example.kvstorageservice.grpc.CountResponse;
import ru.example.kvstorageservice.grpc.DeleteRequest;
import ru.example.kvstorageservice.grpc.GetRequest;
import ru.example.kvstorageservice.grpc.GetResponse;
import ru.example.kvstorageservice.grpc.KvServiceGrpc;
import ru.example.kvstorageservice.grpc.KeyValue;
import ru.example.kvstorageservice.grpc.PutRequest;
import ru.example.kvstorageservice.grpc.RangeRequest;
import ru.example.kvstorageservice.repository.TarantoolKvRepository;
import ru.example.kvstorageservice.validation.KvRequestValidator;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "grpc.server.port=9191")
class KvGrpcServiceTest extends BaseIntegrationTest {

    @Autowired
    private TarantoolKvRepository repository;

    private KvServiceGrpc.KvServiceBlockingStub stub;

    public static final int PORT = 9191;

    @BeforeEach
    void cleanup() {
        repository.range("", "\uFFFF", (key, value) -> repository.delete(key));
    }

    @BeforeEach
    void setUp() {
        ManagedChannel channel = ManagedChannelBuilder
            .forAddress("localhost", PORT)
            .usePlaintext()
            .build();
        stub = KvServiceGrpc.newBlockingStub(channel);
    }

    @Test
    @DisplayName("PUT and GET operations should store and retrieve value correctly")
    void put_and_get_returnsValue() {
        ByteString value = ByteString.copyFromUtf8("hello");

        stub.put(PutRequest.newBuilder().setKey("grpc-key").setValue(value).build());

        GetResponse response = stub.get(GetRequest.newBuilder().setKey("grpc-key").build());
        assertThat(response.getValue()).isEqualTo(value);
    }

    @Test
    @DisplayName("PUT without value and GET: key exists, value field is absent (null in DB)")
    void put_withoutValue_get_hasNoValueField() {
        stub.put(PutRequest.newBuilder().setKey("null-value-key").build());

        GetResponse response = stub.get(GetRequest.newBuilder().setKey("null-value-key").build());
        assertThat(response.hasValue()).isFalse();
    }

    @Test
    @DisplayName("GET for missing key should throw NOT_FOUND exception")
    void get_missingKey_throwsNotFound() {
        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class,
            () -> stub.get(GetRequest.newBuilder().setKey("missing").build()));

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    @DisplayName("DELETE operation should remove key and subsequent GET should return NOT_FOUND")
    void delete_removesKey() {
        stub.put(PutRequest.newBuilder().setKey("del-key").setValue(ByteString.copyFromUtf8("v"))
            .build());
        stub.delete(DeleteRequest.newBuilder().setKey("del-key").build());

        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class,
            () -> stub.get(GetRequest.newBuilder().setKey("del-key").build()));

        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    @DisplayName("RANGE operation should return stream of key-value pairs within specified range")
    void range_returnsStream() {
        stub.put(
            PutRequest.newBuilder().setKey("r1").setValue(ByteString.copyFromUtf8("v1")).build());
        stub.put(
            PutRequest.newBuilder().setKey("r2").setValue(ByteString.copyFromUtf8("v2")).build());

        List<String> keys = new ArrayList<>();
        stub.range(RangeRequest.newBuilder().setKeySince("r1").setKeyTo("r2").build())
            .forEachRemaining(kv -> keys.add(kv.getKey()));

        assertThat(keys).containsExactly("r1", "r2");
    }

    @Test
    @DisplayName("RANGE stream includes entries with null value")
    void range_includesNullValues() {
        stub.put(PutRequest.newBuilder().setKey("rn1").build());
        stub.put(PutRequest.newBuilder().setKey("rn2").setValue(ByteString.copyFromUtf8("x")).build());

        List<KeyValue> rows = new ArrayList<>();
        stub.range(RangeRequest.newBuilder().setKeySince("rn1").setKeyTo("rn2").build())
            .forEachRemaining(rows::add);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getKey()).isEqualTo("rn1");
        assertThat(rows.get(0).hasValue()).isFalse();
        assertThat(rows.get(1).getKey()).isEqualTo("rn2");
        assertThat(rows.get(1).getValue().toStringUtf8()).isEqualTo("x");
    }

    @Test
    @DisplayName("COUNT operation should return total number of keys in storage")
    void count_returnsCorrectNumber() {
        stub.put(PutRequest.newBuilder().setKey("c1").build());
        stub.put(PutRequest.newBuilder().setKey("c2").build());

        CountResponse response = stub.count(CountRequest.newBuilder().build());
        assertThat(response.getCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("PUT with empty key returns INVALID_ARGUMENT")
    void put_emptyKey_invalidArgument() {
        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class,
            () -> stub.put(PutRequest.newBuilder().setKey("").setValue(ByteString.copyFromUtf8("x")).build()));
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    @DisplayName("PUT with key longer than limit returns INVALID_ARGUMENT")
    void put_keyTooLong_invalidArgument() {
        String key = "a".repeat(KvRequestValidator.MAX_KEY_BYTES + 1);
        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class,
            () -> stub.put(PutRequest.newBuilder().setKey(key).setValue(ByteString.copyFromUtf8("v")).build()));
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    @DisplayName("RANGE with key_since greater than key_to returns INVALID_ARGUMENT")
    void range_invertedBounds_invalidArgument() {
        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class,
            () -> stub.range(RangeRequest.newBuilder().setKeySince("z").setKeyTo("a").build()).hasNext());
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    @DisplayName("GET with empty key returns INVALID_ARGUMENT")
    void get_emptyKey_invalidArgument() {
        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class,
            () -> stub.get(GetRequest.newBuilder().setKey("").build()));
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }
}