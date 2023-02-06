package ru.yandex.direct.core.entity.moderationdiag.service;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.ModerationDiagSteps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.DIAG_ID1;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.DIAG_ID2;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.DIAG_ID3;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.createModerationDiag1;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.createModerationDiag2;
import static ru.yandex.direct.core.testing.data.TestModerationDiag.createModerationDiagPerformance;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationDiagServiceTest {

    @Autowired
    private ModerationDiagService moderationDiagService;

    @Autowired
    private ModerationDiagSteps moderationDiagSteps;

    @Before
    public void before() throws Exception {
        moderationDiagSteps.insertStandartDiags();
        moderationDiagService.invalidateAll();
    }

    @After
    public void clean() throws Exception {
        moderationDiagSteps.cleanup();
    }


    @Test
    public void get_Common_CheckReturnValue() {
        Map<Long, ModerationDiag> result = moderationDiagService.get(ModerationDiagType.COMMON);
        assertThat(result, allOf(
                hasEntry(equalTo(DIAG_ID1),
                        beanDiffer(createModerationDiag1().withDiagText("full text1"))),
                hasEntry(equalTo(DIAG_ID2),
                        beanDiffer(createModerationDiag2().withDiagText("short text2"))),
                not(hasEntry(equalTo(DIAG_ID3),
                        beanDiffer(createModerationDiagPerformance()
                                .withDiagText("full text3"))))
        ));
    }

    @Test
    public void get_Performance_CheckReturnValue() {
        Map<Long, ModerationDiag> result = moderationDiagService.get(ModerationDiagType.PERFORMANCE);
        assertThat(result, allOf(
                hasEntry(equalTo(DIAG_ID3),
                        beanDiffer(createModerationDiagPerformance()
                                .withDiagText("full text3"))),
                not(hasEntry(equalTo(DIAG_ID1),
                        beanDiffer(createModerationDiag1().withDiagText("full text1"))))
        ));
    }
}
