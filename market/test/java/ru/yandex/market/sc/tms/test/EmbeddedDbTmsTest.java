package ru.yandex.market.sc.tms.test;

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

import ru.yandex.market.sc.core.config.ScCoreInternalConfiguration;
import ru.yandex.market.sc.core.test.CleanupEmbeddedDbExtension;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestExternalConfiguration;
import ru.yandex.market.sc.core.test.TestInternalConfiguration;
import ru.yandex.market.sc.tms.config.ScTmsInternalConfiguration;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

/**
 * @author valter
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@SpringBootTest(classes = {
        ScCoreInternalConfiguration.class,
        ScTmsInternalConfiguration.class,
        TestExternalConfiguration.class,
        TestInternalConfiguration.class,
        TmsTestExternalConfiguration.class,
},
        properties = {
                "market.tms-core-quartz2.qrtzLogTableName=tms.qrtz_log",
                "org.quartz.jobStore.tablePrefix=tms.qrtz_",
                "org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate",
                "org.quartz.jobStore.class=org.springframework.scheduling.quartz.LocalDataSourceJobStore",
                "schrodingerBox.orderScanLog.daysToPersist=30"
        })
@ImportAutoConfiguration({HibernateJpaAutoConfiguration.class})
@ActiveProfiles(TplProfiles.TESTS)
@ExtendWith(CleanupEmbeddedDbExtension.class)
@ExtendWith(SortableFlowSwitcherExtension.class)
public @interface EmbeddedDbTmsTest {

}
