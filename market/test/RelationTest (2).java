package ru.yandex.market.jmf.module.relation.test;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.impl.EntityData;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.hibernate.HibernateSupportConfiguration;
import ru.yandex.market.jmf.logic.def.Bo;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metainfo.MetaInfoService;
import ru.yandex.market.jmf.module.relation.AvailableRelation;
import ru.yandex.market.jmf.module.relation.Relation;
import ru.yandex.market.jmf.module.relation.impl.RelationServiceImpl;
import ru.yandex.market.jmf.script.ScriptService;
import ru.yandex.market.jmf.timings.impl.TimerTriggerHandler;
import ru.yandex.market.jmf.timings.test.impl.TimerTestUtils;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringJUnitConfig(InternalModuleRelationTestConfiguration.class)
public class RelationTest {

    @Inject
    BcpService bcpService;
    @Inject
    EntityService entityService;
    @Inject
    RelationServiceImpl relationService;
    @Inject
    ScriptService scriptService;
    @Inject
    TimerTriggerHandler timerTriggerHandler;
    @Inject
    private TimerTestUtils timerTestUtils;
    @Inject
    private MetaInfoService metaInfoService;
    @Inject
    private TxService txService;
    @Inject
    @Named(HibernateSupportConfiguration.PROPERTIES)
    private Properties hibernateProperties;

    /**
     * Сценарий:
     * <ol>
     * <li> создаем два объекта и добавляем между ними связь</li>
     * <li>редактируем один из объектов</li>
     * </ol>
     * <p>
     * Ожидаем, что сработает триггер на изменения связанного объекта, выполнится скрипт триггера, который
     * изменит атрибут второго объекта значением устанавлеваемым в первом объекте.
     */
    @Test
    @Transactional
    public void triggerOnRelated() {
        // Настройка системы
        Entity o1 = create();
        Entity o2 = create();

        bcpService.create(AvailableRelation.FQN, Map.of(
                AvailableRelation.SOURCE_TYPE, "simple",
                AvailableRelation.TARGET_TYPE, "simple",
                AvailableRelation.RELATION_TYPE, "relation$linked"
        ));

        bcpService.create(Fqn.of("relation$linked"), Maps.of(Relation.SOURCE, o1, Relation.TARGET, o2));

        // вызов системы
        String attrValue = Randoms.string();
        bcpService.edit(o1, Maps.of("attr0", attrValue));

        // проверка утверждений
        String actualValue = o2.getAttribute("attr0");
        Assertions.assertEquals(attrValue, actualValue);
    }

    @Test
    @Transactional
    public void weakUpCount() {
        // настройка системы
        Entity o1 = create();
        Entity o2 = create();
        // добавляем связь между объектами
        HasGid r = scriptService.execute("api.relation.addLinked(o1, o2)", Maps.of("o1", o1, "o2", o2));

        // вызов системы
        timerTestUtils.simulateTimerExpiration(r.getGid(), Relation.WEAK_UP_TIMER);

        // проверка утверждений
        Long count = scriptService.execute("r.weakUpCount", Maps.of("r", r));
        Assertions.assertEquals(Long.valueOf(1), count, "Должно увеличиться кол-во просыпаний связи");
    }

    @Test
    public void testThatEntityDataReloadingDoesNotBrokeAvailableRelationsCache() {
        var availableRelation = txService.doInNewTx(() -> bcpService.create(AvailableRelation.FQN, Map.of(
                AvailableRelation.SOURCE_TYPE, "simple",
                AvailableRelation.TARGET_TYPE, "simple",
                AvailableRelation.RELATION_TYPE, "relation$linked"
        )));

        txService.doInNewTx(relationService::getAllAvailableRelations);

        txService.runInNewTx(() -> metaInfoService.reload(EntityData.class));

        var actual = txService.doInNewTx(relationService::getAllAvailableRelations);
        for (AvailableRelation relation : actual) {
            relation.getSourceType();
        }
        Assertions.assertEquals(List.of(availableRelation), actual);
    }

    private Entity create() {
        return bcpService.create(Fqn.of("simple$type1"), Maps.of(Bo.TITLE, Randoms.string()));
    }


}
