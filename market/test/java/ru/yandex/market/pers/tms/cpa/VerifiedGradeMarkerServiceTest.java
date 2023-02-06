package ru.yandex.market.pers.tms.cpa;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.service.GradeQueueService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.yt.YtClient;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class VerifiedGradeMarkerServiceTest extends MockedPersTmsTest {

    private static final Long DEFAULT_MODEL_ID = 123L;
    private static final Long DEFAULT_AUTHOR_ID = 12345L;

    @Autowired
    private GradeCreator gradeCreator;
    @Autowired
    private GradeQueueService gradeQueueService;
    @Autowired
    YtClient ytClient;
    @Autowired
    @Qualifier("ytJdbcTemplate")
    JdbcTemplate hahnJdbcTemplate;

    @Autowired
    private VerifiedGradeMarkerService verifiedGradeMarkerService;

    @Test
    public void testCreateNotVerifiedModelGrade() throws Exception {
        long gradeId = gradeCreator.createModelGrade(DEFAULT_MODEL_ID, DEFAULT_AUTHOR_ID);

        verifiedGradeMarkerService.mark();

        Integer verified = pgJdbcTemplate
            .queryForObject("SELECT verified FROM grade WHERE id = ?", Integer.class, gradeId);
        Assert.assertNull(verified);
    }

    @Test
    public void testCreateVerifiedModelGrade() throws Exception {
        long gradeId = gradeCreator.createModelGrade(DEFAULT_MODEL_ID, DEFAULT_AUTHOR_ID);

        mockYtToReturnMarkNeededGrade(Collections.singletonList(gradeId));
        Assert.assertTrue(gradeQueueService.isEmpty());

        verifiedGradeMarkerService.mark();

        Integer verified = pgJdbcTemplate
            .queryForObject("SELECT verified FROM grade WHERE id = ?", Integer.class, gradeId);
        assertEqualsOne(verified);
        checkGradeQueue(gradeId);
    }

    private void checkGradeQueue(long gradeId) {
        assertTrue(gradeQueueService.isInQueue(gradeId));
    }

    private void assertEqualsOne(Integer verified) {
        Assert.assertEquals(Integer.valueOf(1), verified);
    }

    private void mockYtToReturnMarkNeededGrade(List<Long> answer) {
        when(hahnJdbcTemplate.queryForList(anyString(), eq(Long.class))).thenReturn(answer);
    }

}
