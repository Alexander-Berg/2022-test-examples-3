package ru.yandex.market.pvz.core.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pvz.core.config.PvzCoreInternalConfiguration;
import ru.yandex.market.tpl.common.logbroker.config.LogbrokerTestExternalConfig;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

/**
 * @author valter
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@SpringBootTest(classes = {
        PvzCoreInternalConfiguration.class,
        TestInternalConfiguration.class,
        TestExternalConfiguration.class,
        LogbrokerTestExternalConfig.class
})
@ImportAutoConfiguration({HibernateJpaAutoConfiguration.class})
@ActiveProfiles(TplProfiles.TESTS)
@Transactional
@Deprecated
public @interface EmbeddedDbTest {

}
