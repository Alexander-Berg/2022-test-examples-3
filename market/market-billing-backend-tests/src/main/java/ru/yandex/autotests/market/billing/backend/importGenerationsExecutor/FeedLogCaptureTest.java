package ru.yandex.autotests.market.billing.backend.importGenerationsExecutor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsoleFactory;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.feed.FeedLogWithSession;
import ru.yandex.autotests.market.billing.backend.steps.ImportGenerationsExecutorSteps;
import ru.yandex.autotests.market.indexer.backend.core.dao.entities.generation.GenerationMeta;
import ru.yandex.autotests.market.indexer.backend.core.dao.entities.generation.GenerationMetaWithDelta;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.LockRule;


/**
 * User: strangelet
 * Date: 16.10.12 : 15:30
 */
@Aqua.Test(title = "Сравниваются значения FeedLogs выгруженные в базу билинга из MDS в результате работы" +
        " задачи importGenerationsExecutor")
@Feature("importGenerationsExecutor")
@Issue("https://st.yandex-team.ru/AUTOTESTMARKET-1705")
public class FeedLogCaptureTest {
    public static ImportGenerationsExecutorSteps igeSteps = new ImportGenerationsExecutorSteps();
    @ClassRule
    public static LockRule lockRule = new LockRule("importGenerationsExecutor");
    private  Map<String, GenerationMetaWithDelta> indexerGens;
    private GenerationMetaWithDelta billingGen;
    private  List<String> storageFeedLogFiles;
    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();
    private List<Integer> testedFeeds;

    @Before
    public void prepareData() throws IOException {
        MarketBillingConsoleFactory.connectToBilling().runImportFeedLogsExecutor();

        billingGen = igeSteps.getLastGenerationAndDeltaFromBilling();
        indexerGens = igeSteps.getGenerationAndDeltasFromStorage();
        storageFeedLogFiles = igeSteps.getStorageFeedLogFiles();

        // Получаем feedId из свежего full pbuf, пропуская слишком большие
        // и отбираем 100 из известных в тестинге.
        testedFeeds = igeSteps.createTestedFeedsCollectionFromStorage(billingGen.getGeneration(), storageFeedLogFiles);
    }

    @Test
    public void testFeedLogsPack() throws IOException {
        final GenerationMeta bilGen = billingGen.getGeneration();
        final GenerationMetaWithDelta indexGenAndDelta = indexerGens.get(bilGen.getName());
        final GenerationMeta indexGen = indexGenAndDelta.getGeneration();

        //  обновилось ли поколение в биллинге?
        igeSteps.checkGenerationName(billingGen, indexGenAndDelta);

        //  получим фиды из индексатора
        List<FeedLogWithSession> indexerFeeds =
                igeSteps.selectFeedsFromStorage(indexGen, storageFeedLogFiles,
                        stream -> stream
                                // оставляем только нужные
                                .filter(f -> testedFeeds.contains(f.getFeedId()))
                                .map(feed -> igeSteps.getFeedLogFromProto(feed, bilGen))
                );

        // получим фиды из биллинга
        List<FeedLogWithSession> billFeeds = igeSteps.selectFeedsFromBilling(bilGen, testedFeeds);

        // сравним размер выборок
        igeSteps.checkFeedSizes(errorCollector, testedFeeds, indexerFeeds.size(), billFeeds.size());

        // сравнить фиды
        igeSteps.checkFeeds(indexerFeeds, billFeeds);
    }

}
