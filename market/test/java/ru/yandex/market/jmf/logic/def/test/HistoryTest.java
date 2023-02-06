package ru.yandex.market.jmf.logic.def.test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.def.EntityHistory;
import ru.yandex.market.jmf.metadata.Fqn;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class HistoryTest {

    private static final Fqn FQN = Fqn.parse("historySimple");
    private static final String TITLE = "title";

    @Inject
    BcpService bcpService;
    @Inject
    DbService dbService;

    @Test
    public void entityHistory_create() {
        // вызов системы
        Entity entity = create();

        // Проверка утверждений
        Query q = Query.of(EntityHistory.FQN)
                .withFilters(Filters.eq(EntityHistory.ENTITY, entity));
        List<Entity> list = dbService.list(q);

        Assertions.assertFalse(list.isEmpty(), "Должно быть событие создание объекта");
        Entity event = list.get(0);
        String process = event.getAttribute(EntityHistory.PROCESS);
        Assertions.assertEquals("create", process, "Событие должно быть в процессе создания");
        String description = event.getAttribute(EntityHistory.DESCRIPTION);
        Pattern pattern = Pattern.compile("""
                Объект создан:
                  Время создания: .*;
                  Название: .*\\.
                """);
        Assertions.assertTrue(pattern.matcher(description).matches(), "description ожидается: " + pattern.pattern());
    }

    @Test
    public void entityHistory_edit() {
        Entity entity = create();
        // вызов системы
        bcpService.edit(entity, ImmutableMap.of(TITLE, Randoms.string()));

        // Проверка утверждений
        Query q = Query.of(EntityHistory.FQN)
                .withFilters(Filters.eq(EntityHistory.ENTITY, entity),
                        Filters.eq(EntityHistory.PROCESS, "edit"));
        List<Entity> list = dbService.list(q);

        Assertions.assertFalse(list.isEmpty(), "Должно быть событие изменения объекта");
        Entity event = list.get(0);
        String process = event.getAttribute(EntityHistory.PROCESS);
        Assertions.assertEquals("edit", process);
    }

    @Test
    public void entityHistory_delete() {
        Entity entity = create();
        // вызов системы
        bcpService.delete(entity);

        // Проверка утверждений
        Query q = Query.of(EntityHistory.FQN)
                .withFilters(Filters.eq(EntityHistory.ENTITY, entity),
                        Filters.eq(EntityHistory.PROCESS, "delete"));
        List<Entity> list = dbService.list(q);

        Assertions.assertFalse(list.isEmpty(), "Должно быть событие удаления объекта");
        Entity event = list.get(0);
        String process = event.getAttribute(EntityHistory.PROCESS);
        Assertions.assertEquals("delete", process);
    }


    private Entity create(String attr0Value) {
        Map<String, Object> properties = properties(attr0Value);
        return bcpService.create(FQN, properties);
    }

    private Entity create() {
        return create(UUID.randomUUID().toString());
    }

    private Map<String, Object> properties(String attr0Value) {
        return ImmutableMap.of(TITLE, attr0Value);
    }
}
