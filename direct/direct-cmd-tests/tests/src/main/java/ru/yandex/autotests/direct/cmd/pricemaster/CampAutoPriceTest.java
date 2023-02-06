package ru.yandex.autotests.direct.cmd.pricemaster;
//Task: TESTIRT-9409.

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.AjaxCampOptionsRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.SetAutoPriceAjaxRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BidsWarn;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.data.CmdBeans.COMMON_REQUEST_AJAX_CAMP_OPTIONS;
import static ru.yandex.autotests.direct.cmd.data.CmdBeans.COMMON_REQUEST_SET_AUTO_PRICE_AJAX;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение группы без фраз с условием ретаргетинга")
@Stories(TestFeatures.PpcCampAutoPrice.PPC_CAMP_AUTO_PRICE)
@Features(TestFeatures.PPC_CAMP_AUTO_PRICE)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class CampAutoPriceTest {
    private static final String CLIENT = "at-direct-price-master";
    private static final String PHRASE = "тестовая фраза";

    @ClassRule
    public static DirectCmdRule directCmdClassRule = DirectCmdRule.defaultClassRule();

    public TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private DirectJooqDbSteps dbSteps;


    public CampAutoPriceTest() {
        bannersRule = new TextBannersRule().withUlogin(CLIENT);
        bannersRule.getGroup().getPhrases().get(0).withPhrase(PHRASE);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        dbSteps = TestEnvironment.newDbSteps().useShardForLogin(CLIENT);
        Long bid = dbSteps.bidsSteps().getBidByCid(bannersRule.getCampaignId()).getId();
        dbSteps.bidsSteps().setBidsPlace(bid, 0, BidsWarn.No);
        AjaxCampOptionsRequest ajaxCampOptionsRequest = BeanLoadHelper.loadCmdBean(COMMON_REQUEST_AJAX_CAMP_OPTIONS,
                AjaxCampOptionsRequest.class);
        SetAutoPriceAjaxRequest setAutoPriceAjaxRequest = BeanLoadHelper.loadCmdBean(COMMON_REQUEST_SET_AUTO_PRICE_AJAX,
                SetAutoPriceAjaxRequest.class).withWizardSearchPositionCtrCorrection(74);

        bannersRule.getDirectCmdSteps().ajaxCampaignSteps().postAjaxCampOptions(ajaxCampOptionsRequest
                .withCid(bannersRule.getCampaignId()));
        bannersRule.getDirectCmdSteps().ajaxCampaignSteps().postSetAutoPriceAjax(setAutoPriceAjaxRequest
                .withCid(bannersRule.getCampaignId()));
    }

    @Test
    @Description("Запуск скрипта ppcCampAutoPrice проверяем place")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9929")
    public void priceMasterRunCheckPlace() {
        cmdRule.darkSideSteps().getRunScriptSteps().runPpcCampAutoPrice(dbSteps.getCurrentPpcShard(),
                bannersRule.getCampaignId().intValue());

        assertThat("Значения place соответсвует ожиданиям",
                dbSteps.bidsSteps().getBidsByCid(bannersRule.getCampaignId()).stream()
                        .findFirst().orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть bid"))
                        .getPlace(),
                greaterThan(0));

    }

    @Test
    @Description("Запуск скрипта ppcCampAutoPrice проверяем warn")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9930")
    public void priceMasterRunCheckWarn() {
        cmdRule.darkSideSteps().getRunScriptSteps().runPpcCampAutoPrice(dbSteps.getCurrentPpcShard(),
                bannersRule.getCampaignId().intValue());

        assertThat("Значение warn соответствует ожиданиям",
                dbSteps.bidsSteps().getBidsByCid(bannersRule.getCampaignId()).stream()
                        .findFirst().orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть bid"))
                        .getWarn(),
                equalTo(BidsWarn.Yes));
    }
}
