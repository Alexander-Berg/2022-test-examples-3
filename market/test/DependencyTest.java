package ru.yandex.market.jmf.logic.def.test;

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
import ru.yandex.market.jmf.metadata.Fqn;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class DependencyTest {

    @Inject
    BcpService bcpService;
    @Inject
    DbService dbService;

    @Test
    public void delete_action_delete() {
        // настройка системы
        Entity s = create();
        Entity r = create("dependencyLinked1", s);

        // вызов системы
        bcpService.delete(s);

        // проверка утверждений
        flush();

        Entity entity = dbService.get(r.getGid());
        Assertions.assertNull(entity, "Объект должен быть удален по зависимостям");
    }

    @Test
    public void delete_action_forbid() {
        Assertions.assertThrows(ValidationException.class, () -> {
            // настройка системы
            Entity s = create();
            create("dependencyLinked2", s);

            // вызов системы
            bcpService.delete(s);
        });
    }

    private void flush() {
        dbService.flush();
        dbService.clear();
    }

    private Entity create() {
        return bcpService.create(Fqn.parse("dependencySimple"), ImmutableMap.of("title", Randoms.string()));
    }

    private Entity create(String fqn, Entity link) {
        return bcpService.create(Fqn.parse(fqn), ImmutableMap.of("link", link));
    }

}
