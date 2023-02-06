package ru.yandex.market.pvz.tms.test;

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

import ru.yandex.market.pvz.core.test.TestExternalConfiguration;
import ru.yandex.market.pvz.core.test.TestInternalConfiguration;
import ru.yandex.market.pvz.tms.config.PvzTmsInternalConfiguration;
import ru.yandex.market.tpl.common.web.config.TplJettyConfiguration;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

/**
 * @author kukabara
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@SpringBootTest(classes = {
        PvzTmsInternalConfiguration.class,
        TestExternalConfiguration.class,
        TestInternalConfiguration.class,
        PvzTmsTestExternalConfiguration.class,
        TplJettyConfiguration.class
},
        properties = {
                "market.tms-core-quartz2.qrtzLogTableName=tms.qrtz_log",
                "org.quartz.jobStore.tablePrefix=tms.qrtz_",
                "org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate"
        })
@ImportAutoConfiguration({HibernateJpaAutoConfiguration.class})
@ActiveProfiles(TplProfiles.TESTS)
@Transactional
public @interface EmbeddedDbTmsTest {

}
