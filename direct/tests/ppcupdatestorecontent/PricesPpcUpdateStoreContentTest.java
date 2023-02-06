package ru.yandex.autotests.directintapi.tests.ppcupdatestorecontent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersMobileContentPrimaryAction;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.MobileContentRecord;
import ru.yandex.autotests.direct.fakebsproxy.beans.updatestorecontent.UpdateStoreContentMobileResponseBean;
import ru.yandex.autotests.directapi.darkside.connection.FakeBsProxyConfig;
import ru.yandex.autotests.directapi.darkside.model.bslogs.mobile.CountryCurrencies;
import ru.yandex.autotests.directapi.darkside.model.bslogs.mobile.PriceInJson;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.directintapi.utils.StoreContentHelper;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by buhter on 20/08/15.
 */

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.PPC_UPDATE_STORE_CONTENT)
@Issue("https://st.yandex-team.ru/TESTIRT-6732")
@Description("ppcUpdateStoreContent.pl обновление prices")
public class PricesPpcUpdateStoreContentTest extends BasePpcUpdateStoreContentTest {
    @Override
    protected String getLogin() {
        return "at-direct-upd-store-prices";
    }

    private static Map<String, String> prices = new HashMap<>();
    private static Map<String, String> expectedPrices = new HashMap<>();

    static {
        prices.put(CountryCurrencies.BY.country(), "4");
        prices.put(CountryCurrencies.TR.country(), "5.6");
        prices.put(CountryCurrencies.RU.country(), "7.80");
        prices.put(CountryCurrencies.KZ.country(), "8.123");
        prices.put(CountryCurrencies.UA.country(), "6.7693656");
        prices.put(CountryCurrencies.US.country(), "6,455");

        expectedPrices.put(CountryCurrencies.BY.country(), "4");
        expectedPrices.put(CountryCurrencies.TR.country(), "5.60");
        expectedPrices.put(CountryCurrencies.RU.country(), "7.80");
        expectedPrices.put(CountryCurrencies.KZ.country(), "8.12");
        expectedPrices.put(CountryCurrencies.UA.country(), "6.77");
        expectedPrices.put(CountryCurrencies.US.country(), "6.46");
    }

    @Override
    protected UpdateStoreContentMobileResponseBean getResponseBean(CountryCurrencies countryCurrencies
            , MobileContentRecord mobileContent) {
        UpdateStoreContentMobileResponseBean bean = StoreContentHelper.getDefaultResponseBean(countryCurrencies
                , mobileContent);
        bean.getResponse().getResults().get(0).getGroups().get(0).getDocuments().get(0).getProperties()
                .setPrice(prices.get(countryCurrencies.country()));
        return bean;
    }

    @Override
    protected void updateMobileContent(MobileContentRecord mobileContent) {
    }

    @Test
    public void test() {
        Type COMPLEX_MAP_TYPE = new TypeToken<Map<String, Map<String, PriceInJson>>>() {
        }.getType();
        Map<String, Map<String, PriceInJson>> dbPricesMap = new HashMap<>();
        for (CountryCurrencies countryCurrencies : CountryCurrencies.values()) {
            Map<String, PriceInJson> actionPricesMap = new HashMap<>();
            PriceInJson priceInJson = new PriceInJson();
            priceInJson.setPriceCurrency(countryCurrencies.currency().value());
            priceInJson.setPrice(expectedPrices.get(countryCurrencies.country()));
            for (BannersMobileContentPrimaryAction action : BannersMobileContentPrimaryAction.values()) {
                actionPricesMap.put(action.getLiteral(), priceInJson);
            }
            dbPricesMap.put(countryCurrencies.country(), actionPricesMap);
        }

        api.userSteps.getDarkSideSteps().getRunScriptSteps().runPpcUpdateStoreContent(User.get(login).getClientID()
                , FakeBsProxyConfig.getMobileStoreDataUrl(), shard);

        mobileContent = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .mobileContentSteps().getMobileContent(mobContentId);

        assertThat("поле price заполнилось верно", new Gson().fromJson(mobileContent.getPricesJson(), COMPLEX_MAP_TYPE)
                , beanDiffer(dbPricesMap));
    }
}
