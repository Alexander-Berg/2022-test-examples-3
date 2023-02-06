package ru.yandex.autotests.market.billing.backend.PushCategoryMinBidsParamsExecutor;

import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.console.billing.ConsoleConnector;
import ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsoleResource;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.bids.CategoryMinBidsParams;
import ru.yandex.autotests.market.billing.backend.steps.CategoryMinBidsParamsSteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;

import java.io.IOException;
import java.util.List;

/**
 * @author chmilevfa@yandex-team.ru
 * @since 28.04.17
 */
@Aqua.Test(title = "Тест на соответствие таблицы shops_web.category_min_bids_params " +
        "и файла category_min_bids_params.xml в mds-s3")
@Feature("CategoryMinBidsParams")
@Description("Steps:\n" +
        "1. Запускаем и ждём завершения PushCategoryMinBidsParamsExecutor\n" +
        "2. Делаем снимок таблицы shops_web.category_min_bids_params\n" +
        "3. Забираем последнюю версию category_min_bids_params.xml из mds-s3\n" +
        "4. Сравниваем результаты 2 и 3 на соответствие")
@Issue("https://st.yandex-team.ru/MBI-21134")
public class CategoryMinBidsParamsUploadTest {

    private final CategoryMinBidsParamsSteps bidsParamsSteps = new CategoryMinBidsParamsSteps();

    @ClassRule
    public static MarketBillingConsoleResource consoleResource = new MarketBillingConsoleResource(ConsoleConnector.BILLING);

    @Test
    public void testCategoryMinBidsParamsUpload() throws IOException {
        consoleResource.getConsole().runAndWaitForPushCategoryMinBidsParamsExecutorToFinish();

        List<CategoryMinBidsParams> paramsFromBilling = bidsParamsSteps.getCategoryMinBidsParamsFromBilling();
        List<CategoryMinBidsParams> paramsFromStorage = bidsParamsSteps.getCategoryMinBidsParamsFromStorage();

        bidsParamsSteps.checkCategoryMinBidsParams(paramsFromBilling, paramsFromStorage);
    }
}
