package ru.yandex.market.abo.core.ticket;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.problem.model.ProblemType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author imelnikov
 * @date 01.06.17
 */
public class ProblemTypeServiceTest extends EmptyTest {

    @Autowired
    private ProblemTypeService problemTypeService;

    @Test
    public void testProblemTypeService() {
        final ProblemType problemType = problemTypeService.getProblemType(1);
        assertNotNull(problemType);

        // неправильный регион доставки
        ProblemType problemType1 = problemTypeService.getProblemType(43);
        assertTrue(problemType1.isSendingMessage());

        ProblemType problemType2 = problemTypeService.getProblemType(33);
        assertFalse(problemType2.isSendingMessage());
    }

    /**
     * Для всех строчек в таблице core_problem_class есть значение в енуме ProblemClass.
     */
    @Test
    public void eachClass() {
        if (problemTypeService.getAllProblemTypes().stream()
                .map(ProblemType::getProblemClass)
                .anyMatch(Objects::isNull)) {

            fail("Add enum ProblemClass for each row in db");
        }

    }
}
