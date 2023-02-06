package ru.yandex.market.b2b.clients.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.tables.YtTables;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class YtMock {
    @Bean
    @Primary
    public Yt yt() {
        Yt yt = mock(Yt.class);
        when(yt.tables()).thenReturn(mock(YtTables.class));
        return yt;
    }
}
