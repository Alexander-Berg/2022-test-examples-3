package ru.yandex.market.abo.tms;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 *         @date 23.06.2008
 */
public class SimpleDbExecutorTest extends EmptyTest {

    @Autowired
    private SimpleDbExecutor simpleDbExecutor;

    @Test
    public void loadGroups() {
        List<SimpleDbExecutor.QueryGroup> groups = simpleDbExecutor.loadGroups();
        assertFalse(groups.isEmpty());

        groups.forEach(group -> {
            List<String> sql = simpleDbExecutor.loadQueriesForGroup(group);
            assertFalse(sql.isEmpty());
        });
    }
}
