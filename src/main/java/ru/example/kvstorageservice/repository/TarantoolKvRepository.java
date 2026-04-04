package ru.example.kvstorageservice.repository;

import io.tarantool.client.TarantoolClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.example.kvstorageservice.model.GetResult;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import static ru.example.kvstorageservice.util.TarantoolConstants.BATCH_SIZE;
import static ru.example.kvstorageservice.util.TarantoolConstants.COUNT_LUA;
import static ru.example.kvstorageservice.util.TarantoolConstants.DELETE_LUA;
import static ru.example.kvstorageservice.util.TarantoolConstants.GET_LUA;
import static ru.example.kvstorageservice.util.TarantoolConstants.PUT_LUA;
import static ru.example.kvstorageservice.util.TarantoolConstants.RANGE_LUA;

@Component
@RequiredArgsConstructor
public class TarantoolKvRepository {

    private final TarantoolClient client;

    public void put(String key, byte[] value) {
        client.eval(PUT_LUA, Arrays.asList(key, value)).join();
    }

    public GetResult get(String key) {
        List<?> result = client.eval(GET_LUA, List.of(key), Object.class)
            .join().get();

        if (result == null || result.isEmpty()) {
            return new GetResult(false, null);
        }

        Object found = result.get(0);
        if (!(found instanceof Boolean) || !(Boolean) found) {
            return new GetResult(false, null);
        }

        Object raw = result.size() > 1 ? result.get(1) : null;
        return new GetResult(true, extractBytes(raw));
    }

    public void delete(String key) {
        client.eval(DELETE_LUA, List.of(key)).join();
    }

    public long count() {
        List<?> result = client.eval(COUNT_LUA, List.of(), Object.class).join().get();
        if (result == null || result.isEmpty()) return 0L;
        return ((Number) result.get(0)).longValue();
    }

    public void range(String keySince, String keyTo, BiConsumer<String, byte[]> consumer) {
        String currentKey = keySince;
        boolean firstBatch = true;
        boolean hasMore = true;

        while (hasMore) {
            String iterator = firstBatch ? "GE" : "GT";
            firstBatch = false;

            List<?> batchResult = fetchBatch(currentKey, keyTo, iterator);
            if (batchResult == null || batchResult.isEmpty()) break;

            int count = 0;
            for (Object item : batchResult) {
                if (!(item instanceof List<?> pair)) continue;
                if (pair.isEmpty()) continue;
                if (!(pair.get(0) instanceof String key)) continue;

                byte[] value = pair.size() > 1 ? extractBytes(pair.get(1)) : null;
                consumer.accept(key, value);
                currentKey = key;
                count++;
            }

            hasMore = count >= BATCH_SIZE;
        }
    }

    private byte[] extractBytes(Object raw) {
        if (raw instanceof byte[] bytes) return bytes;
        if (raw instanceof String s) return s.getBytes(StandardCharsets.ISO_8859_1);
        return new byte[0];
    }

    private List<?> fetchBatch(String currentKey, String keyTo, String iterator) {
        return client.eval(
            RANGE_LUA,
            List.of(currentKey, keyTo, BATCH_SIZE, iterator),
            Object.class
        ).join().get();
    }
}