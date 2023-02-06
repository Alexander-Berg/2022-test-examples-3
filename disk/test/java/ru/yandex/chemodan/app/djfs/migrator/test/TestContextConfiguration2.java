package ru.yandex.chemodan.app.djfs.migrator.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.djfs.migrator.DjfsMigratorContext;

/**
 * @author yappo
 */

@Configuration
@Import({
        DjfsMigratorContext.class
})
public class TestContextConfiguration2 {
}
