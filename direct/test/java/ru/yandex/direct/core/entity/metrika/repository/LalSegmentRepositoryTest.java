package ru.yandex.direct.core.entity.metrika.repository;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class LalSegmentRepositoryTest {
    @Autowired
    private LalSegmentRepository lalSegmentRepository;

    @Test
    public void createLalSegmentsByDuplicateParentIds() {
        List<Goal> lalSegments = lalSegmentRepository.createLalSegments(List.of(123L, 123L));
        assertThat(lalSegments).hasSize(1);
    }
}
