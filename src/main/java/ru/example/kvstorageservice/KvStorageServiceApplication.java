package ru.example.kvstorageservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.example.kvstorageservice.config.TarantoolProperties;

@SpringBootApplication
@EnableConfigurationProperties(TarantoolProperties.class)
public class KvStorageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KvStorageServiceApplication.class, args);
    }

}
