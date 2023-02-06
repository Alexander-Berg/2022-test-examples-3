package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.autobudget.AutobudgetPricesSetResponse;
import ru.yandex.autotests.directapi.model.clients.ConvertType;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Title;

/**
 * Author: xy6er
 * Date: 21.01.15
 * Test tickets:
 * https://st.yandex-team.ru/TESTIRT-8716
 * https://st.yandex-team.ru/TESTIRT-3506
 * https://st.yandex-team.ru/TESTIRT-3980
 */
@Issues({
        @Issue("https://st.yandex-team.ru/DIRECT-38458"),
        @Issue("https://st.yandex-team.ru/DIRECT-36421"),
        @Issue("https://st.yandex-team.ru/DIRECT-38234")
})
@Aqua.Test(title = "AutobudgetPrices.set - вызов после конвертации клиента с ConvertType.COPY")
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
public class AutobudgetPricesSetAfterConvertationCopyTest extends BaseAutobudgetPricesSetAfterConvertationTest {

    private static final int ERROR_CODE = 9;
    private static final String ERROR_MESSAGE = "archived old version of converted campaign";

    @Test
    @Title("AutobudgetPrices.set на кампании после конвертации копированием")
    @Description("Конвертируем копированием фишкового клиента в рублевого. " +
            "Вызываем для старой кампании AutobudgetPrices.set. " +
            "Проверяем, что в ответ получили ошибку: " + ERROR_MESSAGE)
    public void autobudgetPricesAfterConvertationCopyTest() {
        api.userSteps.clientFakeSteps().convertCurrency(login, Currency.RUB.toString(), ConvertType.COPY);
        Money price = MoneyCurrency.get(Currency.YND_FIXED).getDefaultPriceConstructorAmount();
        request.setPrice(price.floatValue());
        request.setContextPrice(price.floatValue());
        request.setCurrency(0);

        AutobudgetPricesSetResponse expectedError = new AutobudgetPricesSetResponse(
                ERROR_CODE,
                ERROR_MESSAGE,
                request.getGroupExportID(),
                request.getPhraseID()
        );
        api.userSteps.getDarkSideSteps().getAutobudgetSteps()
                .setWithExpectedErrors(new AutobudgetPricesSetRequest[]{request}, expectedError);
    }
}
