package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.UpdateProductJob;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;

import java.util.List;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class UpdateProductJobRepositoryTest extends RepositoryTest {

    @Autowired
    private UpdateProductJobRepository updateProductJobRepository;

    @Test
    @DatabaseSetup(value = "classpath:repository/update_product_job_setup.xml")
    @ExpectedDatabase(value = "classpath:repository/execute_update_products_jobs_result.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void fetchNextJobs() {
        List<UpdateProductJob> jobs = transactionTemplate.execute(t ->
            updateProductJobRepository.fetchNextJobs()
        );

        softly.assertThat(jobs)
            .as("List of jobs matching the condition")
            .hasSize(3);

        softly.assertThat(jobs.get(0).getId())
            .as("The first found correct update product job")
            .isEqualTo(5);
        softly.assertThat(jobs.get(1).getId())
            .as("The second found correct update product job")
            .isEqualTo(6);
        softly.assertThat(jobs.get(2).getId())
            .as("The second found correct update product job")
            .isEqualTo(3);

        jobs.forEach(job -> {
            job.incrementAttemptsCount();
            job.setSuccess(true);
            updateProductJobRepository.save(job);
        });
    }
}
