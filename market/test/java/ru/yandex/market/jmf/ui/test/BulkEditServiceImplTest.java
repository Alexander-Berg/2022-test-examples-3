package ru.yandex.market.jmf.ui.test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.ui.api.content.AttributeDescriptor;
import ru.yandex.market.jmf.ui.api.content.EditProperties;
import ru.yandex.market.jmf.ui.impl.BulkEditServiceImpl;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Transactional
@SpringJUnitConfig(classes = InternalUiTestConfiguration.class)
@TestPropertySource({"classpath:/do_not_require_getters_for_all_attributes.properties"})
public class BulkEditServiceImplTest {

    public static final Fqn CLASS_FQN = Fqn.of("ticket");
    public static final Fqn ROOT_FQN = Fqn.of("ticket$root");
    public static final Fqn TYPE_ONE_FQN = Fqn.of("ticket$one");
    public static final Fqn TYPE_TWO_FQN = Fqn.of("ticket$two");

    public static final String COMMON_ATTR_CODE = "commonAttr";
    public static final String ATTR_ONE_CODE = "attrOne";
    public static final String ATTR_TWO_CODE = "attrTwo";

    @Inject
    BulkEditServiceImpl bulkEditService;

    @Inject
    private BcpService bcpService;

    @Inject
    private MetadataService metadataService;

    @Test
    public void testFormHasCommonAttributesOnly() {
        var entities = new ArrayList<Entity>();

        entities.add(createEntityOne());
        entities.add(createEntityTwo());

        var response = bulkEditService.generateBulkEditCard(entities, CLASS_FQN);

        var editProperties = (EditProperties) response.getForm();

        List<AttributeDescriptor> attrsDescriptor = editProperties.getAttributes();

        assertEquals(ROOT_FQN.toString(), response.getMetaclass());
        assertFalse(getAttribute(attrsDescriptor, ATTR_ONE_CODE).isPresent());
        assertFalse(getAttribute(attrsDescriptor, ATTR_TWO_CODE).isPresent());
        assertTrue(getAttribute(attrsDescriptor, COMMON_ATTR_CODE).isPresent());
    }

    @Test
    public void testEqualCommonAttributes() {
        var entities = new ArrayList<Entity>();
        final String commonStr = "common";

        entities.add(createEntityOne(commonStr, 0));
        entities.add(createEntityOne(commonStr, 999));
        entities.add(createEntityTwo(commonStr, true));

        var response = bulkEditService.generateBulkEditCard(entities, CLASS_FQN);
        Map<String, Object> attrs = response.getAttributes();

        assertEquals(ROOT_FQN.toString(), response.getMetaclass());
        assertEquals(commonStr, attrs.get(COMMON_ATTR_CODE));
        assertFalse(attrs.containsKey(ATTR_ONE_CODE));
        assertFalse(attrs.containsKey(ATTR_TWO_CODE));
    }

    @Test
    public void testNotEqualCommonAttributes() {
        var entities = new ArrayList<Entity>();

        entities.add(createEntityOne("stringOne", 0));
        entities.add(createEntityTwo("stringTwo", true));

        var response = bulkEditService.generateBulkEditCard(entities, ROOT_FQN);
        Map<String, Object> attrs = response.getAttributes();

        assertEquals(ROOT_FQN.toString(), response.getMetaclass());
        assertFalse(attrs.containsKey(COMMON_ATTR_CODE));
        assertFalse(attrs.containsKey(ATTR_ONE_CODE));
        assertFalse(attrs.containsKey(ATTR_TWO_CODE));
    }

    @Test
    public void testNullCommonAttributes() {
        var entities = new ArrayList<Entity>();

        entities.add(createRootEntity(null));
        entities.add(createRootEntity(null));

        var response = bulkEditService.generateBulkEditCard(entities, CLASS_FQN);
        Map<String, Object> attrs = response.getAttributes();

        assertEquals(ROOT_FQN.toString(), response.getMetaclass());
        assertFalse(attrs.containsKey(COMMON_ATTR_CODE));
        assertFalse(attrs.containsKey(ATTR_ONE_CODE));
        assertFalse(attrs.containsKey(ATTR_TWO_CODE));
    }

    @Test
    public void testFormHasAttributesInAlphabeticalOrder() {
        var entities = new ArrayList<Entity>();
        final String commonStr = "common";

        entities.add(createEntityOne(commonStr, 0));
        entities.add(createEntityOne(commonStr, 0));
        entities.add(createEntityTwo(commonStr, true));

        var response = bulkEditService.generateBulkEditCard(entities, ROOT_FQN);
        List<AttributeDescriptor> attrs = ((EditProperties) response.getForm()).getAttributes();
        var lcaMetaclass = metadataService.getMetaclass(Fqn.of(response.getMetaclass()));

        assertEquals(ROOT_FQN.toString(), response.getMetaclass());

        var actual = attrs.stream()
                .map(x -> lcaMetaclass.getAttribute(x.getCode()))
                .filter(Objects::nonNull)
                .map(Attribute::getTitle)
                .collect(Collectors.toList());

        var expected = actual.stream()
                .sorted(Comparator.comparing(s -> s))
                .toArray();

        Assertions.assertArrayEquals(expected, actual.toArray());

    }

    private Optional<AttributeDescriptor> getAttribute(List<AttributeDescriptor> attrs, String code) {
        return attrs.stream().filter(x -> x.getCode().equals(code)).findAny();
    }

    private Entity createRootEntity(String str) {
        var properties = new HashMap<>(getCommonProperties());
        properties.put(COMMON_ATTR_CODE, str);

        return bcpService.create(ROOT_FQN, properties);
    }

    private Entity createEntityOne(String str, Integer num) {
        var properties = new HashMap<String, Object>();
        properties.putAll(getCommonProperties());
        properties.putAll(Maps.of(
                COMMON_ATTR_CODE, str,
                ATTR_ONE_CODE, num));

        return bcpService.create(TYPE_ONE_FQN, properties);
    }

    private Entity createEntityOne() {
        return createEntityOne(Randoms.string(), Randoms.intValue());
    }

    private Entity createEntityTwo(String str, Boolean bool) {
        var properties = new HashMap<String, Object>();
        properties.putAll(getCommonProperties());
        properties.putAll(Maps.of(
                COMMON_ATTR_CODE, str,
                ATTR_TWO_CODE, bool));

        return bcpService.create(TYPE_TWO_FQN, properties);
    }

    private Entity createEntityTwo() {
        return createEntityTwo(Randoms.string(), Randoms.booleanValue());
    }

    private Map<String, Object> getCommonProperties() {
        return Maps.of("title", Randoms.string());

    }
}
