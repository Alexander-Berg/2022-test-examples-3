package ru.yandex.market.jmf.module.def.test;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityDtoService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.ui.api.UiConstants;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(classes = EntityDtoServiceTest.Configuration.class)
public class EntityDtoServiceTest {

    @Inject
    BcpService bcpService;

    @Inject
    EntityDtoService entityDtoService;

    @Test
    public void gid() {
        Entity e = createEntity();
        Map<String, Object> dto = entityDtoService.convert(e, Collections.emptyList());
        Assertions.assertTrue(dto.containsKey(Entity.GID), "DTO объект должен всегда содержать GID");
    }

    @Test
    public void metaclass() {
        Entity e = createEntity();
        Map<String, Object> dto = entityDtoService.convert(e, Collections.emptyList());
        Assertions.assertTrue(dto.containsKey(Entity.METACLASS), "DTO объект должен всегда содержать metaclass");
    }

    @Test
    public void attrWithoutExtension() {
        Entity e = createEntity();
        Map<String, Object> dto = entityDtoService.convert(e, Collections.emptyList());
        Assertions.assertFalse(
                dto.containsKey("attrWithoutExtension"),
                "DTO объект не должен содержать attrWithoutExtension т.к. его явно не запросили");
    }

    @Test
    public void attrWithoutExtension_explicit() {
        Entity e = createEntity();
        Map<String, Object> dto = entityDtoService.convert(e, Collections.singleton("attrWithoutExtension"));
        Assertions.assertTrue(
                dto.containsKey("attrWithoutExtension"), "DTO объект не должен содержать attrWithoutExtension т.к. " +
                        "его явно не запросили");
    }

    @Test
    public void attrWithExtension() {
        Entity e = createEntity();
        Map<String, Object> dto = entityDtoService.convert(e, Collections.emptyList());
        Assertions.assertTrue(
                dto.containsKey("attrWithExtension"),
                "DTO объект должен содержать attrWithExtension т.к. это задано в DtoAttributeExtension");
    }

    @Test
    public void withUiMeta() {
        Entity e = createEntity();
        Map<String, Object> dto = entityDtoService.convert(e, Collections.emptyList());
        Assertions.assertTrue(
                dto.containsKey(UiConstants.PERMISSIONS), "DTO объект должен по умолчанию содержать UI поля");
    }

    @Test
    public void withoutUiMeta() {
        Entity e = createEntity();

        Map<String, Object> dtoWithUi = entityDtoService.convert(e, Collections.emptyList(), false, false);
        Assertions.assertTrue(
                dtoWithUi.containsKey(UiConstants.PERMISSIONS), "DTO объект должен содержать UI поля");

        Map<String, Object> dtoWithoutUi = entityDtoService.convert(e, Collections.emptyList(), false, true);
        Assertions.assertFalse(
                dtoWithoutUi.containsKey(UiConstants.PERMISSIONS), "DTO объект не должен содержать UI поля");
    }

    private Entity createEntity() {
        return bcpService.create(Fqn.of("simpleEntity"), Maps.of(
                "attrWithoutExtension", Randoms.string(),
                "attrWithExtension", Randoms.string()
        ));
    }

    @Import({ModuleDefaultTestConfiguration.class})
    public static class Configuration {
        @Bean
        public MetadataProvider provider(MetadataProviders providers) {
            return providers.of("classpath:metadata/dtoService.xml");
        }
    }
}
