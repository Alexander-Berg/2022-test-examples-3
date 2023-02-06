package ru.yandex.direct.core.entity.promocodes.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;

import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeMobileAppCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newGeoCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.currency.CurrencyCode.RUB;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PromocodeEntryAvailableServiceTest extends PromocodeTestBase {

    private static final String DOMAIN_1LEVEL = "ru";
    private static final String DOMAIN_2LEVEL_A_WITH_WWW = "www.domainA.ru";
    private static final String DOMAIN_2LEVEL_B = "domainB.ru";
    private static final String DOMAIN_2LEVEL_C_ENCODED = "xn--80atjc.xn--p1ai";
    private static final String DOMAIN_3LEVEL = "www.ssu.domain2.ru";

    @Autowired
    private PromocodeEntryAvailableService service;
    @Autowired
    private Steps steps;

    private CampaignSteps campaignSteps;
    private ClientInfo clientInfo;
    private ClientId clientId;
    private long uid;

    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        uid = userInfo.getUid();
        clientId = userInfo.getClientId();
        campaignSteps = steps.campaignSteps();
    }

    @Test
    public void textCampaignWithoutMoney_available() {
        Campaign textCampaign = emptyTextCampaign(clientId, uid);
        CampaignInfo campaignInfo = campaignSteps.createCampaign(textCampaign, clientInfo);

        addTextBanner(campaignInfo, DOMAIN_2LEVEL_A_WITH_WWW);

        Assert.assertTrue(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Проверяем, что если были оплаты, промокод вводить нельзя
     */
    @Test
    public void textCampaignWithMoney_notAvailable() {
        Campaign textCampaign = activeTextCampaign(clientId, uid);
        CampaignInfo campaignInfo = campaignSteps.createCampaign(textCampaign, clientInfo);

        addTextBanner(campaignInfo, DOMAIN_2LEVEL_A_WITH_WWW);

        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Проверяем, что если оплат не было, домена нет в статистике, промокод вводить можно
     * При этом оплаченная кампания GEO не аффектит нас, так как она от другого сервиса
     */
    @Test
    public void geoCampaignWithMoney_available() {
        Campaign geoCampaign = newGeoCampaign(clientId, uid)
                .withBalanceInfo(activeBalanceInfo(CurrencyCode.RUB));
        campaignSteps.createCampaign(geoCampaign, clientInfo);

        CampaignInfo textCampaignInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);

        addTextBanner(textCampaignInfo, DOMAIN_2LEVEL_A_WITH_WWW);

        Assert.assertTrue(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Если есть РМП кампании, то промокод вводить нельзя
     */
    @Test
    public void mobileCampaign_notAvailable() {
        Campaign mobileCampaign = activeMobileAppCampaign(clientId, uid)
                .withBalanceInfo(emptyBalanceInfo(RUB));
        CampaignInfo campaignInfo = campaignSteps.createCampaign(mobileCampaign, clientInfo);

        addTextBanner(campaignInfo, DOMAIN_2LEVEL_A_WITH_WWW);

        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    @Test
    public void withoutCampaigns_notAvailable() {
        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    @Test
    public void withoutBanners_notAvailable() {
        campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);
        campaignSteps.createCampaign(emptyDynamicCampaign(clientId, uid), clientInfo);

        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Если домен третьего уровня на хостинге - промокод доступен
     */
    @Test
    public void withThirdLevelDomainOnHosting_available() {
        Campaign textCampaign = emptyTextCampaign(clientId, uid);
        CampaignInfo campaignInfo = campaignSteps.createCampaign(textCampaign, clientInfo);

        addTextBanner(campaignInfo, "www.mysite.wix.com");

        Assert.assertTrue(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Проверяем, что если объявления логина ведут на несколько разных доменов - промокод недоступен
     */
    @Test
    public void withTwoDifferentDomains2Level_notAvailable() {
        CampaignInfo campaignInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);

        addTextBanner(campaignInfo, DOMAIN_2LEVEL_A_WITH_WWW);
        addTextBanner(campaignInfo, DOMAIN_2LEVEL_B);

        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Проверяем, что если оплат не было, домена нет в статистике, но баннеры с доменом различаются наличием www,
     * промокод вводить можно
     */
    @Test
    public void withTwoSameDomains2Level_available() {
        CampaignInfo campaignInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);

        addTextBanner(campaignInfo, DOMAIN_2LEVEL_B);
        addTextBanner(campaignInfo, "www." + DOMAIN_2LEVEL_B);

        Assert.assertTrue(service.isPromocodeEntryAvailable(clientId));
    }

    @Test
    public void withTextAndDynamicBannersAndSameDomains2Level_available() {
        CampaignInfo textCampInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);
        CampaignInfo dynamicCampInfo = campaignSteps.createCampaign(emptyDynamicCampaign(clientId, uid), clientInfo);

        addTextBanner(textCampInfo, DOMAIN_2LEVEL_A_WITH_WWW);
        addDynamicBanner(dynamicCampInfo, DOMAIN_2LEVEL_A_WITH_WWW);

        Assert.assertTrue(service.isPromocodeEntryAvailable(clientId));
    }

    @Test
    public void withTextAndDynamicBannersAndDifferentDomains2Level_notAvailable() {
        CampaignInfo textCampaignInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);

        Campaign dynamicCampaign = emptyDynamicCampaign(clientId, uid);
        CampaignInfo dynamicCampaignInfo = campaignSteps.createCampaign(dynamicCampaign, clientInfo);

        addTextBanner(textCampaignInfo, DOMAIN_2LEVEL_A_WITH_WWW);
        addDynamicBanner(dynamicCampaignInfo, DOMAIN_2LEVEL_B);

        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Проверяем, что если оплат не было, домена нет в статистике, но он третьего уровня, промокод вводить нельзя
     */
    @Test
    public void with3LevelDomain_notAvailable() {
        CampaignInfo campaignInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);

        addTextBanner(campaignInfo, DOMAIN_3LEVEL);

        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    @Test
    public void with1LevelDomain_notAvailable() {
        CampaignInfo campaignInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);

        addTextBanner(campaignInfo, DOMAIN_1LEVEL);

        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Проверяем, что если есть нулевая статистика по домену, но в другой кодировке, промокод вводить можно
     */
    @Test
    public void domainInEncoding_available() {
        CampaignInfo campaignInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);

        addTextBanner(campaignInfo, DOMAIN_2LEVEL_C_ENCODED);

        Assert.assertTrue(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Проверяем, что если оплат не было, но домен есть в статистике, промокод вводить нельзя
     */
    @Test
    public void domainWithStat_notAvailable() {
        CampaignInfo campaignInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);

        addTextBanner(campaignInfo, DOMAIN_2LEVEL_A_WITH_WWW);

        addDomainStat(DOMAIN_2LEVEL_A_WITH_WWW, 1L);

        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Проверяем, что если есть статистика по домену, но в другой кодировке, промокод вводить нельзя
     */
    @Test
    public void domainInEncodingWithStat_notAvailable() {
        CampaignInfo campaignInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);

        addTextBanner(campaignInfo, DOMAIN_2LEVEL_C_ENCODED);

        addDomainStat(DOMAIN_2LEVEL_C_ENCODED, 1L);

        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Проверяем, что если оплат не было, а зеркало домена есть в статистике, промокод вводить нельзя
     */
    @Test
    public void domainWithStatInMirrorDomain_notAvailable() {
        CampaignInfo campaignInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);

        addTextBanner(campaignInfo, DOMAIN_2LEVEL_A_WITH_WWW);

        addMainMirror(DOMAIN_2LEVEL_A_WITH_WWW, DOMAIN_2LEVEL_B);
        addDomainStat(DOMAIN_2LEVEL_B, 1L);

        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    /**
     * Проверяем, что если оплат не было, а зеркало домена у ДО в статистике, промокод вводить нельзя
     */
    @Test
    public void dynamicCampaignDomainWithStatInMirrorDomain_notAvailable() {
        CampaignInfo dynamicCampInfo = campaignSteps.createCampaign(emptyDynamicCampaign(clientId, uid), clientInfo);

        addDynamicBanner(dynamicCampInfo, DOMAIN_2LEVEL_A_WITH_WWW);

        addMainMirror(DOMAIN_2LEVEL_A_WITH_WWW, DOMAIN_2LEVEL_B);
        addDomainStat(DOMAIN_2LEVEL_B, 1L);

        Assert.assertFalse(service.isPromocodeEntryAvailable(clientId));
    }

    @Test
    public void domainWithMirrorDomain_available() {
        CampaignInfo campaignInfo = campaignSteps.createCampaign(emptyTextCampaign(clientId, uid), clientInfo);

        addTextBanner(campaignInfo, DOMAIN_2LEVEL_A_WITH_WWW);

        addMainMirror(DOMAIN_2LEVEL_A_WITH_WWW, DOMAIN_3LEVEL);

        Assert.assertTrue(service.isPromocodeEntryAvailable(clientId));
    }

    private void addTextBanner(CampaignInfo campaignInfo, String domain) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId()), campaignInfo);
        steps.bannerSteps().createBanner(activeTextBanner()
                .withDomain(domain), adGroupInfo);
    }

    private void addDynamicBanner(CampaignInfo campaignInfo, String domain) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps()
                .createAdGroup(activeDynamicTextAdGroup(campaignInfo.getCampaignId())
                        .withDomainUrl(domain), campaignInfo);
        steps.bannerSteps().createActiveDynamicBanner(adGroupInfo);
    }
}
