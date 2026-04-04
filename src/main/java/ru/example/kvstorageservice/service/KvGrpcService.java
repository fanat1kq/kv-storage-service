package ru.example.kvstorageservice.service;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.example.kvstorageservice.model.GetResult;
import ru.example.kvstorageservice.repository.TarantoolKvRepository;
import ru.example.kvstorageservice.validation.KvRequestValidator;
import ru.example.kvstorageservice.grpc.CountRequest;
import ru.example.kvstorageservice.grpc.CountResponse;
import ru.example.kvstorageservice.grpc.DeleteRequest;
import ru.example.kvstorageservice.grpc.DeleteResponse;
import ru.example.kvstorageservice.grpc.GetRequest;
import ru.example.kvstorageservice.grpc.GetResponse;
import ru.example.kvstorageservice.grpc.KeyValue;
import ru.example.kvstorageservice.grpc.KvServiceGrpc;
import ru.example.kvstorageservice.grpc.PutRequest;
import ru.example.kvstorageservice.grpc.PutResponse;
import ru.example.kvstorageservice.grpc.RangeRequest;

@GrpcService
@RequiredArgsConstructor
public class KvGrpcService extends KvServiceGrpc.KvServiceImplBase {

    private final TarantoolKvRepository tarantool;

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        KvRequestValidator.validateKey(request.getKey());
        byte[] value = request.hasValue() ? request.getValue().toByteArray() : null;
        KvRequestValidator.validateValue(value);
        tarantool.put(request.getKey(), value);
        responseObserver.onNext(PutResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
        KvRequestValidator.validateKey(request.getKey());
        GetResult result = tarantool.get(request.getKey());

        if (!result.found()) {
            throw Status.NOT_FOUND
                .withDescription("Key not found: " + request.getKey())
                .asRuntimeException();
        }

        GetResponse.Builder builder = GetResponse.newBuilder();
        if (result.value() != null) {
            builder.setValue(ByteString.copyFrom(result.value()));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        KvRequestValidator.validateKey(request.getKey());
        tarantool.delete(request.getKey());
        responseObserver.onNext(DeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void range(RangeRequest request, StreamObserver<KeyValue> responseObserver) {
        KvRequestValidator.validateRangeBounds(request.getKeySince(), request.getKeyTo());
        tarantool.range(request.getKeySince(), request.getKeyTo(),
            (key, value) -> responseObserver.onNext(buildKeyValue(key, value)));
        responseObserver.onCompleted();
    }

    @Override
    public void count(CountRequest request, StreamObserver<CountResponse> responseObserver) {
        long count = tarantool.count();
        responseObserver.onNext(CountResponse.newBuilder().setCount(count).build());
        responseObserver.onCompleted();
    }

    private KeyValue buildKeyValue(String key, byte[] value) {
        KeyValue.Builder builder = KeyValue.newBuilder().setKey(key);
        if (value != null) builder.setValue(ByteString.copyFrom(value));
        return builder.build();
    }
}