package ru.yandex.autotests.direct.web.cleandata;


import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import ru.yandex.autotests.direct.web.TestEnvironment;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.web.steps.UserSteps;
import ru.yandex.autotests.direct.web.util.WebRuleFactory;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.webdriver.rules.WebDriverConfiguration;

import static ru.yandex.autotests.direct.web.TestEnvironment.getWebDriverConfiguration;

@Aqua.Test
@Title("Удаление кампаний и объектов")
@Stories("Удаление кампаний и объектов")
@Features("Очистка тестовых данных")
public class CleanTestDataTest {

    public WebDriverConfiguration config = getWebDriverConfiguration();
    @ClassRule
    public static RuleChain defaultClassRuleChain = WebRuleFactory.defaultClassRuleChain();
    @Rule
    public RuleChain defaultRuleChain = WebRuleFactory.defaultRuleChain(config);

    public UserSteps user;

    @Before
    public void before() {
        user = UserSteps.getInstance(UserSteps.class, config);
    }


    @Test
    @Title("Удаление кампаний клиента dna-test-client")
    @Description("Удаление кампаний клиента dna-test-client")
    public void delCampsDnaTestClient() {
        user.byUsingBackend().deleteCampaignsExceptLast("dna-test-client", 300);
    }

    @Test
    @Title("Удаление кампаний клиента at-direct-dna-smart-tester")
    @Description("Удаление кампаний клиента at-direct-dna-smart-tester")
    public void delCampsAtDirectDnaSmart() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-direct-dna-smart-tester", 300);
    }

    @Test
    @Title("Удаление библиотек минус-фраз клиента dna-test-client")
    @Description("Удаление библиотек минус-фраз клиента dna-test-client")
    public void deleteMinusPhrasesLibrariesDnaTestClient() {
        user.byUsingBackend().deleteMinusPhraseLibrariesExceptLast("dna-test-client", 5);
    }

    @Test
    @Title("Удаление кампаний клиента at-priceconstructor-c")
    @Description("Удаление кампаний клиента at-priceconstructor-c")
    public void delCampsAtPriceconstructorC() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-priceconstructor-c", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-contactinfo-validation-c")
    @Description("Удаление кампаний клиента at-contactinfo-validation-c")
    public void delCampsAtContactinfoC() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-contactinfo-validation-c", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-daybudget-cl5")
    @Description("Удаление кампаний клиента at-daybudget-cl5")
    public void delCampsAtDaybudgetCl() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-daybudget-cl5", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-contactinfo-parameters-c")
    @Description("Удаление кампаний клиента at-contactinfo-parameters-c")
    public void delCampsAtContactinfoParam() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-contactinfo-parameters-c", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-daybudget-c")
    @Description("Удаление кампаний клиента at-daybudget-c")
    public void delCampsAtDaybudgetC() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-daybudget-c", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-priceconstructor-c2")
    @Description("Удаление кампаний клиента at-priceconstructor-c2")
    public void delCampsAtPriceconstructorC2() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-priceconstructor-c2", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-cpm-tester")
    @Description("Удаление кампаний клиента at-cpm-tester")
    public void delCampsAtCpmTester() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-cpm-tester", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-direct-copy-rub")
    @Description("Удаление кампаний клиента at-direct-copy-rub")
    public void delCampsAtCopyRub() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-direct-copy-rub", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-direct-phrase-prices-c")
    @Description("Удаление кампаний клиента at-direct-phrase-prices-c")
    public void delCampsAtDirectPricesC() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-direct-phrase-prices-c", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-direct-feeds-c4")
    @Description("Удаление кампаний клиента at-direct-feeds-c4")
    public void delCampsAtDirectFeedsC4() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-direct-feeds-c4", 8);
    }

    @Test
    @Title("Удаление фидов клиента at-direct-feeds-c4")
    @Description("Удаление фидов клиента at-direct-feeds-c4")
    public void delFeedsAtDirectFeedsC4() {
        user.byUsingBackend().deleteFeedsExceptFirstAndLast("at-direct-feeds-c4", 0,10);
    }

    @Test
    @Title("Удаление кампаний клиента at-tester-actionlinks")
    @Description("Удаление кампаний клиента at-tester-actionlinks")
    public void delCampsAtTesterActions() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-tester-actionlinks", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-timetargeting-client")
    @Description("Удаление кампаний клиента at-timetargeting-client")
    public void delCampsAtTimetargetCl() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-timetargeting-client", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-direct-bannergrch-c")
    @Description("Удаление кампаний клиента at-direct-bannergrch-c")
    public void delCampsAtDirectBannergrchC() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-direct-bannergrch-c", 25);
    }

    @Test
    @Title("Удаление кампаний клиента at-direct-integration")
    @Description("Удаление кампаний клиента at-direct-integration")
    public void delCampsAtDirectIntegration() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-direct-integration", 30);
    }

    @Test
    @Title("Удаление кампаний клиента at-direct-metrika-counter")
    @Description("Удаление кампаний клиента at-direct-metrika-counter")
    public void delCampsAtDirectMetrikaCounter() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-direct-metrika-counter", 50);
    }

    @Test
    @Title("Удаление кампаний клиента at-serv-cpi")
    @Description("Удаление кампаний клиента at-serv-cpi")
    public void delCampsAtServCpi() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-serv-cpi", 50);
    }

    @Test
    @Title("Удаление кампаний клиента at-transfer-without-wallet")
    @Description("Удаление кампаний клиента at-transfer-without-wallet")
    public void delCampsAtTransferWithoutWallet() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-transfer-without-wallet", 120);
    }

    @Test
    @Title("Удаление кампаний клиента at-capmeditpage-c")
    @Description("Удаление кампаний клиента at-capmeditpage-c")
    public void delCampsAtCapmeditpageC() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-capmeditpage-c", 100);
    }

    @Test
    @Title("Удаление кампаний клиента at-strategies-c-ic")
    @Description("Удаление кампаний клиента at-strategies-c-ic")
    public void delCampsAtStratCIc() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-strategies-c-ic", 100);
    }

    @Test
    @Title("Удаление кампаний клиента at-direct-mc-banner-tester-1")
    @Description("Удаление кампаний клиента at-direct-mc-banner-tester-1")
    public void delCampsAtMcBanner() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-direct-mc-banner-tester-1", 25);
    }

    @Test
    @Title("Удаление кампаний клиента yndx-uac-e2e-test")
    @Description("Удаление кампаний клиента yndx-uac-e2e-test")
    public void delCampsUacE2e() {
        user.byUsingBackend().deleteCampaignsExceptLast("yndx-uac-e2e-test", 300);
    }

    @Test
    @Title("Удаление кампаний клиента yndx-uac-create-rmp")
    @Description("Удаление кампаний клиента yndx-uac-create-rmp")
    public void delCampsUacCreateRmp() {
        user.byUsingBackend().deleteCampaignsExceptLast("yndx-uac-create-rmp", 300);
    }

    @Test
    @Title("Удаление кампаний клиента yndx-uac-create-rmp-draft")
    @Description("Удаление кампаний клиента yndx-uac-create-rmp-draft")
    public void delCampsUacCreateRmpDraft() {
        user.byUsingBackend().deleteCampaignsExceptLast("yndx-uac-create-rmp-draft", 300);
    }

    @Test
    @Title("Удаление кампаний клиента at-transport-tester-7")
    @Description("Удаление кампаний клиента at-transport-tester-7")
    public void delCampsTransportTester7() {
        user.byUsingBackend().deleteCampaignsExceptLast("at-transport-tester-7", 30);
    }

    @Test
    @Title("Удаление кампаний клиента osudar-dna")
    @Description("Удаление кампаний клиента osudar-dna")
    public void delCampsOsudarDna() {
        user.byUsingBackend().deleteCampaignsExceptLast("osudar-dna", 30);
    }
}
