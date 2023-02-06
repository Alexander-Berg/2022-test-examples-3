package ru.yandex.market.jmf.search;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.ReindexService;
import ru.yandex.market.jmf.db.hibernate.impl.adapter.EntityAdapterServiceImpl;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ActiveProfiles("singleTx")
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InternalModuleSearchTestConfiguration.class)
public class ReindexFtsTest {
    private static final Fqn FQN = Fqn.of("testSearch");
    private static final Fqn FQN_CHILD = Fqn.of("pts$child");
    private static final Fqn FQN_PTS = Fqn.of("pts");

    @Inject
    private SearchService searchService;
    @Inject
    private ReindexService reindexService;
    @Inject
    private BcpService bcpService;
    @Inject
    private EntityAdapterServiceImpl entityAdapterService;

    @Test
    public void checkReindexIsWork() {
        String title = "someTitle";
        Entity testEntity = bcpService.create(FQN, Maps.of("title", title));

        var persistentEntity = entityAdapterService.unwrap(testEntity);
        if (persistentEntity instanceof HasSystemFtsBody) {
            ((HasSystemFtsBody) persistentEntity).setSystemFtsBody("222222222");
        } else {
            fail("Почему-то метакласс не содержит ftsBody");
        }

        //Т.к. затерли ftsBody то не должны найти сущность
        List<Metaclass> result = searchService.search(title);
        assertEquals(0, result.size());

        // Переиндексируем, т.е. ftsBody восстановится
        reindexService.reindex(FQN, "ftsSearch");
        reindexService.doReindex();

        //Снова ищем, должны найти сущность
        result = searchService.search(title);
        assertEquals(1, result.size());
    }

    @Test
    public void checkReindexIsWorkWithChild() {
        String title = "someTitle";
        Entity testEntity = bcpService.create(FQN_CHILD, Maps.of("title", title, "testCode", "testCode111"));

        var persistentEntity = entityAdapterService.unwrap(testEntity);
        if (persistentEntity instanceof HasSystemFtsBody) {
            ((HasSystemFtsBody) persistentEntity).setSystemFtsBody("222222222");
        } else {
            fail("Почему-то метакласс не содержит ftsBody");
        }

        //Т.к. затерли ftsBody то не должны найти сущность
        List<Metaclass> result = searchService.search(title);
        assertEquals(0, result.size());

        // Переиндексируем, т.е. ftsBody восстановится
        reindexService.reindex(FQN_PTS, "ftsSearch");
        reindexService.doReindex();

        //Снова ищем, должны найти сущность
        result = searchService.search(title);
        var resultByCode = searchService.search("testCode111");
        assertEquals(1, result.size());
        assertEquals(1, resultByCode.size());
    }

}
