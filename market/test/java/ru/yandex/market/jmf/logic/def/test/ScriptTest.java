package ru.yandex.market.jmf.logic.def.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.entity.HasId;
import ru.yandex.market.jmf.entity.impl.adapter.EntityAdapter;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.script.ScriptService;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class ScriptTest {

    public static final Fqn FQN = Fqn.of("entitySimple$type1");

    @Inject
    BcpService bcpService;
    @Inject
    ScriptService scriptService;
    @Inject
    DbService dbService;

    @Test
    public void checkEquals() {
        Entity e = create();

        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o1", e,
                "o2", e
        );
        String script = "o1 == o2";
        boolean result = Boolean.TRUE.equals(scriptService.execute(script, variables));
        Assertions.assertTrue(result);
    }

    @Test
    public void checkSelfEquals() {
        Entity e = create();

        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o1", e
        );
        String script = "o1 == o1";
        boolean result = Boolean.TRUE.equals(scriptService.execute(script, variables));
        Assertions.assertTrue(result);
    }

    @Test
    public void checkNullEquals() {
        Entity e = create();

        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o1", e
        );
        String script = "o1 == null";
        boolean result = Boolean.TRUE.equals(scriptService.execute(script, variables));
        Assertions.assertFalse(result);
    }

    @Test
    public void checkHashCode() {
        Entity e = create();

        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o1", e,
                "h1", e.hashCode()
        );
        String script = "o1.hashCode() == h1";
        boolean result = Boolean.TRUE.equals(scriptService.execute(script, variables));
        Assertions.assertTrue(result);
    }

    @Test
    public void checkId() {
        Entity e = create();

        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o1", e
        );
        String script = "o1.id";
        Object result = scriptService.execute(script, variables);
        Assertions.assertEquals(((HasId) ((EntityAdapter) e).adaptee()).getId(), result);
    }

    @Test
    public void checkMetaclass() {
        Entity e = create();

        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o1", e
        );
        String script = "o1.metaclass.fqn";
        Object result = scriptService.execute(script, variables);
        Assertions.assertEquals(FQN, result);
    }

    @Test
    public void checkToString() {
        Entity e = create();

        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o1", e
        );
        String script = "\"\" + o1";
        Object result = scriptService.execute(script, variables);
        Assertions.assertEquals(e.getGid(), result);
    }

    @Test
    public void checkWrapped() {
        Entity e = create();

        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o1", e
        );
        String script = "\"EntityValue\" == o1.getClass().getSimpleName()";
        boolean result = Boolean.TRUE.equals(scriptService.execute(script, variables));
        Assertions.assertTrue(result, "Объект должен быть обернут с помощью ScriptValue для избегания модификации");
    }

    @Test
    public void checkCollectionWrapped() {
        Entity e = create();

        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o1", e,
                "c1", Sets.newHashSet(e)
        );
        String script = "c1.contains(o1)";
        boolean result = Boolean.TRUE.equals(scriptService.execute(script, variables));
        Assertions.assertTrue(result, "Коллекция содержит объект");
    }

    @Test
    public void checkCollectionUnmodifiable() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableMap<String, Object> variables = ImmutableMap.of(
                    "c1", Sets.newHashSet()
            );
            String script = "c1.add(\"v1\")";
            scriptService.execute(script, variables);
        });
    }

    /**
     * Проверяем работоспособность создания объекта через api (api.bcp.create(...))
     */
    @Test
    public void api_create() {
        String attrValue = Randoms.string();

        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "attrValue", attrValue
        );
        String script = "api.bcp.create('entitySimple$type1', ['attr0': attrValue])";
        Object result = scriptService.execute(script, variables);

        // проверка утверждений
        Assertions.assertNotNull(result);
        Entity e = dbService.get(result.toString());
        String value = e.getAttribute("attr0");
        Assertions.assertEquals(attrValue, value);
    }

    /**
     * Проверяем работоспособность редактирования объекта через api (api.bcp.edit(...))
     */
    @Test
    public void api_edit() {
        Entity entity = create();
        String attrValue = Randoms.string();

        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "entity", entity.getGid(),
                "attrValue", attrValue
        );
        String script = "api.bcp.edit(entity, ['attr0': attrValue])";
        scriptService.execute(script, variables);

        // проверка утверждений
        String value = entity.getAttribute("attr0");
        Assertions.assertEquals(attrValue, value);
    }

    /**
     * Проверяем работоспособность редактирования объекта через api (api.bcp.edit(...))
     */
    @Test
    public void api_delete() {
        Entity entity = create();
        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "entity", entity.getGid()
        );
        String script = "api.bcp.delete(entity)";
        scriptService.execute(script, variables);

        // проверка утверждений
        Entity actual = dbService.get(entity.getGid());
        Assertions.assertNull(actual, "Объект должен отсутствовать");
    }

    @Test
    public void apiDbSelect() {
        Entity e0 = create();
        Entity e1 = create();

        // вызов системы
        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o0", e0,
                "o1", e1
        );
        String script = "api.db.select('from entitySimple$type1 o where o.attr0 = :v0', ['v0': o0.attr0])";
        Collection<HasGid> result = scriptService.execute(script, variables);

        // проверка утверждений
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(e0.getGid(), Iterables.get(result, 0).getGid());
    }

    @Test
    public void apiDbSelectLimit1() {
        Map<String, Object> properties = ImmutableMap.of("attr0", Randoms.string());
        Entity e0 = bcpService.create(FQN, properties);
        Entity e1 = bcpService.create(FQN, properties);

        // вызов системы
        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o0", e0,
                "o1", e1
        );
        String script = "api.db.select('from entitySimple$type1 o where o.attr0 = :v0', ['v0': o0.attr0], 1)";
        Collection<HasGid> result = scriptService.execute(script, variables);

        // проверка утверждений
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void apiDbSelectLimit10() {
        Map<String, Object> properties = ImmutableMap.of("attr0", Randoms.string());
        Entity e0 = bcpService.create(FQN, properties);
        Entity e1 = bcpService.create(FQN, properties);

        // вызов системы
        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o0", e0,
                "o1", e1
        );
        String script = "api.db.select('from entitySimple$type1 o where o.attr0 = :v0', ['v0': o0.attr0], 10)";
        Collection<HasGid> result = scriptService.execute(script, variables);

        // проверка утверждений
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
    }

    @Test
    public void apiDbOfGet() {
        Entity e0 = create();

        // вызов системы
        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o0", e0
        );
        String script = "api.db.of('entitySimple$type1').get(o0.gid)";
        HasGid result = scriptService.execute(script, variables);

        // проверка утверждений
        Assertions.assertNotNull(result);
        Assertions.assertEquals(e0.getGid(), result.getGid());
    }

    @Test
    public void apiDbOfGetNull() {
        // вызов системы
        String script = "api.db.of('entitySimple$type1').get(null)";
        HasGid result = scriptService.execute(script, ImmutableMap.of());

        // проверка утверждений
        Assertions.assertNull(result);
    }

    @Test
    public void apiDbOfWithEqFilterList() {
        Entity e0 = create();

        // вызов системы
        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o0", e0
        );
        String script = "api.db.of('entitySimple$type1').withFilters { eq('attr0', o0.attr0) }.list()";
        Collection<HasGid> result = scriptService.execute(script, variables);

        // проверка утверждений
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(e0.getGid(), Iterables.get(result, 0).getGid());
    }

    @Test
    public void apiDbOfInFilterList() {
        Entity e0 = create();
        Entity e1 = create();
        create();

        // вызов системы
        ImmutableMap<String, Object> variables = ImmutableMap.of(
                "o0", e0,
                "o1", e1
        );
        String script = "api.db.of('entitySimple$type1').withFilters { _in('attr0', [o0.attr0, o1.attr0]) }.list()";
        Collection<HasGid> result = scriptService.execute(script, variables);

        // проверка утверждений
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Collection<String> gids = Collections2.transform(result, HasGid::getGid);
        Assertions.assertTrue(gids.contains(e0.getGid()));
        Assertions.assertTrue(gids.contains(e1.getGid()));
    }

    @Test
    public void apiDbOfOrderAscList() {
        Entity e0 = bcpService.create(FQN, ImmutableMap.of("attr0", "0_lowValue"));
        Entity e1 = bcpService.create(FQN, ImmutableMap.of("attr0", "1_bigValue"));

        // вызов системы
        ImmutableMap<String, Object> variables = ImmutableMap.of();
        String script = "api.db.of('entitySimple$type1').withOrders(api.db.orders.asc('attr0')).list()";
        List<HasGid> result = scriptService.execute(script, variables);

        // проверка утверждений
        Assertions.assertNotNull(result);
        List<String> gids = Lists.transform(result, HasGid::getGid);
        int lPosition = gids.indexOf(e0.getGid());
        int bPosition = gids.indexOf(e1.getGid());

        Assertions.assertTrue(lPosition < bPosition);
    }

    @Test
    public void apiDbOfOrderDescList() {
        Entity e0 = bcpService.create(FQN, ImmutableMap.of("attr0", "0_lowValue"));
        Entity e1 = bcpService.create(FQN, ImmutableMap.of("attr0", "1_bigValue"));

        // вызов системы
        ImmutableMap<String, Object> variables = ImmutableMap.of();
        String script = "api.db.of('entitySimple$type1').withOrders(api.db.orders.desc('attr0')).list()";
        List<HasGid> result = scriptService.execute(script, variables);

        // проверка утверждений
        Assertions.assertNotNull(result);
        List<String> gids = Lists.transform(result, HasGid::getGid);
        int lPosition = gids.indexOf(e0.getGid());
        int bPosition = gids.indexOf(e1.getGid());

        Assertions.assertTrue(lPosition > bPosition);
    }


    private Entity create() {
        Map<String, Object> properties = ImmutableMap.of("attr0", Randoms.string());
        return bcpService.create(FQN, properties);
    }
}
