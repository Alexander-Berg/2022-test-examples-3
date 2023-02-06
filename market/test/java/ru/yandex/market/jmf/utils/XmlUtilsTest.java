package ru.yandex.market.jmf.utils;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.jmf.utils.test.gen.extension.ExtensionElement;
import ru.yandex.market.jmf.utils.test.gen.root.RealRootElement;
import ru.yandex.market.jmf.utils.test.gen.root.Root;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = XmlUtilsTest.TestConf.class)
public class XmlUtilsTest {

    @Inject
    XmlUtils xmlUtils;

    /**
     * Проверяем возможность доопределить типы в отдельной xsd
     */
    @Test
    public void deserealizeMulti() {
        Root obj = xmlUtils.readResource("/unmarshaller-test.xml", Root.class);

        Assertions.assertEquals(2, obj.getEl().size());
        RealRootElement e1 = (RealRootElement) obj.getEl().get(0);
        Assertions.assertEquals("rootValue", e1.getRealAttr());
        ExtensionElement e2 = (ExtensionElement) obj.getEl().get(1);
        Assertions.assertEquals("extensionValue", e2.getExtensionAttr());
    }

    @Configuration
    @Import(UtilsTestConfiguration.class)
    public static class TestConf {
        @Bean
        public JaxbObjectFactoryProvider testUtilsJaxbObjectFactoryProvider() {
            return new DefaultJaxbObjectFactoryProvider(
                    ru.yandex.market.jmf.utils.test.gen.root.ObjectFactory.class,
                    ru.yandex.market.jmf.utils.test.gen.extension.ObjectFactory.class
            );
        }
    }
}
