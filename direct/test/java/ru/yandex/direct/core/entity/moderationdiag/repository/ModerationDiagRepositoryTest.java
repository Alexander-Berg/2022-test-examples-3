package ru.yandex.direct.core.entity.moderationdiag.repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestModerationDiag;
import ru.yandex.direct.core.testing.steps.ModerationDiagSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationDiagRepositoryTest {

    @Autowired
    private ModerationDiagRepository moderationDiagRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ModerationDiagSteps moderationDiagSteps;

    @Before
    public void before() throws Exception {
        moderationDiagSteps.insertStandartDiags();
    }

    @After
    public void after() {
        moderationDiagSteps.cleanup();
    }

    @Test
    public void insertModerationDiagOrUpdateTexts() {
        List<ModerationDiag> moderationDiags = moderationDiagRepository.fetch(ModerationDiagType.COMMON);
        assertThat(moderationDiags, hasSize(2));
        ModerationDiag firstDiag = getFirstDiag(moderationDiags);
        assertEquals("full text1", firstDiag.getFullText());
        assertEquals("short text1", firstDiag.getShortText());

        ModerationDiag updatedDiag = TestModerationDiag.createModerationDiag1()
                .withFullText("Full new text")
                .withShortText("short new text")
                .withStrongReason(false);

        moderationDiagRepository.insertModerationDiagOrUpdateTexts(Collections.singleton(updatedDiag));
        moderationDiags = moderationDiagRepository.fetch(ModerationDiagType.COMMON);
        assertThat(moderationDiags, hasSize(2));
        firstDiag = getFirstDiag(moderationDiags);
        assertEquals("Full text updated", "Full new text", firstDiag.getFullText());
        assertEquals("Short text updated", "short new text", firstDiag.getShortText());
        assertEquals("Other fields not changed", true, firstDiag.getAllowFirstAid());
    }

    private static ModerationDiag getFirstDiag(List<ModerationDiag> moderationDiags) {
        return moderationDiags.stream().filter(m -> m.getId() == TestModerationDiag.DIAG_ID1)
                .collect(Collectors.toList()).get(0);
    }
}
