package ru.yandex.autotests.directintapi.tests.autobudgetpricesset;

import com.yandex.direct.api.v5.keywords.KeywordGetItem;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.model.clients.ConvertType;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Author: xy6er
 * Date: 21.01.15
 * https://st.yandex-team.ru/TESTIRT-3506
 * https://st.yandex-team.ru/TESTIRT-3980
 */
@Aqua.Test(title = "AutobudgetPrices.set - вызов после конвертации клиента с ConvertType.MODIFY")
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.AUTOBUDGET_PRICES_SET)
public class AutobudgetPricesSetAfterConvertationModifyTest extends BaseAutobudgetPricesSetAfterConvertationTest {

    @Test
    public void autobudgetPricesSetAfterConvertationModifyTest() {
        api.userSteps.clientFakeSteps().convertCurrency(login, Currency.RUB.toString(), ConvertType.MODIFY);
        log.info("Проверяем, что AutobudgetPrices.set проходит, если время конвертации MODIFY прошло");
        Money price = MoneyCurrency.get(Currency.RUB).getDefaultPriceConstructorAmount();
        request.setPrice(price.floatValue());
        request.setContextPrice(price.floatValue());
        request.setCurrency(1);
        api.userSteps.getDarkSideSteps().getAutobudgetSteps().set(request);

        log.info("Проверяем, что значение price изменилось");
        KeywordGetItem keywordGetItem = api.userSteps.keywordsSteps().keywordsGetById(login, keywordId).get(0);
        assertThat("Не удалось установить верное значение поля Price",
                keywordGetItem.getBid(),
                equalTo(price.bidLong().longValue()));
    }

}
