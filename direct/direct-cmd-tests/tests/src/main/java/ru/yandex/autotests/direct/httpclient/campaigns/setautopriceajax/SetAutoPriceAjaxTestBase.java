package ru.yandex.autotests.direct.httpclient.campaigns.setautopriceajax;

import java.math.RoundingMode;
import java.util.Arrays;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.setautopriceajax.SetAutoPriceAjaxRequestParameters;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyFormat;
import ru.yandex.autotests.directapi.model.User;

import static ru.yandex.autotests.direct.httpclient.util.CommonUtils.sleep;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 11.06.15.
 * TESTIRT-5015
 */
public abstract class SetAutoPriceAjaxTestBase {
    protected String CLIENT_LOGIN = "at-direct-b-autopriceajax";
    public BannersRule bannersRule = new TextBannersRule()
            .overrideGroupTemplate(new Group().withBanners(
                    Arrays.asList(BannersFactory.getDefaultTextBanner(), BannersFactory.getDefaultTextBanner())))
            .withUlogin(CLIENT_LOGIN);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static CSRFToken csrfToken;
    final String EXPECTED_PRICE = "100";
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    protected Integer campaignId;
    protected Long firstBannerId;
    protected Long secondBannerId;
    protected SetAutoPriceAjaxRequestParameters requestParams;
    protected DirectResponse response;

    public SetAutoPriceAjaxTestBase(SetAutoPriceAjaxRequestParameters requestParams) {
        this.requestParams = requestParams;
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId().intValue();
        firstBannerId = bannersRule.getCurrentGroup().getBanners().get(0).getBid();
        secondBannerId = bannersRule.getCurrentGroup().getBanners().get(1).getBid();
        requestParams.setCid(String.valueOf(campaignId));
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT_LOGIN).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
    }

    protected String getConvertedPrice(Float price) {
        return Money.valueOf(price).convert(User.get(CLIENT_LOGIN).getCurrency()).setScale(RoundingMode.CEILING)
                .stringValue(MoneyFormat.INTEGER);
    }

    protected void waitAndCheckChangedPriceForBanners(Matcher matcher, String... expectedPrices) {
        final int ATTEMPTS = 20;
        final int TIMEOUT = 2_000;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            if (!matcher.matches(expectedPrices[0])) {
                sleep(TIMEOUT);
            } else {
                for (String price : expectedPrices) {
                    assertThat("цена фразы соответствует ожиданиям", price, matcher);
                }
                break;
            }
        }
    }
}
