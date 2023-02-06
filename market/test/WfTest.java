package ru.yandex.market.jmf.logic.wf.test;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(InternalLogicWfTestConfiguration.class)
public class WfTest {
    private static final Fqn FQN = Fqn.parse("wf1");
    private static final String INITIAL_STATUS = "begin";
    private static final String STATUS_1 = "st1";
    private static final String STATUS_2 = "st2";
    private static final String STATUS_3 = "st3";
    private static final String STATUS_4 = "st4";
    private static final String STATUS_5 = "st5";

    @Inject
    BcpService bcpService;
    @Inject
    DbService dbService;

    @Test
    public void confirmedTransition() {
        Entity e = bcpService.create(FQN, Collections.emptyMap());

        Entity result = bcpService.edit(e, ImmutableMap.of(HasWorkflow.STATUS, STATUS_1));

        // проверяем правильность заполнение как атрибутов определенных в Типе, так и в Классе
        Assertions.assertEquals(STATUS_1, result.getAttribute(HasWorkflow.STATUS), "Должны получить initial-статус");
    }

    @Test
    public void create() {
        Entity result = bcpService.create(FQN, Collections.emptyMap());

        // проверяем правильность заполнение как атрибутов определенных в Типе, так и в Классе
        Assertions.assertEquals(INITIAL_STATUS, result.getAttribute(HasWorkflow.STATUS), "Должны получить " +
                "initial-статус");
    }

    @Test
    public void version_create() {
        Entity e = bcpService.create(FQN, Collections.emptyMap());

        List<Entity> versions = dbService.listOfVersions(e);
        Assertions.assertEquals(1,
                versions.size(), "Т.к. статус объекта версионированный атрибут, то должны создать версию объекта");
        Entity version = versions.get(0);
        Assertions.assertEquals(INITIAL_STATUS,
                version.getAttribute(HasWorkflow.STATUS), "Должны получить статус в котором был создан объект");
    }

    @Test
    public void version_edit() {
        Entity e = bcpService.create(FQN, Collections.emptyMap());
        bcpService.edit(e, ImmutableMap.of(HasWorkflow.STATUS, STATUS_1));

        List<Entity> versions = dbService.listOfVersions(e);
        Assertions.assertEquals(
                2, versions.size(), "Т.к. статус объекта версионированный атрибут, то должны создать версию объекта " +
                        "при " +
                        "создании объекта и при его изменении");
        Entity createVersion = versions.get(0);
        Assertions.assertEquals(INITIAL_STATUS,
                createVersion.getAttribute(HasWorkflow.STATUS), "Должны получить статус в котором был создан объект");
        Entity editVersion = versions.get(1);
        Assertions.assertEquals(STATUS_1,
                editVersion.getAttribute(HasWorkflow.STATUS),
                "Должны получить статус в котором был установлен во время изменения объекта");
    }

    @Test
    public void unconfirmedTransition() {
        Entity e = bcpService.create(FQN, Collections.emptyMap());

        // Должны получиьт ошибку т.к. не настроен переход из begin в st2
        Assertions.assertThrows(ValidationException.class, () -> bcpService.edit(e,
                ImmutableMap.of(HasWorkflow.STATUS, STATUS_2)));
    }

    @Test
    public void backTransition() {
        Entity e = bcpService.create(FQN, Collections.emptyMap());
        bcpService.edit(e.getGid(), ImmutableMap.of(HasWorkflow.STATUS, STATUS_1));

        // Должны получиьт ошибку т.к. есть переход из begin в st1, но нет перехода из st1 в begin
        Assertions.assertThrows(ValidationException.class, () -> bcpService.edit(e.getGid(),
                ImmutableMap.of(HasWorkflow.STATUS, INITIAL_STATUS)));
    }

    @Test
    public void preOkCondition() {
        Entity e = bcpService.create(FQN, Collections.emptyMap());

        Entity result = bcpService.edit(e, ImmutableMap.of(HasWorkflow.STATUS, STATUS_3));

        // проверяем правильность заполнение как атрибутов определенных в Типе, так и в Классе
        Assertions.assertEquals(STATUS_3, result.getAttribute(HasWorkflow.STATUS), "Должны получить initial-статус");
    }

    @Test
    public void preErrorCondition() {
        Entity e = bcpService.create(FQN, Collections.emptyMap());

        // Должны получить ошибку т.к. preCondition, сконфигурированный для статуса st4, возвращает ошибку
        Assertions.assertThrows(ValidationException.class, () -> bcpService.edit(e,
                ImmutableMap.of(HasWorkflow.STATUS, STATUS_4)));
    }

    @Test
    public void postErrorCondition() {
        Entity e = bcpService.create(FQN, Collections.emptyMap());
        bcpService.edit(e, ImmutableMap.of(HasWorkflow.STATUS, STATUS_5));

        // Должны получить ошибку т.к. preCondition, сконфигурированный для статуса st4, возвращает ошибку
        Assertions.assertThrows(ValidationException.class, () -> bcpService.edit(e,
                ImmutableMap.of(HasWorkflow.STATUS, STATUS_1)));
    }

    @Test
    public void checkInheritFalse_withTransition() {
        Entity e = bcpService.create(Fqn.of("rootMC$child"), Maps.of());
        bcpService.edit(e, Maps.of("status", "s1"));
    }

    @Test
    public void checkInheritFalse_withoutTransition() {
        Entity e = bcpService.create(Fqn.of("rootMC$child"), Maps.of());
        // должны получиьт ошибку т.к. у rootMC$child нет перехода s0->s2
        Assertions.assertThrows(ValidationException.class, () -> bcpService.edit(e, Maps.of("status", "s2")));
    }
}
