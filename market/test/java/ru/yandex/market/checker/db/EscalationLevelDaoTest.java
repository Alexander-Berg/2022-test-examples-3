package ru.yandex.market.checker.db;

import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checker.FunctionalTest;
import ru.yandex.market.checker.api.model.EscalationLevel;
import ru.yandex.market.checker.api.model.EscalationRequestBody;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ParametersAreNonnullByDefault
public class EscalationLevelDaoTest extends FunctionalTest {

    @Autowired
    private EscalationLevelDao escalationLevelDao;

    @Test
    @DisplayName("Создать и обновить уровень эскалации")
    void test_createAndUpdateEscalationLevel() {
        EscalationLevel inserted = escalationLevelDao.createEscalationLevel(
                new EscalationRequestBody().name("name1"));

        assertEquals(inserted, new EscalationLevel().id(1L).name("name1"));

        Optional<EscalationLevel> updated = escalationLevelDao.updateEscalationLevel(
                1L,
                new EscalationRequestBody().name("name2"));

        assertEquals(updated.orElse(null), new EscalationLevel().id(1L).name("name2"));
    }

    @Test
    @DisplayName("Выборка уровней эскалации")
    @DbUnitDataSet(before = "selectEscalationLevel.csv")
    void test_selectEscalationLevel() {
        escalationLevelDao.deleteEscalationLevel(2L);
        List<EscalationLevel> levelsAsc = escalationLevelDao.getEscalationLevels(0, 50, "asc");

        assertEquals(levelsAsc.size(), 2);
        assertEquals(levelsAsc.get(0).getName(), "CRIT");
        assertEquals(levelsAsc.get(1).getName(), "INFO");

        Optional<EscalationLevel> level = escalationLevelDao.getById(1L);

        assertEquals(level.orElse(null), new EscalationLevel().id(1L).name("CRIT"));
    }
}
