package ru.yandex.reminders.logic.callmeback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.misc.io.http.Timeout;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class CallmebackDevpackContextConfiguration {
    private static final String DEVPACK_CONFIG = "devpack-config.yaml";

    @Bean
    public DevPackConfig devpackConfig() throws IOException {
        val yaml = Files.readAllBytes(Paths.get(DEVPACK_CONFIG));
        val mapper = new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(yaml, DevPackConfig.class);
    }

    @Bean
    public CallmebackManager callmebackManager(DevPackConfig devPackConfig) {
        val items = devPackConfig.getDictitems();
        val callmebackUrl = String.format("http://localhost:%d", items.getCallmeback().getPort());
        val remindersUrl = String.format("http://localhost:%d", items.getReminders().getPort());
        val registryMock = mock(MeterRegistry.class);
        val counter = mock(Counter.class);
        val timer = mock(Timer.class);
        when(registryMock.counter(anyString())).thenReturn(counter);
        when(registryMock.timer(anyString())).thenReturn(timer);
        // We use the same settings for maxCons and timeout, that are used by default for everything
        return new CallmebackManager(1, Timeout.seconds(1),
                Collections.singletonList(new JsonContentTypeInterceptor()),
                callmebackUrl, remindersUrl, registryMock);
    }
}
