package ru.yandex.market.tsum.pipelines.checkout.jobs;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.checkout.jobs.MultipleTanksShootingConfiguration.DefaultShootingOptions;
import ru.yandex.market.tsum.pipelines.checkout.jobs.MultipleTanksShootingConfiguration.PerTankShootingOptions;
import ru.yandex.market.tsum.pipelines.checkout.jobs.MultipleTanksShootingConfiguration.ShareableShootingOptions;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CreateShootingIssueJobTest.Config.class)
public class CreateShootingIssueJobTest {
    @Autowired
    private JobTester jobTester;

    @Test
    public void description1DefaultAnd1PerTank() throws Exception {
        MultipleTanksShootingConfiguration configuration = createMultipleTanksShootingConfiguration(
            generateShootingOptions(1, 1, 1, 1),
            generatePerTankShootingOptions(2, 2, 2, 2, 2)
        );
        CreateShootingIssueJob job = runJob(configuration);
        verify(job).createIssue(any(JobContext.class),
            argThat(s -> contains(s, getRegionId(2), getCheckouterBalancer(2), getMutableDistribution(2),
                getLoyaltyCoinsPromoId(2)) && !s.contains(getTankBaseUrl(2))));
    }

    @Test
    public void description1DefaultAnd2PerTank() throws Exception {
        MultipleTanksShootingConfiguration configuration = createMultipleTanksShootingConfiguration(
            generateShootingOptions(1, 1, 1, 1),
            generatePerTankShootingOptions(2, 2, 2, 2, 2),
            generatePerTankShootingOptions(3, 3, 3, 3, 3)
        );
        CreateShootingIssueJob job = runJob(configuration);
        verify(job).createIssue(any(JobContext.class),
            argThat(s -> contains(s, getTankBaseUrl(2), getTankBaseUrl(3))
                && notContains(s, getRegionId(1))));
    }

    @Test
    public void description1DefaultAnd2PerTankWithCommons() throws Exception {
        MultipleTanksShootingConfiguration configuration = createMultipleTanksShootingConfiguration(
            generateShootingOptions(1, 1, 1, 1),
            generatePerTankShootingOptions(2, 2, 2, 2, 2),
            generatePerTankShootingOptions(2, 3, 2, 3, 3)
        );
        CreateShootingIssueJob job = runJob(configuration);
        verify(job).createIssue(any(JobContext.class),
            argThat(s -> contains(s, getTankBaseUrl(2), getTankBaseUrl(3), getLoyaltyCoinsPromoId(2),
                getLoyaltyCoinsPromoId(3), getMutableDistribution(2), getMutableDistribution(3))
                && StringUtils.countMatches(s, getCheckouterBalancer(2)) == 1
                && StringUtils.countMatches(s, getRegionId(2)) == 1
                && notContains(s, getRegionId(1))));
    }

    private CreateShootingIssueJob runJob(MultipleTanksShootingConfiguration configuration) throws Exception {
        CreateShootingIssueJob job = jobTester
            .jobInstanceBuilder(CreateShootingIssueJob.class)
            .withResources(mock(ShootingIssueJobConfig.class), mock(PandoraShootingConfigImpl.class),
                mock(ExternalServicesResourcesConfig.class), configuration)
            .create();
        CreateShootingIssueJob result = Mockito.spy(job);
        TestJobContext context = new TestJobContext();
        result.execute(context);
        return result;
    }

    private static boolean contains(String subject, Object... values) {
        return Stream.of(values).map(Object::toString).allMatch(subject::contains);
    }

    private static boolean notContains(String subject, String... values) {
        return Stream.of(values).noneMatch(subject::contains);
    }

    private static MultipleTanksShootingConfiguration createMultipleTanksShootingConfiguration(
        ShareableShootingOptions defaultShootingOption,
        PerTankShootingOptions... perTankShootingOptions) {
        final MultipleTanksShootingConfiguration result = new MultipleTanksShootingConfiguration();
        result.setDefaultShootingOptions(toDefaultShootingOptions(defaultShootingOption));
        result.setPerTankShootingOptions(Arrays.asList(perTankShootingOptions));
        return result;
    }

    private static PerTankShootingOptions toPerTankShootingOptions(
        ShareableShootingOptions shootingOptions, String tankBaseUrl) {
        final PandoraTankConfigImpl tankConfig = new PandoraTankConfigImpl();
        tankConfig.setTankBaseUrl(tankBaseUrl);
        PerTankShootingOptions result = new PerTankShootingOptions();
        result.setTankConfig(tankConfig);
        result.setCheckouterConfig(shootingOptions.getCheckouterConfig());
        result.setLoyaltyShootingOptions(shootingOptions.getLoyaltyShootingOptions());
        result.setMutableConfig(shootingOptions.getMutableConfig());
        result.setRegionSpecificConfig(shootingOptions.getRegionSpecificConfig());
        return result;
    }

    private static DefaultShootingOptions toDefaultShootingOptions(ShareableShootingOptions shootingOptions) {
        DefaultShootingOptions result = new DefaultShootingOptions();
        result.setCheckouterConfig(shootingOptions.getCheckouterConfig());
        result.setLoyaltyShootingOptions(shootingOptions.getLoyaltyShootingOptions());
        result.setMutableConfig(shootingOptions.getMutableConfig());
        result.setRegionSpecificConfig(shootingOptions.getRegionSpecificConfig());
        return result;
    }

    private static PerTankShootingOptions generatePerTankShootingOptions(
        int checkout, int loyalty, int region, int mutable, int tank) {
        return toPerTankShootingOptions(
            generateShootingOptions(checkout, loyalty, region, mutable), getTankBaseUrl(tank));
    }

    @NotNull
    private static String getTankBaseUrl(int variant) {
        return "tankBaseUrl" + variant;
    }

    @NotNull
    private static ShareableShootingOptions generateShootingOptions(
        int checkout, int loyalty, int region, int mutable) {
        ShareableShootingOptions result = new ShareableShootingOptions();
        if (checkout > 0) {
            PandoraCheckouterConfigImpl.Builder builder = new PandoraCheckouterConfigImpl.Builder()
                .setBalancer(getCheckouterBalancer(checkout))
                .setCartRepeats(10)
                .setCartDurationSec(10)
                .setHandles("handles" + checkout)
                .setHandlesCommonDelayMs(1000)
                .setOffersDistribution("offerDistribution" + checkout);
            result.setCheckouterConfig(builder.build());
        }
        if (loyalty > 0) {
            LoyaltyShootingOptions.Builder builder = LoyaltyShootingOptions.builder()
                .withCoinsPromoId(getLoyaltyCoinsPromoId(loyalty))
                .withPercentOfCashbackOrders(200 + loyalty)
                .withPercentOfFlashOrders(300 + loyalty)
                .withPercentOfOrdersPaidByCoins(400 + loyalty)
                .withPercentOfOrdersUsingPromo(500 + loyalty);
            result.setLoyaltyShootingOptions(builder.build());
        }
        if (region > 0) {
            PandoraRegionSpecificConfigImpl regionSpecificConfig = new PandoraRegionSpecificConfigImpl(
                "deliveryServices" + region,
                "warehouseId" + region,
                getRegionId(region),
                "addresses" + region,
                "#promo" + region,
                "DELIVERY" + region);
            result.setRegionSpecificConfig(regionSpecificConfig);
        }
        if (mutable > 0) {
            PandoraMutableConfigImpl.Builder builder = PandoraMutableConfigImpl.builder()
                .setCoinsPerHour(1)
                .setDistribution(getMutableDistribution(mutable))
                .setDuration(10)
                .setOrdersPerHour(3600 + mutable)
                .setStocksRequiredRate("1.0");
            result.setMutableConfig(builder.build());
        }
        return result;
    }

    @NotNull
    private static String getMutableDistribution(int variant) {
        return "distribution" + variant;
    }

    @NotNull
    private static String getRegionId(int variant) {
        return "regionId" + variant;
    }

    private static int getLoyaltyCoinsPromoId(int variant) {
        return 20000 + variant;
    }

    @NotNull
    private static String getCheckouterBalancer(int variant) {
        return "balancer" + variant;
    }

    @Configuration
    @Import(JobTesterConfig.class)
    static class Config {
        @Bean
        Issues startrekIssues() {
            Issues mock = mock(Issues.class);
            Issue issue = mock(Issue.class);
            when(issue.getKey()).thenReturn("TICKET-123");
            when(mock.create(any(IssueCreate.class), anyBoolean())).thenReturn(issue);
            return mock;
        }
    }
}
