package ru.yandex.market.pvz.core.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.pvz.core.config.PvzCoreInternalConfiguration;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

/**
 * @author valter
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@ExtendWith({CleanupEmbeddedDbExtension.class, ResetCachesAfterBeforeEachExtension.class})
@SpringBootTest(classes = {
        PvzCoreInternalConfiguration.class,
        TestInternalConfiguration.class,
        TestExternalConfiguration.class
})
@ImportAutoConfiguration({HibernateJpaAutoConfiguration.class})
@ActiveProfiles(TplProfiles.TESTS)
public @interface TransactionlessEmbeddedDbTest {

}
