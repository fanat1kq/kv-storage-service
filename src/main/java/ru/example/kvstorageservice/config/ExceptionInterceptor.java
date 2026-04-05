package ru.example.kvstorageservice.config;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@GrpcGlobalServerInterceptor
public class ExceptionInterceptor implements ServerInterceptor {

    @Override
    public <Q, R> ServerCall.Listener<Q> interceptCall(
        ServerCall<Q, R> call, Metadata headers, ServerCallHandler<Q, R> next) {
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(
            next.startCall(call, headers)) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (StatusRuntimeException e) {
                    var code = e.getStatus().getCode();
                    if (code == Status.Code.INVALID_ARGUMENT) {
                        log.debug("gRPC invalid argument: {}", e.getStatus().getDescription());
                    } else if (code != Status.Code.NOT_FOUND) {
                        log.warn("gRPC {}: {}", code, e.getStatus().getDescription());
                    }
                    call.close(e.getStatus(), new Metadata());
                } catch (Exception e) {
                    log.error("gRPC internal error", e);
                    call.close(Status.INTERNAL.withDescription(e.getMessage()), new Metadata());
                }
            }
        };
    }
}