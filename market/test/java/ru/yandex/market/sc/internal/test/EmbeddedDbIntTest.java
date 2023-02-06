package ru.yandex.market.sc.internal.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.sc.core.config.ScCoreInternalConfiguration;
import ru.yandex.market.sc.core.test.CleanupEmbeddedDbExtension;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestExternalConfiguration;
import ru.yandex.market.sc.core.test.TestInternalConfiguration;
import ru.yandex.market.sc.internal.config.ScIntInternalConfiguration;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

/**
 * @author valter
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@SpringBootTest(classes = {
        ScCoreInternalConfiguration.class,
        ScIntInternalConfiguration.class,
        TestExternalConfiguration.class,
        TestInternalConfiguration.class,
        TestIntConfiguration.class,
},
        properties = {
                "spring.jpa.properties.hibernate.metadata_builder_contributor=" +
                        "ru.yandex.market.tpl.common.db.hibernate.TplMetadataBuilderContributor"
        })
@AutoConfigureMockMvc
@ImportAutoConfiguration({HibernateJpaAutoConfiguration.class})
@ActiveProfiles(TplProfiles.TESTS)
@ExtendWith(CleanupEmbeddedDbExtension.class)
@ExtendWith({SortableFlowSwitcherExtension.class})
public @interface EmbeddedDbIntTest {

}
