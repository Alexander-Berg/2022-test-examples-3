package ru.yandex.market.jmf.logic.def.test;

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
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class UniqueAttributeGroupTest {

    private static final Fqn FQN_2 = Fqn.parse("entitySimple$type2");
    private static final Fqn FQN_11 = Fqn.parse("entitySimple$type11");
    private static final String UNIQ_GROUP_CLASS_ATTR_1 = "uniqGroupClassAttr1";
    private static final String UNIQ_GROUP_CLASS_ATTR_2 = "uniqGroupClassAttr2";
    private static final String UNIQ_GROUP_CLASS_ATTR_3 = "uniqGroupClassAttr3";
    private static final String UNIQ_GROUP_TYPE_ATTR_1 = "uniqGroupTypeAttr1";
    private static final String UNIQ_GROUP_TYPE_ATTR_2 = "uniqGroupTypeAttr2";

    @Inject
    BcpService bcpService;

    @Test
    public void testCreateUniqueGroupInClass() {
        Assertions.assertThrows(ValidationException.class, () -> {
            String value1 = Randoms.string();
            String value2 = Randoms.string();
            bcpService.create(FQN_2, Map.of(
                    UNIQ_GROUP_CLASS_ATTR_1, value1,
                    UNIQ_GROUP_CLASS_ATTR_2, value2
            ));
            bcpService.create(FQN_2, Map.of(
                    UNIQ_GROUP_CLASS_ATTR_1, value1,
                    UNIQ_GROUP_CLASS_ATTR_2, value2
            ));
        });
    }

    @Test
    public void testEditUniqueGroupInClass() {
        Assertions.assertThrows(ValidationException.class, () -> {
            String value1 = Randoms.string();
            String value2 = Randoms.string();
            Entity entity = bcpService.create(FQN_2, Map.of(
                    UNIQ_GROUP_CLASS_ATTR_1, value1,
                    UNIQ_GROUP_CLASS_ATTR_2, Randoms.string()
            ));
            bcpService.create(FQN_2, Map.of(
                    UNIQ_GROUP_CLASS_ATTR_1, value1,
                    UNIQ_GROUP_CLASS_ATTR_2, value2
            ));
            bcpService.edit(entity, Map.of(UNIQ_GROUP_CLASS_ATTR_2, value2));
        });
    }

    @Test
    public void testCreateUniqueGroupInType() {
        Assertions.assertThrows(ValidationException.class, () -> {
            String value1 = Randoms.string();
            String value2 = Randoms.string();
            String value3 = Randoms.string();
            bcpService.create(FQN_11, Map.of(
                    UNIQ_GROUP_TYPE_ATTR_1, value1,
                    UNIQ_GROUP_TYPE_ATTR_2, value2,
                    UNIQ_GROUP_CLASS_ATTR_3, value3
            ));
            bcpService.create(FQN_11, Map.of(
                    UNIQ_GROUP_TYPE_ATTR_1, value1,
                    UNIQ_GROUP_TYPE_ATTR_2, value2,
                    UNIQ_GROUP_CLASS_ATTR_3, value3
            ));
        });
    }

    @Test
    public void testEditUniqueGroupInType() {
        Assertions.assertThrows(ValidationException.class, () -> {
            String value1 = Randoms.string();
            String value2 = Randoms.string();
            String value3 = Randoms.string();
            Entity entity = bcpService.create(FQN_11, Map.of(
                    UNIQ_GROUP_TYPE_ATTR_1, Randoms.string(),
                    UNIQ_GROUP_TYPE_ATTR_2, value2,
                    UNIQ_GROUP_CLASS_ATTR_3, value3
            ));
            bcpService.create(FQN_11, Map.of(
                    UNIQ_GROUP_TYPE_ATTR_1, value1,
                    UNIQ_GROUP_TYPE_ATTR_2, value2,
                    UNIQ_GROUP_CLASS_ATTR_3, value3
            ));
            bcpService.edit(entity, Map.of(UNIQ_GROUP_TYPE_ATTR_1, value1));
        });
    }

    @Test
    public void testCreateNullValue() {
        String value1 = Randoms.string();
        String value3 = Randoms.string();
        bcpService.create(FQN_11, Map.of(
                UNIQ_GROUP_TYPE_ATTR_1, value1,
                UNIQ_GROUP_CLASS_ATTR_3, value3
        ));
        bcpService.create(FQN_11, Map.of(
                UNIQ_GROUP_TYPE_ATTR_1, value1,
                UNIQ_GROUP_CLASS_ATTR_3, value3
        ));
    }

    @Test
    public void testEditNullValue() {
        String value1 = Randoms.string();
        String value3 = Randoms.string();
        Entity entity = bcpService.create(FQN_11, Map.of(
                UNIQ_GROUP_TYPE_ATTR_1, value1,
                UNIQ_GROUP_TYPE_ATTR_2, Randoms.string(),
                UNIQ_GROUP_CLASS_ATTR_3, value3
        ));
        bcpService.create(FQN_11, Map.of(
                UNIQ_GROUP_TYPE_ATTR_1, value1,
                UNIQ_GROUP_CLASS_ATTR_3, value3
        ));
        bcpService.edit(entity, Collections.singletonMap(UNIQ_GROUP_TYPE_ATTR_2, null));
    }

    @Import(LogicDefaultTestConfiguration.class)
    public static class Configuration {
        @Bean
        public MetadataProvider provider(MetadataProviders providers) {
            return providers.of("classpath:entity_metadata.xml");
        }
    }
}
