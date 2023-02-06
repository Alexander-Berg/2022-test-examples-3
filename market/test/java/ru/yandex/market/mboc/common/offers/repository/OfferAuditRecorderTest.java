package ru.yandex.market.mboc.common.offers.repository;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.infrastructure.job.JobContext;
import ru.yandex.market.mboc.common.infrastructure.job.JobContextAccessor;
import ru.yandex.market.mboc.common.offers.model.Offer;

public class OfferAuditRecorderTest {
    private final ExecutorService jobExecutor = Executors.newSingleThreadExecutor();

    @Test
    public void testUsesJobNameInAuthor() throws Exception {
        testUsesJobNameInAuthorCase(null, null, null);
        testUsesJobNameInAuthorCase("test", null, "test");
        testUsesJobNameInAuthorCase(null, "job", "job/");
        testUsesJobNameInAuthorCase("test", "job", "job/test");
    }

    private void testUsesJobNameInAuthorCase(String modifiedBy, String jobName, String expected) throws Exception {
        Optional<String> result = jobExecutor.submit(() -> {
            JobContextAccessor.setCurrent(jobName != null ? new JobContext(jobName) : null);
            Optional<String> author = OfferAuditRecorder.buildAuthor(
                new OfferRepositoryImpl.OfferAuditInformation(new Offer().setTransientModifiedBy(modifiedBy))
            );
            JobContextAccessor.clear();
            return author;
        }).get(2, TimeUnit.SECONDS);

        if (expected == null) {
            Assertions.assertThat(result).isEmpty();
        } else {
            Assertions.assertThat(result).isPresent().contains(expected);
        }
    }
}
