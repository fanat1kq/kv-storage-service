package ru.example.kvstorageservice.repository;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.example.kvstorageservice.BaseIntegrationTest;
import ru.example.kvstorageservice.model.GetResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TarantoolKvRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TarantoolKvRepository repository;

    @BeforeEach
    void cleanup() {
        repository.range("", "\uFFFF", (key, value) -> repository.delete(key));
    }

    @Test
    @DisplayName("PUT and GET: Should store and retrieve value by key")
    void put_and_get_returnsValue() {
        byte[] value = "hello".getBytes();
        repository.put("key1", value);

        GetResult result = repository.get("key1");

        assertThat(result.found()).isTrue();
        assertThat(result.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("GET: Should return NOT_FOUND when key does not exist")
    void get_missingKey_returnsNotFound() {
        GetResult result = repository.get("missing");
        assertThat(result.found()).isFalse();
    }

    @Test
    @DisplayName("PUT with null value: key exists, value is null")
    void put_nullValue_and_get_returnsPresent() {
        repository.put("key-null", null);

        GetResult result = repository.get("key-null");
        assertThat(result.found()).isTrue();
        assertThat(result.value()).isNull();
    }

    @Test
    @DisplayName("DELETE: Should remove key and make it unavailable")
    void delete_removesKey() {
        repository.put("key2", "value".getBytes());
        repository.delete("key2");

        assertThat(repository.get("key2").found()).isFalse();
    }

    @Test
    @DisplayName("COUNT: Should return total number of keys in storage")
    void count_returnsCorrectNumber() {
        repository.put("a", "1".getBytes());
        repository.put("b", "2".getBytes());
        repository.put("c", "3".getBytes());

        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("RANGE: Should return all keys within specified inclusive bounds")
    void range_returnsKeysInBounds() {
        repository.put("a", "1".getBytes());
        repository.put("b", "2".getBytes());
        repository.put("c", "3".getBytes());
        repository.put("d", "4".getBytes());

        List<String> keys = new ArrayList<>();
        repository.range("b", "c", (key, value) -> keys.add(key));

        assertThat(keys).containsExactly("b", "c");
    }

    @Test
    @DisplayName("RANGE: Should not call consumer when storage is empty")
    void range_emptySpace_callsConsumerNever() {
        List<String> keys = new ArrayList<>();
        repository.range("a", "z", (key, value) -> keys.add(key));

        assertThat(keys).isEmpty();
    }
}