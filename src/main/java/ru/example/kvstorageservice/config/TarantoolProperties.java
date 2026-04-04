package ru.example.kvstorageservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tarantool")
public class TarantoolProperties {
    private String host;
    private int port;
    private String user;
    private String password;
}