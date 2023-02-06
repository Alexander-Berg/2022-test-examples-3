package ru.yandex.chemodan.app.telemost;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.telemost.bazinga.task.TelemostTasksContextConfiguration;
import ru.yandex.commune.bazinga.context.BazingaClientContextConfiguration;
import ru.yandex.commune.bazinga.impl.storage.BazingaStorage;
import ru.yandex.commune.bazinga.impl.storage.memory.InMemoryBazingaStorage;

@Configuration
@Import({
        TelemostTasksContextConfiguration.class,
        BazingaClientContextConfiguration.class,
})
public class TelemostTestBazingaContextConfiguration {

    @Bean
    public BazingaStorage bazingaStorage() {
        return new InMemoryBazingaStorage();
    }
}
