package ru.yandex.market.jmf.logic.def.test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityAdapterService;
import ru.yandex.market.jmf.entity.HasId;
import ru.yandex.market.jmf.entity.impl.adapter.EntityAdapter;
import ru.yandex.market.jmf.metadata.AttributeNotFoundException;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class EntityTest {

    private static final Fqn FQN_0 = Fqn.parse("entitySimple");
    private static final Fqn FQN_1 = Fqn.parse("entitySimple$type1");
    private static final Fqn FQN_2 = Fqn.parse("entitySimple$type2");
    private static final Fqn FQN_DV = Fqn.parse("entityWithDefaultValue");
    private static final String ATTR_0 = "attr0";
    private static final String ATTR_1 = "attr1";
    private static final String ATTR_2 = "attr2";

    @Inject
    BcpService bcpService;
    @Inject
    DbService dbService;
    @Inject
    EntityAdapterService entityAdapterService;

    @Test
    public void metaclass() {
        Entity entity = bcpService.create(FQN_2, ImmutableMap.of(ATTR_0, UUID.randomUUID()));

        Metaclass result = entity.getMetaclass();

        // проверяем правильность заполнение как атрибутов определенных в Типе, так и в Классе
        Assertions.assertEquals(FQN_2, result.getFqn());
    }

    @Test
    public void fqn() {
        Entity entity = bcpService.create(FQN_2, ImmutableMap.of(ATTR_0, UUID.randomUUID()));

        Fqn result = entity.getFqn();

        // проверяем правильность заполнение как атрибутов определенных в Типе, так и в Классе
        Assertions.assertEquals(FQN_2, result);
    }

    @Test
    public void create_2() {
        String value0 = Randoms.string();
        String value2 = Randoms.string();
        Entity result = bcpService.create(FQN_2, ImmutableMap.of(ATTR_0, value0, ATTR_2, value2));

        // проверяем правильность заполнение как атрибутов определенных в Типе, так и в Классе
        Assertions.assertEquals(value0, result.getAttribute(ATTR_0));
        Assertions.assertEquals(value2, result.getAttribute(ATTR_2));
    }

    @Test
    public void replicate() {
        String value0 = Randoms.string();
        String value2 = Randoms.string();
        long id = Randoms.unsignedLongValue(1_000_000);
        Entity result = bcpService.replicate(FQN_2, FQN_2.gidOf(id), ImmutableMap.of(
                ATTR_0, value0,
                ATTR_2, value2
        ));

        // проверяем правильность заполнение id объекта
        Assertions.assertEquals(id, ((HasId) ((EntityAdapter) result).adaptee()).getId());
    }

    @Test
    public void delete() {
        // настройка системы
        Entity e = create();
        // вызов системы
        bcpService.delete(e.getGid());
        // проверяем правильность заполнение id объекта
        Entity actualValue = dbService.get(e.getGid());
        Assertions.assertNull(actualValue, "Объект должен быть удален");
    }

    @Test
    public void create_attr() {
        String value = Randoms.string();

        Entity result = create(value);

        String attr0 = result.getAttribute(ATTR_0);
        Assertions.assertEquals(value, attr0, "Должны заполнить значение аттрибута");
    }

    @Test
    public void create_entityAttrCode() {
        String value = Randoms.string();
        Entity result = create(value);

        String attr0 = result.getAttribute(ATTR_0);
        Assertions.assertEquals(value, attr0, "Должны заполнить значение аттрибута");
    }

    @Test
    public void create_entityAttrCode_notExists() {
        Assertions.assertThrows(AttributeNotFoundException.class, () -> {
            String value = Randoms.string();
            Entity result = create(value);

            result.getAttribute(Randoms.string());
        });
    }

    @Test
    public void create_entityAttr() {
        String value = Randoms.string();
        Entity result = create(value);

        String attr0 = result.getAttribute(result.getMetaclass().getAttributeOrError(ATTR_0));
        Assertions.assertEquals(value, attr0, "Должны заполнить значение аттрибута");
    }

    @Test
    public void create_entityAttr_null() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            String value = Randoms.string();
            Entity result = create(value);

            result.getAttribute((Attribute) null);
        });
    }

    @Test
    public void create_attrWithDefaultValue() {
        Entity result = bcpService.create(FQN_DV, Collections.emptyMap());

        String attr0 = result.getAttribute(ATTR_0);
        Assertions.assertEquals("Default Value", attr0, "Должны заполнить значение аттрибута");
    }

    @Test
    public void create_createTime() {
        Entity result = create();

        OffsetDateTime creationTime = result.getAttribute("creationTime");
        Assertions.assertNotNull(creationTime, "Должны заполнить время создания объекта");
        Assertions.assertTrue(
                isNow(creationTime),
                "Вемя создания должно совпадать с текущим временем (с учетом разницы получения времен)");
    }

    @Test
    public void create_error() {
        Assertions.assertThrows(ValidationException.class, () -> {
            String value = Randoms.string();
            bcpService.create(FQN_0, ImmutableMap.of(ATTR_0, value));
            // Должны получиьт ошибку т.к. пытаемся создать объект типизированного класса без указания типа
        });
    }

    @Test
    public void edit_attr() {
        Entity entity = create();

        String value = Randoms.string();
        Entity result = bcpService.edit(entity, properties(value));

        String attr0 = result.getAttribute(ATTR_0);
        Assertions.assertEquals(value, attr0, "Должны заполнить значение аттрибута");
    }

    @Test
    public void modifyCreationTime() {
        Entity result = create();
        OffsetDateTime creationTime = result.getAttribute("creationTime");

        Map<String, Object> properties = new HashMap<>();
        properties.put("creationTime", Now.offsetDateTime());

        Entity result2 = bcpService.edit(result, properties);

        OffsetDateTime newCreationTime = result2.getAttribute("creationTime");
        Assertions.assertEquals(creationTime, newCreationTime);
    }

    @Test
    public void wrapWithAttributes_byAttribute() {
        Entity entity = create();

        String value = Randoms.string();
        Entity wrapped = entityAdapterService.wrap(entity, Maps.of(ATTR_0, value));
        Attribute attribute = wrapped.getMetaclass().getAttributeOrError(ATTR_0);
        Object result = wrapped.getAttribute(attribute);

        Assertions.assertEquals(value, result);
    }

    @Test
    public void wrapWithAttributes_byCode() {
        Entity entity = create();

        String value = Randoms.string();
        Entity wrapped = entityAdapterService.wrap(entity, Maps.of(ATTR_0, value));
        Object result = wrapped.getAttribute(ATTR_0);

        Assertions.assertEquals(value, result);
    }

    @Test
    public void wrapWithAttributes_byMethod() {
        SimpleType1Entity entity = create();

        String value = Randoms.string();
        SimpleType1Entity wrapped = entityAdapterService.wrap(entity, Maps.of(ATTR_0, value));
        String result = wrapped.getAttr0();

        Assertions.assertEquals(value, result);
    }

    @Test
    public void wrapWithAttributes_otherAttr() {
        Entity entity = create();

        String value = Randoms.string();
        Entity wrapped = entityAdapterService.wrap(entity, Maps.of(ATTR_2, value));
        Object result = wrapped.getAttribute(ATTR_0);

        Assertions.assertEquals(entity.getAttribute(ATTR_0), result);
    }

    @Test
    public void wrapWithAttributes_wrapOfWrapped_override_getBy() {
        SimpleType1Entity entity = create();

        String value = Randoms.string();
        SimpleType1Entity wrapped = entityAdapterService.wrap(entity, Maps.of(ATTR_0, value));
        Attribute attribute = wrapped.getMetaclass().getAttributeOrError(ATTR_0);
        Assertions.assertEquals(value, wrapped.getAttribute(attribute));
        Assertions.assertEquals(value, wrapped.getAttribute(ATTR_0));
        Assertions.assertEquals(value, wrapped.getAttr0());

        String nextValue = Randoms.string();
        SimpleType1Entity wrappedOfWrapped = entityAdapterService.wrap(wrapped, Maps.of(ATTR_0, nextValue));
        attribute = wrappedOfWrapped.getMetaclass().getAttributeOrError(ATTR_0);
        Assertions.assertEquals(nextValue, wrappedOfWrapped.getAttribute(attribute));
        Assertions.assertEquals(nextValue, wrappedOfWrapped.getAttribute(ATTR_0));
        Assertions.assertEquals(nextValue, wrappedOfWrapped.getAttr0());
    }

    @Test
    public void wrapWithAttributes_wrapOfWrapped_overrideAttribute() {
        SimpleType1Entity entity = create();

        String value = Randoms.string();
        SimpleType1Entity wrapped = entityAdapterService.wrap(entity, Maps.of(ATTR_0, value));
        Assertions.assertEquals(value, wrapped.getAttr0());

        String nextValue = Randoms.string();
        SimpleType1Entity wrappedOfWrapped = entityAdapterService.wrap(wrapped, Maps.of(ATTR_0, nextValue));
        Assertions.assertEquals(nextValue, wrappedOfWrapped.getAttr0());
    }

    @Test
    public void wrapWithAttributes_wrapOfWrapped_addAttribute() {
        SimpleType1Entity entity = create();

        String valueAttr0 = Randoms.string();
        SimpleType1Entity wrapped = entityAdapterService.wrap(entity, Maps.of(ATTR_0, valueAttr0));
        Assertions.assertEquals(valueAttr0, wrapped.getAttr0());
        Assertions.assertNull(wrapped.getAttr1());

        String valueAttr1 = Randoms.string();
        SimpleType1Entity wrappedOfWrapped = entityAdapterService.wrap(wrapped, Maps.of(ATTR_1, valueAttr1));
        Assertions.assertEquals(valueAttr0, wrappedOfWrapped.getAttr0());
        Assertions.assertEquals(valueAttr1, wrappedOfWrapped.getAttr1());
    }

    @Test
    public void wrapWithAttributes_wrapOfWrapped_overrideAndAddAttribute() {
        SimpleType1Entity entity = create();

        String valueAttr0 = Randoms.string();
        SimpleType1Entity wrapped = entityAdapterService.wrap(entity, Maps.of(ATTR_0, valueAttr0));
        Assertions.assertEquals(valueAttr0, wrapped.getAttr0());
        Assertions.assertNull(wrapped.getAttr1());

        String newValueAttr0 = Randoms.string();
        String valueAttr1 = Randoms.string();
        SimpleType1Entity wrappedOfWrapped = entityAdapterService.wrap(wrapped, Maps.of(
                ATTR_0, newValueAttr0,
                ATTR_1, valueAttr1));
        Assertions.assertEquals(newValueAttr0, wrappedOfWrapped.getAttr0());
        Assertions.assertEquals(valueAttr1, wrappedOfWrapped.getAttr1());
    }

    @Test
    public void wrapWithAttributes_wrapOfWrapped_setNullAttribute() {
        SimpleType1Entity entity = create();

        String valueAttr0 = Randoms.string();
        SimpleType1Entity wrapped = entityAdapterService.wrap(entity, Maps.of(ATTR_0, valueAttr0));
        Assertions.assertEquals(valueAttr0, wrapped.getAttr0());

        SimpleType1Entity wrappedOfWrapped = entityAdapterService.wrap(wrapped, Maps.of(ATTR_0, null));
        Assertions.assertNull(wrappedOfWrapped.getAttr0());
    }

    @Test
    public void wrapWithAttributes_equals() {
        SimpleType1Entity entity = create();

        String value = Randoms.string();
        SimpleType1Entity wrapped = entityAdapterService.wrap(entity, Maps.of(ATTR_0, value));

        Assertions.assertEquals(entity, wrapped);
    }

    private SimpleType1Entity create(String attr0Value) {
        Map<String, Object> properties = properties(attr0Value);
        return bcpService.create(FQN_1, properties);
    }

    private SimpleType1Entity create() {
        return create(Randoms.string());
    }

    private boolean isNow(OffsetDateTime value) {
        OffsetDateTime now = Now.offsetDateTime();
        return 2 > Math.abs(now.toEpochSecond() - value.toEpochSecond());
    }

    private Map<String, Object> properties(String attr0Value) {
        return ImmutableMap.of(ATTR_0, attr0Value);
    }

}
