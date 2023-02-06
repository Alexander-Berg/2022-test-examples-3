package ru.yandex.autotests.market.billing.backend.importGenerationsExecutor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsoleFactory;
import ru.yandex.autotests.market.billing.backend.steps.ImportGenerationsExecutorSteps;
import ru.yandex.autotests.market.indexer.backend.core.dao.entities.generation.GenerationMeta;
import ru.yandex.autotests.market.indexer.backend.core.dao.entities.generation.GenerationMetaWithDelta;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.LockRule;

/**
 * User: strangelet
 * Date: 21.11.12 : 16:32
 * refactored by syrisko
 */
@Aqua.Test(title = " Сравнение последних N поколений и дельт с одинаковым именем в базе билинга " +
        "и в версионном хранилище как результат работы importGenerationsExecutor.")
@Feature("importGenerationsExecutor")
@Issue("https://st.yandex-team.ru/AUTOTESTMARKET-1705")
public class GenerationsCaptureTest {

    @ClassRule
    public static LockRule lockRule = new LockRule("importGenerationsExecutor");

    private static ImportGenerationsExecutorSteps igeSteps = new ImportGenerationsExecutorSteps();

    private static GenerationMetaWithDelta billingGenAndDelta;
    private static Map<String, GenerationMetaWithDelta> indexerGenAndDeltas;


    @BeforeClass
    public static void prepareData() throws IOException {
        // Найти информацию о поколениях в S3
        indexerGenAndDeltas = igeSteps.getGenerationAndDeltasFromStorage();

        // Запустить индексацию поколений
        MarketBillingConsoleFactory.connectToBilling().runImportFeedLogsExecutor();

        // Получить последнее поколение из БД
        billingGenAndDelta = igeSteps.getLastGenerationAndDeltaFromBilling();
    }

    @Test
    public void testGenerationsAndDeltas() throws IOException {
        final GenerationMeta actualGen = billingGenAndDelta.getGeneration();
        final String name = actualGen.getName();

        final GenerationMetaWithDelta indexerGenAndDelta = indexerGenAndDeltas.get(name);
        final GenerationMeta expectedGen = indexerGenAndDelta.getGeneration();

        // сравниваем поколения по именам и количеству. актульных может быть больше,
        // так как между проверкой в сторадже и импортом могло накидаться еще
        igeSteps.compareGenerationAndDelta(billingGenAndDelta, indexerGenAndDelta);

        // сравниваем главные поколения
        igeSteps.compareGeneration(actualGen, expectedGen);

        // сравниваем дельты
        final List<GenerationMeta> expectedDeltas = indexerGenAndDelta.getDeltas();
        for (final GenerationMeta expectedDelta : expectedDeltas) {
            final GenerationMeta actualDelta = billingGenAndDelta.getDeltas().stream()
                .filter(g -> g.getReleaseDate().equals(expectedDelta.getReleaseDate()))
                .findFirst()
                .orElse(null);

            igeSteps.compareDelta(actualDelta, expectedDelta);
        }
    }

}
