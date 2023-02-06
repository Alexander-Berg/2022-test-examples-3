package ru.yandex.autotests.market.shop.backend.tms;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.data.wiki.StorageFileTestParamProvider;
import ru.yandex.autotests.market.mbi.environment.mds.MdsClientProvider;
import ru.yandex.autotests.market.shop.backend.core.console.shop.ConsoleConnector;
import ru.yandex.autotests.market.shop.backend.core.console.shop.MarketShopConsoleResource;
import ru.yandex.autotests.market.shop.backend.steps.TmsSteps;
import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assume.assumeThat;
import static ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy.LAST_ONLY;

/**
 * TODO would be good to launch this test in parallel, because each case runs a job that takes essential time.
 * (But it is probably not possible because of hazelcast lock upon job run)
 * <p>
 * @author ivmelnik on 29.08.16.
 */
@Aqua.Test(title = "Мониторинг обновления файлов в mdsS3 после запуска соответствующих tms задач")
@Feature("tmsTask.monitoring")
@RunWith(Parameterized.class)
public class ShopTmsFileMonitoring {

    private static final int TOTAL_TIME_MINUTES = 30;

    private static final int TOTAL_TIME_SECONDS = TOTAL_TIME_MINUTES * 60;

    private static MdsClientProvider mdsClientProvider = new MdsClientProvider();

    private static MdsS3Client mdsS3Client = mdsClientProvider.mdsS3Client();

    private static StorageFileTestParamProvider paramProvider = StorageFileTestParamProvider.getInstance();

    private static TmsSteps tmsSteps = new TmsSteps();

    @Parameterized.Parameters(name = "Проверка обновления файла {2} после запуска джобы {0}")
    public static Collection<Object[]> getParameters() {
        return paramProvider.getStorageFileTestParams().stream()
            .map(f -> new Object[]{f.getJobName(), f.getSkip(), f.getResourceConfigurationString(), f.getHistoryDays()})
            .collect(Collectors.toList());
    }

    @Parameterized.Parameter
    public String jobName;

    @Parameterized.Parameter(1)
    public boolean skipCase;

    @Parameterized.Parameter(2)
    public String resourceConfigurationString;

    @Parameterized.Parameter(3)
    public int historyDays;

    private static MarketShopConsoleResource consoleResource = new MarketShopConsoleResource(ConsoleConnector.SHOP);
    private static Timeout testTimeout = Timeout.seconds(TOTAL_TIME_SECONDS);

    @ClassRule
    public static RuleChain ruleChain = RuleChain
            .outerRule(consoleResource)
            .around(testTimeout);

    @Before
    public void setUp() throws Exception {
        checkSkip();
    }

    @Test
    public void testFileUpdate() throws Exception {
        final URL urlBefore = getUrl(resourceConfigurationString, historyDays);
        long lastModifiedBefore = tmsSteps.getLastModifiedDate(urlBefore);
        consoleResource.getConsole().runJob(jobName);
        final URL urlAfter = getUrl(resourceConfigurationString, historyDays);
        long lastModifiedAfter = tmsSteps.getLastModifiedDate(urlAfter);
        tmsSteps.checkFileChanged(lastModifiedBefore, lastModifiedAfter);
    }

    private void checkSkip() {
        assumeThat("Тест пропущен по параметру в таблице", skipCase, equalTo(false));
    }

    private ResourceLocation buildLocation(@Nonnull final String config, final int ttl) {
        final String bucket = mdsClientProvider.getMbiBucketName();
        final ResourceFileDescriptor fileDescriptor = ResourceFileDescriptor.parse(config);
        String key;
        if (ttl == 0) {
            key = mdsClientProvider.keyGenerator().generateLast(fileDescriptor);
        } else {
            final ResourceListing list = mdsS3Client.list(
                ResourceConfiguration.create(bucket, LAST_ONLY, fileDescriptor, null).toLocation(), true
            );
            final List<String> keys = list.getKeys();
            keys.sort(Comparator.reverseOrder());
            key = keys.get(0);
        }

        return ResourceLocation.create(bucket, key);
    }

    private URL getUrl(@Nonnull final String config, final int ttl) {
        final ResourceLocation resourceLocation = buildLocation(config, ttl);
        return mdsS3Client.getUrl(resourceLocation);
    }
}
