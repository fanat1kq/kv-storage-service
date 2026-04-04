package ru.example.kvstorageservice;

import java.time.Duration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static final GenericContainer<?> TARANTOOL = new GenericContainer<>("tarantool/tarantool:3.2")
        .withExposedPorts(3301)
        .withCommand("tarantool", "/etc/tarantool/init.lua")
        .withClasspathResourceMapping("init.lua", "/etc/tarantool/init.lua", BindMode.READ_ONLY)
        .waitingFor(Wait.forLogMessage(".*Tarantool KV ready.*", 1)
            .withStartupTimeout(Duration.ofMinutes(2)));

    @DynamicPropertySource
    static void tarantoolProperties(DynamicPropertyRegistry registry) {
        registry.add("tarantool.host", TARANTOOL::getHost);
        registry.add("tarantool.port", () -> TARANTOOL.getMappedPort(3301));

    }
}