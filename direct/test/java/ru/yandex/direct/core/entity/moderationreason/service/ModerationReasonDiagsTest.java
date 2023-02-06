package ru.yandex.direct.core.entity.moderationreason.service;

import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestModerationDiag;
import ru.yandex.direct.core.testing.steps.ModerationDiagSteps;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.createModerationDiagPerformance;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationReasonDiagsTest {

    @Autowired
    public ModerationReasonService moderationReasonService;

    @Autowired
    private ModerationDiagSteps moderationDiagSteps;

    @Before
    public void setUp() throws Exception {
        moderationDiagSteps.insertStandartDiags();
    }

    @After
    public void clean() throws Exception {
        moderationDiagSteps.cleanup();
    }

    @Test
    public void getModerationDiagsByIds_singleObject_CheckReturnValue() {
        List<ModerationDiag> result = moderationReasonService.getModerationDiagsByIds(
                Collections.singletonList(TestModerationDiag.DIAG_ID3));
        assertThat(result, contains(beanDiffer(createModerationDiagPerformance()
                .withDiagText("full text3"))));
    }

    @Test
    public void getModerationDiagsByIds_wrongValues_CheckReturnValue() {
        List<ModerationDiag> result = moderationReasonService.getModerationDiagsByIds(
                asList(TestModerationDiag.DIAG_ID3, 100000L));
        assertThat(result, contains(beanDiffer(createModerationDiagPerformance()
                .withDiagText("full text3"))));
    }

    @Test
    public void getModerationDiagsByIds_OnlyNotPerfomanceValues_ReturnEmptyList() {
        List<ModerationDiag> result = moderationReasonService.getModerationDiagsByIds(
                asList(TestModerationDiag.DIAG_ID1, TestModerationDiag.DIAG_ID2));
        assertThat(result, equalTo(emptyList()));
    }
}
