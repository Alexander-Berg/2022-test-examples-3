package ru.yandex.market.jmf.db.api.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.db.api.DbApiConfiguration;
import ru.yandex.market.jmf.entity.test.EntityApiTestConfiguration;

@Configuration
@Import({
        DbApiConfiguration.class,
        EntityApiTestConfiguration.class
})
public class DbApiTestConfiguration {
}
