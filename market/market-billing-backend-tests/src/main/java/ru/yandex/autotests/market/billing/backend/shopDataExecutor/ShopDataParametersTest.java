package ru.yandex.autotests.market.billing.backend.shopDataExecutor;

import java.util.ArrayList;
import java.util.Collection;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopData;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields;
import ru.yandex.autotests.market.billing.backend.data.wiki.ShopDataTestParamsProvider;
import ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.AUTOBROKER_ENABLED;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.CLIENT_ID;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.CPA;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.DATASOURCE_IS_ENABLED;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.DATASOURCE_NAME;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.DELIVERY_SRC;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.EXBID;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.FREE;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.FROM_MARKET;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.HOME_REGION;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.IS_CPA_PARTNER;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.IS_CPA_PRIOR;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.IS_DISCOUNTS_ENABLED;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.IS_ENABLED;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.IS_MOCK;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.IS_OFFLINE;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.IS_ONLINE;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.IS_TESTED;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.PHONE;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.PHONE_DISPLAY_OPTIONS;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.PREFIX;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.PREPAY_ENABLED;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.PRICE_SCHEME;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.PRIORITY_REGION_ORIGINAL;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.QUALITY_RATING;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.SHIPPING_FULL_TEXT;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.SHIPPING_PAGE_URL;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.SHIPPING_SHORT_TEXT;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.SHOPNAME;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.SHOP_CLUSTER_ID;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.SHOP_CURRENCY;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.SHOP_GRADES_COUNT;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.SHOW_PREMIUM;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.SHOW_SHIPPING_LINK;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.TARIFF;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.URL;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.USE_OPEN_STAT;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.shops.ShopDataFields.WARNINGS;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.getInstance;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectAutobrokerEnabled;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectClientId;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectCpa;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectCpaPartner;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectCpaPrior;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectDatasourceIsEnabled;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectDatasourceName;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectDeliverySrcParameter;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectExBid;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectFree;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectFromMarket;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectHomeRegion;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectIsDiscountEnabled;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectIsEnabled;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectIsMock;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectIsOffline;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectIsOnline;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectIsTested;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectPhone;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectPhoneDisplayOptions;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectPrefix;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectPrepayEnabled;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectPriceScheme;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectPriorityRegionOriginal;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectQualityRating;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectShippingFullText;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectShippingPageUrl;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectShippingShortText;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectShopClusterId;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectShopCurrency;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectShopGradesCount;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectShopNameParameter;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectShowPremium;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectShowShippingLink;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectTariff;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectUrl;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectUseOpenStat;
import static ru.yandex.autotests.market.billing.backend.steps.ShopDataParametersSteps.hasCorrectWarnings;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 6/11/15
 */

@Aqua.Test(title = "Тест параметров выгрузки shopdata (shopDataExecutor)")
@Feature("shopdata")
@Description("")
@RunWith(Parameterized.class)
public class ShopDataParametersTest {

    private ShopDataParametersSteps shopDataParametersSteps = getInstance();

    public static ShopDataParametersSteps.ShopDataFileDao dao;

    @Parameterized.Parameter
    public static ShopDataTestParamsProvider provider;

    @Parameterized.Parameter(1)
    public static ShopDataFields field;

    @Parameterized.Parameter(2)
    public static Matcher<ShopData> matcher;

    @Parameterized.Parameters(name= "{index}: проверка параметра {1}")
    public static Collection<Object[]> data() throws Exception {
        provider = ShopDataTestParamsProvider.getInstance();
        dao = ShopDataParametersSteps.getShopDataFileDao(provider.getAllInterestedShopIds());
        return new ArrayList<Object[]>(){{
            add(new Object[]{provider, PHONE,                       hasCorrectPhone()});
            add(new Object[]{provider, PHONE_DISPLAY_OPTIONS,       hasCorrectPhoneDisplayOptions()});
            add(new Object[]{provider, SHIPPING_FULL_TEXT,          hasCorrectShippingFullText()});
            add(new Object[]{provider, SHIPPING_SHORT_TEXT,         hasCorrectShippingShortText()});
            add(new Object[]{provider, DELIVERY_SRC,                hasCorrectDeliverySrcParameter()});
            add(new Object[]{provider, SHOPNAME,                    hasCorrectShopNameParameter()});
            add(new Object[]{provider, IS_DISCOUNTS_ENABLED,        hasCorrectIsDiscountEnabled()});
            add(new Object[]{provider, IS_TESTED,                   hasCorrectIsTested()});
            add(new Object[]{provider, AUTOBROKER_ENABLED,          hasCorrectAutobrokerEnabled()});
            add(new Object[]{provider, FREE,                        hasCorrectFree()});
            add(new Object[]{provider, EXBID,                       hasCorrectExBid()});
            add(new Object[]{provider, PRIORITY_REGION_ORIGINAL,    hasCorrectPriorityRegionOriginal()});
            add(new Object[]{provider, HOME_REGION,                 hasCorrectHomeRegion()});
            add(new Object[]{provider, IS_MOCK,                     hasCorrectIsMock()});
            add(new Object[]{provider, USE_OPEN_STAT,               hasCorrectUseOpenStat()});
            add(new Object[]{provider, IS_ONLINE,                   hasCorrectIsOnline()});
            add(new Object[]{provider, IS_CPA_PRIOR,                hasCorrectCpaPrior()});
            add(new Object[]{provider, IS_CPA_PARTNER,              hasCorrectCpaPartner()});
            add(new Object[]{provider, FROM_MARKET,                 hasCorrectFromMarket()});
            add(new Object[]{provider, TARIFF,                      hasCorrectTariff()});
            add(new Object[]{provider, SHOW_PREMIUM,                hasCorrectShowPremium()});
            add(new Object[]{provider, CPA,                         hasCorrectCpa()});
            add(new Object[]{provider, IS_OFFLINE,                  hasCorrectIsOffline()});
            add(new Object[]{provider, SHOP_CURRENCY,               hasCorrectShopCurrency()});
            add(new Object[]{provider, QUALITY_RATING,              hasCorrectQualityRating()});
            add(new Object[]{provider, IS_ENABLED,                  hasCorrectIsEnabled()});
            add(new Object[]{provider, PRICE_SCHEME,                hasCorrectPriceScheme()});
            add(new Object[]{provider, SHIPPING_PAGE_URL,           hasCorrectShippingPageUrl()});
            add(new Object[]{provider, SHOW_SHIPPING_LINK,          hasCorrectShowShippingLink()});
            add(new Object[]{provider, CLIENT_ID,                   hasCorrectClientId()});
            add(new Object[]{provider, DATASOURCE_NAME,             hasCorrectDatasourceName()});
            add(new Object[]{provider, URL,                         hasCorrectUrl()});
            add(new Object[]{provider, WARNINGS,                    hasCorrectWarnings()});
            add(new Object[]{provider, DATASOURCE_IS_ENABLED,       hasCorrectDatasourceIsEnabled()});
            add(new Object[]{provider, PREFIX,                      hasCorrectPrefix()});
            add(new Object[]{provider, SHOP_CLUSTER_ID,             hasCorrectShopClusterId()});
            add(new Object[]{provider, SHOP_GRADES_COUNT,           hasCorrectShopGradesCount()});//
            add(new Object[]{provider, PREPAY_ENABLED,              hasCorrectPrepayEnabled()});
        }};
    }

    @Test
    public void testParameter() {
        shopDataParametersSteps.checkParameterForShops(field, provider, dao, matcher);
    }

}
