package ru.yandex.direct.web.entity.adgroup.controller.cpm;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyYndFixed;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.adgroup.controller.CpmAdGroupController;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;
import ru.yandex.direct.web.validation.model.ValidationResponse;
import ru.yandex.direct.web.validation.model.WebDefect;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmYndxFrontpageCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueCpmNotLessThan;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.web.testing.data.TestAdGroups.defaultCpmAdGroupRetargeting;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmYndxFrontpageAdGroup;


public class CpmAdGroupControllerAddFrontpagePricesTestBase {
    private static final String DEFECT_ID_CODE =
            invalidValueCpmNotLessThan(Money.valueOf(.0, CurrencyCode.RUB)).defectId().getCode();

    @Parameterized.Parameter
    public Collection<FrontpageCampaignShowType> frontpageCampaignShowTypes;

    @Parameterized.Parameter(1)
    public Double retargetingPrice;

    @Parameterized.Parameter(2)
    public String geo;

    @Parameterized.Parameter(3)
    public Boolean hasError;

    @Parameterized.Parameter(4)
    public String description;

    @Autowired
    private Steps steps;

    @Autowired
    private CpmAdGroupController controller;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    @Autowired
    private DirectWebAuthenticationSource authenticationSource;

    private ClientInfo clientInfo;
    private Currency clientCurrency;

    @Before
    public void before() throws Exception {
        new TestContextManager(this.getClass()).prepareTestInstance(this);
        clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.CHF));
        clientCurrency = clientService.getWorkCurrency(clientInfo.getClientId());
        testCpmYndxFrontpageRepository.fillMinBidsTestValues();
        setAuthData();
    }

    protected void testPriceWithGeoTargetingAdd() {
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmYndxFrontpageCampaign(clientInfo.getClientId(), clientInfo.getUid()), clientInfo);
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                clientInfo.getShard(),
                campaignInfo.getCampaignId(),
                frontpageCampaignShowTypes);
        WebCpmAdGroup requestAdGroup = randomNameWebCpmYndxFrontpageAdGroup(null, campaignInfo.getCampaignId())
                .withGeneralPrice(null)
                .withGeo(geo);
        WebCpmAdGroupRetargeting retargeting = defaultCpmAdGroupRetargeting()
                .withGroups(emptyList())
                .withPriceContext(retargetingPrice);
        requestAdGroup = requestAdGroup.withRetargetings(singletonList(retargeting));

        checkValidation(requestAdGroup, campaignInfo.getCampaignId(), true);
    }

    protected void testPriceWithGeoTargetingUpdate() {
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmYndxFrontpageCampaign(clientInfo.getClientId(), clientInfo.getUid()), clientInfo);
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                clientInfo.getShard(),
                campaignInfo.getCampaignId(),
                frontpageCampaignShowTypes);
        WebCpmAdGroup requestAdGroupForAdd = randomNameWebCpmYndxFrontpageAdGroup(null, campaignInfo.getCampaignId())
                .withGeneralPrice(null);
        WebCpmAdGroupRetargeting retargetingForAdd = defaultCpmAdGroupRetargeting()
                .withGroups(emptyList())
                .withPriceContext(CurrencyYndFixed.getInstance().getMinCpmPrice().doubleValue());
        requestAdGroupForAdd = requestAdGroupForAdd.withRetargetings(singletonList(retargetingForAdd));

        controller.saveCpmAdGroup(singletonList(requestAdGroupForAdd), campaignInfo.getCampaignId(),
                true, false, false, null);


        Long adAgroupId = findAdGroups(campaignInfo.getCampaignId()).get(0).getId();

        WebCpmAdGroup requestAdGroupForUpdate =
                randomNameWebCpmYndxFrontpageAdGroup(adAgroupId, campaignInfo.getCampaignId())
                        .withGeneralPrice(null)
                        .withGeo(geo);

        Retargeting savedRetargeting = findRetargetings(adAgroupId).get(0);
        WebCpmAdGroupRetargeting retargetingForUpdate = defaultCpmAdGroupRetargeting()
                .withGroups(emptyList())
                .withPriceContext(retargetingPrice)
                .withId(savedRetargeting.getId())
                .withRetargetingConditionId(savedRetargeting.getRetargetingConditionId());
        requestAdGroupForUpdate = requestAdGroupForUpdate.withRetargetings(singletonList(retargetingForUpdate));

        checkValidation(requestAdGroupForUpdate, campaignInfo.getCampaignId(), false);
    }

    private void checkValidation(WebCpmAdGroup requestAdGroup, Long campaignId, Boolean isNewAdGroups) {
        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(requestAdGroup), campaignId, isNewAdGroups, false, false, null);
        if (!hasError) {
            assertThat(webResponse.isSuccessful(), equalTo(true));
            List<AdGroup> adGroups = findAdGroups(campaignId);
            assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
            Long adGroupId = adGroups.get(0).getId();
            List<Retargeting> retargetings = findRetargetings(adGroupId);
            assertThat("должен быть добавлен один ретаргетинг", retargetings, hasSize(1));
            assertThat("Цена ретаргетинга совпадает с ожидаемой",
                    Money.valueOf(retargetings.get(0).getPriceContext(), clientCurrency.getCode()),
                    equalTo(Money.valueOf(retargetingPrice, clientCurrency.getCode())));
        } else {
            assertThat(webResponse.isSuccessful(), equalTo(false));
            WebValidationResult vr = ((ValidationResponse) webResponse).validationResult();
            assertThat(vr.getErrors().isEmpty(), equalTo(false));
            assertThat(mapList(vr.getErrors(), WebDefect::getCode).contains(DEFECT_ID_CODE), equalTo(true));
        }
    }

    private List<AdGroup> findAdGroups(Long campaignId) {
        AdGroupsSelectionCriteria criteria = new AdGroupsSelectionCriteria().withCampaignIds(campaignId);
        List<Long> adGroupIds = adGroupRepository
                .getAdGroupIdsBySelectionCriteria(clientInfo.getShard(), criteria, LimitOffset.maxLimited());
        return adGroupRepository.getAdGroups(clientInfo.getShard(), adGroupIds);
    }

    private List<Retargeting> findRetargetings(Long adGroupId) {
        return retargetingRepository.getRetargetingsByAdGroups(clientInfo.getShard(), singletonList(adGroupId));
    }

    protected void setAuthData() {
        DirectWebAuthenticationSourceMock authSource =
                (DirectWebAuthenticationSourceMock) authenticationSource;
        authSource.withOperator(new User()
                .withUid(clientInfo.getUid()));
        authSource.withSubjectUser(new User()
                .withClientId(clientInfo.getClientId())
                .withUid(clientInfo.getUid()));

        User user = clientInfo.getChiefUserInfo().getUser();
        SecurityContextHolder.getContext()
                .setAuthentication(new DirectAuthentication(user, user));
    }
}
