package ru.yandex.autotests.market.billing.backend.postamatCategoriesAvailiabilityExportExecutor;

import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.ConsoleConnector;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsoleResource;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.delivery.PostamatCategory;
import ru.yandex.autotests.market.billing.backend.steps.MarketDeliverySteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;

import java.util.List;

import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.DELIVERY_SERVICE_CATEGORIES_AVAILIABILITY_EXPORT_EXECUTOR;

/**
 * Created by ivmelnik on 14.10.16.
 */
@Aqua.Test(title = "Тест на выгрузку категорий товаров для почтоматов Инпоста")
@Feature("Inpost")
@Description("Проверяем выгрузку категорий товаров, которые могут быть доставлены в почтоматы. " +
        "Выгрузка идет в mds-s3, для индексатора.")
@Issue("https://st.yandex-team.ru/AUTOTESTMARKET-3911")
public class PostamatCategoriesExportTest {

    private MarketDeliverySteps marketDeliverySteps = new MarketDeliverySteps();

    @ClassRule
    public static MarketBillingConsoleResource consoleResource = new MarketBillingConsoleResource(ConsoleConnector.BILLING);

    @Test
    public void testCategoryExport() {
        List<PostamatCategory> postamatCategoriesFromDb = marketDeliverySteps.getPostamatCategories();
        consoleResource.getConsole().runJob(DELIVERY_SERVICE_CATEGORIES_AVAILIABILITY_EXPORT_EXECUTOR);
        List<PostamatCategory> postamatCategoriesFromExport = marketDeliverySteps.getPostamatCategoriesFromExport();
        marketDeliverySteps.checkPostamatCategories(postamatCategoriesFromDb, postamatCategoriesFromExport);
    }
}
