package ru.yandex.market.sc.api.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.sc.api.config.ScApiInternalConfiguration;
import ru.yandex.market.sc.core.test.CleanupEmbeddedDbExtension;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestExternalConfiguration;
import ru.yandex.market.sc.core.test.TestInternalConfiguration;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

/**
 * @author valter
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@SpringBootTest(classes = {
        ScApiInternalConfiguration.class,
        TestExternalConfiguration.class,
        TestInternalConfiguration.class,
        TestApiConfiguration.class
})
@AutoConfigureMockMvc
@ImportAutoConfiguration({HibernateJpaAutoConfiguration.class, ValidationAutoConfiguration.class})
@ActiveProfiles(TplProfiles.TESTS)
@ExtendWith({CleanupEmbeddedDbExtension.class, SpringExtension.class})
@ExtendWith({SortableFlowSwitcherExtension.class})
@DbUnitDataSet(truncateAllTables = false)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        MockitoTestExecutionListener.class
})
public @interface ScApiControllerTest {

}
