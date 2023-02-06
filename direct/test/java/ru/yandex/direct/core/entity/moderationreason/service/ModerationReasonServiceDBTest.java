package ru.yandex.direct.core.entity.moderationreason.service;


import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestModerationDiag;
import ru.yandex.direct.core.testing.steps.ModerationDiagSteps;
import ru.yandex.direct.core.testing.steps.ModerationReasonSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.BANNER;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationReasonServiceDBTest {
    private static final List<ModerationDiag> MODERATION_DIAG_LIST = asList(
            TestModerationDiag.createModerationDiag1().withDiagText("full text1"),
            TestModerationDiag.createModerationDiag2().withDiagText("short text2"));
    private long objectId1;
    private long objectId2;
    private List<Long> objectIds;

    @Autowired
    public ModerationReasonService moderationReasonService;
    @Autowired
    private ModerationReasonSteps moderationReasonSteps;
    @Autowired
    private ModerationDiagSteps moderationDiagSteps;
    @Autowired
    private ModerationDiagService moderationDiagService;

    @Before
    public void setUp() throws Exception {
        moderationDiagService.invalidateAll();
        moderationDiagSteps.insertStandartDiags();
        moderationReasonSteps.clean();
        moderationReasonSteps.insertStandartReasons();
        objectId1 = moderationReasonSteps.insertRejectedBannerAndReason();
        objectId2 = moderationReasonSteps.insertRejectedBannerAndReason();
        objectIds = asList(objectId1, objectId2);
    }

    @After
    public void cleanup() {
        moderationDiagSteps.cleanup();
    }

    @Test
    public void getReasons_ReturnsCorrectResult() {
        Map<Long, Map<ModerationReasonObjectType, List<ModerationDiag>>> reasons =
                moderationReasonService.getReasons(objectIds);

        assertThat(reasons, equalTo(ImmutableMap.of(
                objectId1, ImmutableMap.of(BANNER, MODERATION_DIAG_LIST),
                objectId2, ImmutableMap.of(BANNER, MODERATION_DIAG_LIST)
        )));
    }

    @Test
    public void getCalloutRejectedReasons_ReturnCorrect() {
        Map<Long, List<ModerationDiag>> reasons =
                moderationReasonService.getCalloutRejectedReasons(objectId1, asList(8L, 9L, 10L));

        assertThat(reasons, equalTo(ImmutableMap.of(
                8L, MODERATION_DIAG_LIST,
                9L, MODERATION_DIAG_LIST,
                10L, MODERATION_DIAG_LIST)

        ));
    }
}
