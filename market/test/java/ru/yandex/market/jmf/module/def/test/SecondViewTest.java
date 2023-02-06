package ru.yandex.market.jmf.module.def.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.module.def.SecondViewExtension;
import ru.yandex.market.jmf.module.def.impl.SecondViewUiCardExtensionStrategy;
import ru.yandex.market.jmf.ui.api.UiApiConfiguration;
import ru.yandex.market.jmf.ui.api.UiService;
import ru.yandex.market.jmf.ui.api.content.Card;
import ru.yandex.market.jmf.utils.Maps;

@SpringJUnitConfig(classes = SecondViewTest.Configuration.class)
public class SecondViewTest {

    @Inject
    MetadataService metadataService;
    @Inject
    UiService uiService;
    @Inject
    BcpService bcpService;

    @Test
    public void checkExtension() {
        Metaclass mc = metadataService.getMetaclassOrError(Fqn.of("withSecondViewEntity"));
        SecondViewExtension extension = mc.getExtension(SecondViewExtension.class);

        Assertions.assertNotNull(extension, "В метаклассе определено расширение SecondView");
        Assertions.assertEquals(mc.getAttributeOrError("secondViewAttr"), extension.getAttribute());
        Assertions.assertEquals("view", extension.getCard());
    }

    @Test
    @Transactional
    public void checkCardExtension() {
        // настройка системы
        Entity secondViewEntity = bcpService.create(Fqn.of("simpleEntity"), Maps.of());
        Entity withSecondViewEntity = bcpService.create(
                Fqn.of("withSecondViewEntity"),
                Maps.of("secondViewAttr", secondViewEntity)
        );

        // вызов системы
        Card card = uiService.getCard(withSecondViewEntity, "view");

        // проверка утверждений
        SecondViewUiCardExtensionStrategy.SecondViewUiExtension extension = extractExtension(card);

        Assertions.assertEquals(secondViewEntity.getGid(), extension.getGid());
        Assertions.assertEquals("secondViewAttr", extension.getAttribute());
        Assertions.assertEquals("view", extension.getCard());
    }

    @Test
    @Transactional
    public void checkCardExtension_null() {
        // настройка системы
        Entity withSecondViewEntity = bcpService.create(
                Fqn.of("withSecondViewEntity"),
                Maps.of("secondViewAttr", null)
        );

        // вызов системы
        Card card = uiService.getCard(withSecondViewEntity, "view");

        // проверка утверждений
        SecondViewUiCardExtensionStrategy.SecondViewUiExtension extension = extractExtension(card);

        Assertions.assertNull(extension.getGid());
        Assertions.assertEquals("secondViewAttr", extension.getAttribute());
        Assertions.assertEquals("view", extension.getCard());
    }

    @Test
    @Transactional
    public void checkCardExtension_gid() {
        // настройка системы
        Entity secondViewEntity = bcpService.create(Fqn.of("simpleEntity"), Maps.of());
        Entity withSecondViewEntity = bcpService.create(
                Fqn.of("withSecondViewGidEntity"),
                Maps.of("secondViewAttr", secondViewEntity)
        );

        // вызов системы
        Card card = uiService.getCard(withSecondViewEntity, "view");

        // проверка утверждений
        SecondViewUiCardExtensionStrategy.SecondViewUiExtension extension = extractExtension(card);

        Assertions.assertEquals(secondViewEntity.getGid(), extension.getGid());
        Assertions.assertEquals("secondViewAttr", extension.getAttribute());
        Assertions.assertEquals("view", extension.getCard());
    }

    private SecondViewUiCardExtensionStrategy.SecondViewUiExtension extractExtension(Card card) {
        return (SecondViewUiCardExtensionStrategy.SecondViewUiExtension) card.getExtensions().get("secondView");
    }

    @Import({ModuleDefaultTestConfiguration.class, UiApiConfiguration.class})
    public static class Configuration {

        @Bean
        public MetadataProvider provider(MetadataProviders providers) {
            return providers.of("classpath:metadata/secondView.xml");
        }
    }
}
