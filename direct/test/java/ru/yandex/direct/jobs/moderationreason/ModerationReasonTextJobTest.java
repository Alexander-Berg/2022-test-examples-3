package ru.yandex.direct.jobs.moderationreason;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagType;
import ru.yandex.direct.core.entity.moderationdiag.repository.ModerationDiagRepository;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

// если вдруг захотелось запустить, нужно закомментировать @Disabled
@Disabled("Для запуска вручную")
@JobsTest
@ExtendWith(SpringExtension.class)
class ModerationReasonTextJobTest {

    private static final Logger logger = LoggerFactory.getLogger(ModerationReasonTextJobTest.class);

    // may be changes in future
    private static final int DOCUMENTS_IN_SITEMAP = 29;

    @Autowired
    private ModerationReasonTextJob moderationReasonTextJob;

    @Autowired
    private ModerationDiagRepository moderationDiagRepository;

    @Test
    void getSiteMap() throws InterruptedException, ExecutionException, TimeoutException {
        String sitemap = moderationReasonTextJob.getSiteMap("ru");
        Set<String> ids = moderationReasonTextJob.siteMapToIds(sitemap);
        for (String id : ids) {
            logger.info(id);
        }
        assertEquals(DOCUMENTS_IN_SITEMAP, ids.size());
    }

    @Test
    void execute() {
        List<ModerationDiag> diags = moderationDiagRepository.fetch(ModerationDiagType.COMMON);
        assertEquals(0, diags.size());
        moderationReasonTextJob.execute();
        diags = moderationDiagRepository.fetch(ModerationDiagType.COMMON);
        assertEquals(DOCUMENTS_IN_SITEMAP, diags.size());
    }

    @Test
    void executeTwice() {
        List<ModerationDiag> diags = moderationDiagRepository.fetch(ModerationDiagType.COMMON);
        assertEquals(0, diags.size());
        moderationReasonTextJob.execute();
        diags = moderationDiagRepository.fetch(ModerationDiagType.COMMON);
        assertEquals(DOCUMENTS_IN_SITEMAP, diags.size());

        moderationReasonTextJob.execute();
        diags = moderationDiagRepository.fetch(ModerationDiagType.COMMON);
        assertEquals(DOCUMENTS_IN_SITEMAP, diags.size());
    }
}
