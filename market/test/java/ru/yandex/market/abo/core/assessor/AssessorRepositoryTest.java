package ru.yandex.market.abo.core.assessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.assessor.model.Assessor;
import ru.yandex.market.abo.core.assessor.service.AssessorRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author agavrikov
 * @date 07.04.18
 */
public class AssessorRepositoryTest extends EmptyTest {

    @Autowired
    AssessorRepository assessorRepository;

    @Test
    public void testRepo() {
        assessorRepository.saveAll(initAssessorList());

        List<Assessor> assessors = assessorRepository.findAllById(Arrays.asList(1L, 2L));
        List<Assessor> dbAssessors = Arrays.asList(
                new Assessor(1L, "login1"),
                new Assessor(2L, "login2")
        );
        assertEquals(assessors, dbAssessors);
    }

    private List<Assessor> initAssessorList() {
        List<Assessor> assessorList = new ArrayList<>();
        assessorList.add(new Assessor(1L, "login1"));
        assessorList.add(new Assessor(2L, "login2"));
        assessorList.add(new Assessor(3L, "login3"));
        return assessorList;
    }
}
