package ru.yandex.market.jmf.logic.def.test;


import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.NonUniqueObjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class HasStringIdTest {
    public static final Fqn TEST_FQN = Fqn.of("stringIdTest");
    @Inject
    private BcpService bcpService;
    @Inject
    private DbService dbService;

    private String id;

    @BeforeEach
    public void setUp() {
        id = RandomStringUtils.randomAlphanumeric(5);
    }

    @Test
    public void testThrowsExceptionWhenIdIsNotSet() {
        assertThrows(ValidationException.class, () ->
                bcpService.create(TEST_FQN, Map.of()));
    }

    @Test
    public void testThatIdCouldBeSetViaBcp() {
        var entity = bcpService.create(TEST_FQN, Map.of("id", id));

        assertEquals(TEST_FQN.gidOf(id), entity.getGid());
    }

    @Test
    public void testThatIdCouldNotBeChanged() {
        var entity = bcpService.create(TEST_FQN, Map.of("id", id));

        var anotherId = "anotherId";
        bcpService.edit(entity, Map.of("id", anotherId));

        var gid = TEST_FQN.gidOf(id);
        assertEquals(gid, dbService.get(gid).getGid());
        assertNull(dbService.get(TEST_FQN.gidOf(anotherId)));
    }

    @Test
    public void testThatItReallyUnique() {
        bcpService.create(TEST_FQN, Map.of("id", id));
        assertThrows(NonUniqueObjectException.class, () -> {
            bcpService.create(TEST_FQN, Map.of("id", id));
        });
    }

    @Test
    public void getByGid() {
        var entity = bcpService.create(TEST_FQN, Map.of("id", id));
        bcpService.create(TEST_FQN, Map.of("id", Randoms.string()));

        Map<String, Entity> result = dbService.get(TEST_FQN, List.of(TEST_FQN.gidOf(id),
                TEST_FQN.gidOf(Randoms.string())));

        assertEquals(1, result.size());
        assertTrue(result.containsKey(TEST_FQN.gidOf(id)));
        assertTrue(result.containsValue(entity));
    }

    @Test
    public void getByGidEmpty() {
        bcpService.create(TEST_FQN, Map.of("id", id));

        Map<String, Entity> result = dbService.get(TEST_FQN, List.of());

        assertEquals(0, result.size());
    }

    @Test
    public void filterByGidEmpty() {
        var entity = bcpService.create(TEST_FQN, Map.of("id", id));
        bcpService.create(TEST_FQN, Map.of("id", Randoms.string()));

        Query q = Query.of(TEST_FQN).withFilters(Filters.eq(HasGid.GID, TEST_FQN.gidOf(id)));
        List<Entity> result = dbService.list(q);

        assertEquals(1, result.size());
        assertTrue(result.contains(entity));
    }
}
