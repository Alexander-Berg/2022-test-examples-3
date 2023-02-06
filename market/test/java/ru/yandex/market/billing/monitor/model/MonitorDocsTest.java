package ru.yandex.market.billing.monitor.model;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.monitor.JugglerAggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@ActiveProfiles("goe-processing")
class MonitorDocsTest extends FunctionalTest {
    private static final Set<String> KNOWN_JOBS_WITHOUT_DOCS = Set.of(
            "clidYtExportExecutor",
            "stocksBySupplyReportDailyExportExecutor"
    );

    private static final Set<String> KNOWN_DOCS_WITHOUT_JOBS = Set.of(
            "template"
    );
    @Autowired
    private ApplicationContext applicationContext;

    private Set<String> getDocumentedJobs() throws IOException {
        Set<String> jobs = new HashSet<>();
        PathMatchingResourcePatternResolver res = new PathMatchingResourcePatternResolver();
        Resource[] resources = res.getResources("classpath:list/*.md");
        for (Resource r : resources) {
            if (!r.isFile()) {
                continue;
            }
            File file = r.getFile();
            String name = file.getName();
            jobs.add(name.replace(".md", ""));
        }
        jobs.remove("template.md");
        return jobs;
    }

    @Test
    void jobHasOperationDoc() throws IOException {
        Set<String> documentedJobs = getDocumentedJobs();
        Map<String, Object> jobs = applicationContext.getBeansWithAnnotation(JugglerAggregate.class);
        Set<String> undocumentedJobs = Sets.difference(jobs.keySet(), documentedJobs);
        Set<String> jobsToReport = Sets.difference(undocumentedJobs, KNOWN_JOBS_WITHOUT_DOCS);
        assertThat("Some descriptions don't exists.\n"
                + "See also https://docs.yandex-team.ru/market-billing/tests/jobHasOperationDoc\n"
                + "Failed jobs: " + jobsToReport, jobsToReport, hasSize(0));
    }

    @Test
    void operationDocsHasJobs() throws IOException {
        Set<String> documentedJobs = getDocumentedJobs();
        Map<String, Object> jobs = applicationContext.getBeansWithAnnotation(JugglerAggregate.class);
        Set<String> undocumentedJobs = Sets.difference(documentedJobs, jobs.keySet());
        Set<String> jobsToReport = Sets.difference(undocumentedJobs, KNOWN_DOCS_WITHOUT_JOBS);
        assertThat("Some descriptions exists, but jobs don't.\n"
                + "See also https://docs.yandex-team.ru/market-billing/tests/jobHasOperationDoc\n"
                + "Failed jobs: " + jobsToReport, jobsToReport, hasSize(0));
    }

}
