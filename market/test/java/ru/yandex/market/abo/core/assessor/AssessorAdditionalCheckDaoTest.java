package ru.yandex.market.abo.core.assessor;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.assessor.quest.AssessorAdditionalCheck;
import ru.yandex.market.abo.core.assessor.quest.AssessorAdditionalCheckRepo;
import ru.yandex.market.abo.core.assessor.quest.QuestCheckMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by antipov93@yndx-team.ru.
 */
public class AssessorAdditionalCheckDaoTest extends EmptyTest {

    @Autowired
    private AssessorAdditionalCheckRepo assessorAdditionalCheckRepo;

    @Test
    void test() {
        assessorAdditionalCheckRepo.saveAll(
                Arrays.asList(
                        new AssessorAdditionalCheck(100, 101, new Date(), 1, QuestCheckMethod.DEFAULT, "test"),
                        new AssessorAdditionalCheck(110, 111, new Date(), 2, QuestCheckMethod.CLONE, "blah")
                )
        );
        List<AssessorAdditionalCheck> loaded = assessorAdditionalCheckRepo.findAll();
        assertEquals(2, loaded.size());
        AssessorAdditionalCheck a = loaded.get(0);
        assertNotNull(assessorAdditionalCheckRepo.findByIdOrNull(a.getId()));
        assessorAdditionalCheckRepo.deleteById(a.getId());
        assertEquals(1, assessorAdditionalCheckRepo.findAll().size());
    }
}
