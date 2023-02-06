package ru.yandex.market.tsup.config;

import java.io.IOException;

import io.lettuce.core.ReadFrom;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import redis.embedded.RedisExecProvider;
import redis.embedded.RedisServer;
import redis.embedded.util.OS;

@Configuration
@Data
@ConfigurationProperties("tsup.redis")
public class RedisConfiguration {

    private int port;
    private String host;
    private String sandboxToken;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory redisConnectionFactory
    ) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        return redisTemplate;
    }

    @Bean
    @DependsOn("redisServerMock")
    public LettuceConnectionFactory redisConnectionFactory(
        org.springframework.data.redis.connection.RedisConfiguration redisConfiguration,
        LettuceClientConfiguration clientConfiguration
    ) {
        return new LettuceConnectionFactory(redisConfiguration, clientConfiguration);
    }

    @Bean
    public org.springframework.data.redis.connection.RedisConfiguration redisLocalConfiguration() {
        return new RedisStandaloneConfiguration(host, port);
    }

    @Bean
    public LettuceClientConfiguration lettuceClientConfiguration() {
        return LettuceClientConfiguration.builder()
            .readFrom(ReadFrom.REPLICA_PREFERRED)
            .build();
    }

    @Bean
    public RedisServer redisServerMock() throws IOException {
        String pathToRedis = new RedisDownloader(sandboxToken).getRedisPath();

        RedisExecProvider provider = RedisExecProvider.build()
            .override(OS.UNIX, pathToRedis)
            .override(OS.MAC_OS_X, pathToRedis);
        var server = new RedisServer(provider, port);
        server.start();
        return server;
    }
}
