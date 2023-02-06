package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.StockPushesJob;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;

import java.time.LocalDateTime;
import java.util.Optional;

class StockPushesJobRepositoryTest extends RepositoryTest {

    @Autowired
    private StockPushesJobRepository repository;

    @Test
    @DatabaseSetup(value = "classpath:repository/stock_pushes_job_setup.xml")
    void fetchNextJob() {
        Optional<StockPushesJob> fetched = repository.fetchNextJob();

        softly.assertThat(fetched)
            .as("Asserting that the fetched record is present")
            .isPresent();

        StockPushesJob job = fetched.get();

        softly.assertThat(job.getId())
            .as("Asserting the job id")
            .isEqualTo(2);
        softly.assertThat(job.getFromDt())
            .as("Asserting the job from_dt")
            .isEqualTo(LocalDateTime.parse("2018-03-05T18:55:56.430000"));
        softly.assertThat(job.getToDt())
            .as("Asserting the job to_dt")
            .isEqualTo(LocalDateTime.parse("2018-04-04T18:55:56.430000"));
        softly.assertThat(job.getTriesCount())
            .as("Asserting the job tries count")
            .isEqualTo((short) 1);
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/empty_stock_pushes_job_setup.xml")
    void fetchNextJobEmpty() {
        Optional<StockPushesJob> fetched = repository.fetchNextJob();

        softly.assertThat(fetched)
            .as("Asserting that the fetched record is not present")
            .isEmpty();
    }
}
