package ru.example.kvstorageservice.config;

import io.tarantool.client.TarantoolClient;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.pool.InstanceConnectionGroup;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
@EnableConfigurationProperties(TarantoolProperties.class)
public class TarantoolConfig {

    @Bean
    public TarantoolClient tarantoolClient(TarantoolProperties props) throws Exception {
        InstanceConnectionGroup group = InstanceConnectionGroup.builder()
            .withHost(props.getHost())
            .withPort(props.getPort())
            .withUser(props.getUser())
            .withPassword(props.getPassword())
            .build();

        return TarantoolFactory.box()
            .withGroups(Collections.singletonList(group))
            .build();
    }
}