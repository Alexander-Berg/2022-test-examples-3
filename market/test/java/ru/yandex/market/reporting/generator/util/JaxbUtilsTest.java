package ru.yandex.market.reporting.generator.util;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ru.yandex.market.reporting.common.domain.JobParameters;
import ru.yandex.market.reporting.common.domain.tree.Region;
import ru.yandex.market.reporting.common.util.JaxbUtils;
import ru.yandex.market.reporting.generator.domain.AuditReportParameters;
import ru.yandex.market.reporting.generator.domain.MarketReportParameters;

import java.util.ArrayList;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
public class JaxbUtilsTest {
    @Test
    public void marshall() throws Exception {
        Region region = new Region(1L, "юникод", 0L, new ArrayList<>());

        assertThat(JaxbUtils.unmarshall(JaxbUtils.marshall(region), Region.class), is(region));
    }

    @Test
    public void marshallJobParameters() throws Exception {
        MarketReportParameters marketReportParameters = new MarketReportParameters();
        marketReportParameters.setShop("MTC");
        marketReportParameters.setDomain("mts.ru");
        marketReportParameters.setRegions(ImmutableList.of(225L, 10174L, 187L, 1L));
        marketReportParameters.setCategories(ImmutableList.of(91491L, 90490L, 91042L, 10498025L));

        JobParameters<MarketReportParameters> jobParameters = new JobParameters<>(marketReportParameters);

        assertThat(JaxbUtils.unmarshall(JaxbUtils.marshall(jobParameters), JobParameters.class), is(jobParameters));

    }

    @Test
    public void marshallWithNewLines() throws Exception {
        MarketReportParameters marketReportParameters = new MarketReportParameters();
        marketReportParameters.setDomain("mts.\nru");
        JobParameters<MarketReportParameters> jobParameters = new JobParameters<>(marketReportParameters);
        assertThat(JaxbUtils.unmarshall(JaxbUtils.marshall(jobParameters), JobParameters.class), is(jobParameters));
    }

    /**
     * Broken json sometimes comes from external sources,
     * it's easier just try to fix it
     */
    @Test
    public void shouldUnmarshallBrokenJson() {
        MarketReportParameters marketReportParameters = new MarketReportParameters();
        marketReportParameters.setDomain("mts.\nru");
        JobParameters<MarketReportParameters> jobParameters = new JobParameters<>(marketReportParameters);
        String brokenJson = JaxbUtils.marshall(jobParameters).replace("\\n", "\n");
        JaxbUtils.unmarshall(brokenJson, JobParameters.class);
    }

    @Test
    public void shouldMarshalAuditParams() {
        AuditReportParameters auditReportParameters = new AuditReportParameters();
        auditReportParameters.setShop("MTC");
        auditReportParameters.setDomain("mts.ru");
        auditReportParameters.setFeedId(12345L);

        JobParameters<AuditReportParameters> jobParameters = new JobParameters<>(auditReportParameters);

        assertThat(JaxbUtils.unmarshall(JaxbUtils.marshall(jobParameters), JobParameters.class), is(jobParameters));
    }

}
