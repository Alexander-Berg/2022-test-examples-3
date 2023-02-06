package ru.yandex.direct.core.testing.steps;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.entity.moderationdiag.repository.ModerationDiagRepository;
import ru.yandex.direct.core.testing.data.TestModerationDiag;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static ru.yandex.direct.dbschema.ppcdict.tables.ModerateDiags.MODERATE_DIAGS;

public class ModerationDiagSteps {
    private final ModerationDiagRepository moderationDiagRepository;

    private final DslContextProvider dslContextProvider;

    @Autowired
    public ModerationDiagSteps(ModerationDiagRepository moderationDiagRepository,
                               DslContextProvider dslContextProvider) {
        this.moderationDiagRepository = moderationDiagRepository;
        this.dslContextProvider = dslContextProvider;
    }


    synchronized public void insertStandartDiags() {
        dslContextProvider.ppcdict().truncate(MODERATE_DIAGS).execute();
        moderationDiagRepository.insertModerationDiagOrUpdateTexts(asList(
                TestModerationDiag.createModerationDiag1(),
                TestModerationDiag.createModerationDiag2(),
                TestModerationDiag.createModerationDiagPerformance()
        ));
    }

    public List<ModerationDiag> getModerationDiags(Collection<Long> moderationDiagIds) {
        return moderationDiagRepository.fetch(moderationDiagIds);
    }

    synchronized public void cleanup() {
        dslContextProvider.ppcdict().truncate(MODERATE_DIAGS).execute();
    }
}
