package ru.yandex.direct.intapi.entity.balanceclient.controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.TemporalUnitOffset;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.promocodes.model.CampPromocodes;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo;
import ru.yandex.direct.core.entity.promocodes.repository.CampPromocodesRepository;
import ru.yandex.direct.core.testing.MockMvcCreator;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.BalanceInfo;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants;
import ru.yandex.misc.lang.StringUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NotifyPromocodeTest {
    private static final CompareStrategy EXCEPT_LAST_CHANGE = allFieldsExcept(newPath("lastChange"));
    private static final TemporalUnitOffset LASTCHANGE_OFFSET = new TemporalUnitWithinOffset(15, ChronoUnit.MINUTES);

    private static final String DOMAIN = "test.com";
    private static final PromocodeInfo PROMOCODE_INFO = new PromocodeInfo()
            .withId(1160L)
            .withCode("WD63YD")
            .withInvoiceId(4271165L)
            .withInvoiceExternalId("Б-5126650-1")
            .withForNewClientsOnly(false)
            .withInvoiceEnabledAt(LocalDateTime.of(2037, 7, 31, 11, 55, 8));


    @Autowired
    private BalanceClientController controller;

    @Autowired
    private MockMvcCreator mockMvcCreator;

    @Autowired
    private CampPromocodesRepository campPromocodesRepository;

    @Autowired
    private Steps steps;

    private MockMvc mockMvc;
    private MockHttpServletRequestBuilder requestBuilder;
    private CampaignInfo textCampaignUnderWallet;
    private CampaignInfo walletWithTextCampaign;
    private CampaignInfo textCampaignNoWallet;
    private CampaignInfo dynamicCampaignUnderWallet;
    private CampaignInfo walletWithDynamicCampaign;
    private CampaignInfo dynamicCampaignNoWallet;

    @Before
    public void buildMockMvc() {
        mockMvc = mockMvcCreator.setup(controller).build();
        requestBuilder = post(BalanceClientServiceConstants.NOTIFY_PROMOCODE_PREFIX)
                .contentType(MediaType.APPLICATION_JSON);
    }

    @Before
    public void createTestDataText() {
        walletWithTextCampaign = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));

        BalanceInfo balanceInfo =
                activeBalanceInfo(CurrencyCode.RUB).withWalletCid(walletWithTextCampaign.getCampaignId());
        textCampaignUnderWallet = steps.campaignSteps().createCampaign(
                newTextCampaign(walletWithTextCampaign.getClientId(), walletWithTextCampaign.getUid())
                        .withBalanceInfo(balanceInfo),
                walletWithTextCampaign.getClientInfo());
        addTextBanners(textCampaignUnderWallet);

        textCampaignNoWallet = steps.campaignSteps().createActiveTextCampaign();
        addTextBanners(textCampaignNoWallet);
    }

    private void addTextBanners(CampaignInfo textCampaign) {
        steps.bannerSteps().createBanner(
                activeTextBanner()
                        .withHref("http://" + DOMAIN)
                        .withDomain(DOMAIN)
                        .withReverseDomain(StringUtils.reverse(DOMAIN))
                        .withStatusBsSynced(StatusBsSynced.YES),
                textCampaign);

        String domainVariant1 = "TEST.com";
        steps.bannerSteps().createBanner(
                activeTextBanner()
                        .withHref("http://" + domainVariant1)
                        .withDomain(domainVariant1)
                        .withReverseDomain(StringUtils.reverse(domainVariant1))
                        .withStatusBsSynced(StatusBsSynced.YES),
                textCampaign);

        String domainVariant2 = "tEsT.com";
        steps.bannerSteps().createBanner(
                activeTextBanner()
                        .withHref("http://" + domainVariant2)
                        .withDomain(domainVariant2)
                        .withReverseDomain(StringUtils.reverse(domainVariant2))
                        .withStatusBsSynced(StatusBsSynced.YES),
                textCampaign);
    }

    @Before
    public void createTestDataDynamic() {
        walletWithDynamicCampaign = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));

        BalanceInfo balanceInfo = activeBalanceInfo(CurrencyCode.RUB)
                .withWalletCid(walletWithDynamicCampaign.getCampaignId());

        dynamicCampaignUnderWallet = steps.campaignSteps().createCampaign(
                activeDynamicCampaign(walletWithDynamicCampaign.getClientId(), walletWithDynamicCampaign.getUid())
                        .withBalanceInfo(balanceInfo),
                walletWithDynamicCampaign.getClientInfo());
        addDynamicBanners(dynamicCampaignUnderWallet);

        dynamicCampaignNoWallet = steps.campaignSteps().createActiveDynamicCampaign();
        addDynamicBanners(dynamicCampaignNoWallet);
    }

    private void addDynamicBanners(CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(
                activeDynamicTextAdGroup(campaignInfo.getCampaignId()).withDomainUrl(DOMAIN),
                campaignInfo);
        steps.bannerSteps().createActiveDynamicBanner(adGroupInfo);
    }

    private void testAddCampaignPromocodes(CampaignInfo campaignInfo) throws Exception {
        makePromocodeRequest(campaignInfo);

        CampPromocodes expected = new CampPromocodes()
                .withCampaignId(campaignInfo.getCampaignId())
                .withRestrictedDomain(DOMAIN)
                .withPromocodes(singletonList(PROMOCODE_INFO));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk());

        CampPromocodes actual =
                campPromocodesRepository.getCampaignPromocodes(campaignInfo.getShard(), campaignInfo.getCampaignId());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(actual).is(matchedBy(beanDiffer(expected).useCompareStrategy(EXCEPT_LAST_CHANGE)));
        if (actual != null) {
            soft.assertThat(actual.getLastChange()).isCloseTo(LocalDateTime.now(), LASTCHANGE_OFFSET);
        }
        soft.assertAll();
    }

    private void testIgnoreCampaignPromocodes(CampaignInfo campaignInfo) throws Exception {
        makePromocodeRequest(campaignInfo);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk());

        assertNull("У кампании не появилось промокодов", campPromocodesRepository
                .getCampaignPromocodes(campaignInfo.getShard(), campaignInfo.getCampaignId()));
    }

    private void makePromocodeRequest(CampaignInfo campaignInfo) {
        requestBuilder.content("[{"
                + "\"ServiceID\": 7,"
                + "\"ServiceOrderID\": " + campaignInfo.getCampaignId() + ","
                + "\"Promocodes\": [{"
                + "     \"PromocodeID\": 1160,"
                + "     \"InvoiceID\": 4271165,"
                + "     \"PromocodeCode\": \"WD63YD\","
                + "     \"IsGlobalUnique\": false,"
                + "     \"InvoiceEID\": \"\\u0411-5126650-1\","
                + "     \"NeedUniqueUrls\": true,"
                + "     \"UnusedPromocodeQty\": \"0.253162\","
                + "     \"AvailablePromocodeQty\": \"0.253162\","
                + "     \"AnySameSeries\": false,"
                + "     \"NewClientsOnly\": false,"
                + "     \"StartDt\": \"2037-07-31T11:55:08\""
                + "     }]"
                + "}]");
    }

    @Test
    public void testAddCampaignPromocodesForTextCampaign() throws Exception {
        testAddCampaignPromocodes(textCampaignNoWallet);
    }

    @Test
    public void testAddCampaignPromocodesForWalletWithTextCampaign() throws Exception {
        testAddCampaignPromocodes(walletWithTextCampaign);
    }

    @Test
    public void testAddCampaignPromocodesForDynamicCampaign() throws Exception {
        testAddCampaignPromocodes(dynamicCampaignNoWallet);
    }

    @Test
    public void testAddCampaignPromocodesForWalletWithDynamicCampaign() throws Exception {
        testAddCampaignPromocodes(walletWithDynamicCampaign);
    }

    @Test
    public void testNoCampaignPromocodesForTextCampaignUnderWallet() throws Exception {
        testIgnoreCampaignPromocodes(textCampaignUnderWallet);
    }

    @Test
    public void testNoCampaignPromocodesForDynamicCampaignUnderWallet() throws Exception {
        testIgnoreCampaignPromocodes(dynamicCampaignUnderWallet);
    }

    @Test
    public void testRemoveCampaignPromocodes() throws Exception {
        CampPromocodes old = new CampPromocodes()
                .withCampaignId(textCampaignNoWallet.getCampaignId())
                .withRestrictedDomain(DOMAIN)
                .withPromocodes(emptyList())
                .withLastChange(LocalDateTime.now());
        campPromocodesRepository.addCampaignPromocodes(textCampaignNoWallet.getShard(), old);

        requestBuilder.content("[{"
                + "\"ServiceID\": 7,"
                + "\"ServiceOrderID\": " + textCampaignNoWallet.getCampaignId() + ","
                + "\"Promocodes\": []"
                + "}]");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk());

        assertNull(campPromocodesRepository
                .getCampaignPromocodes(textCampaignNoWallet.getShard(), textCampaignNoWallet.getCampaignId()));
    }

    @Test
    public void testValidation() throws Exception {
        requestBuilder.content("[{"
                + "\"ServiceID\": 99999,"
                + "\"ServiceOrderID\": 0,"
                + "\"Promocodes\": []"
                + "}]");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest());
    }
}
