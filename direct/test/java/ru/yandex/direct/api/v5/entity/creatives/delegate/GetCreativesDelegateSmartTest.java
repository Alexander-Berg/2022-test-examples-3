package ru.yandex.direct.api.v5.entity.creatives.delegate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.creatives.BusinessTypeEnum;
import com.yandex.direct.api.v5.creatives.CreativeFieldEnum;
import com.yandex.direct.api.v5.creatives.CreativeGetItem;
import com.yandex.direct.api.v5.creatives.GetRequest;
import com.yandex.direct.api.v5.creatives.GetResponse;
import com.yandex.direct.api.v5.creatives.ObjectFactory;
import com.yandex.direct.api.v5.creatives.SmartCreativeFieldEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.creatives.converter.GetResponseConverter;
import ru.yandex.direct.api.v5.entity.creatives.validation.GetCreativesValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.client.service.checker.ClientAccessCheckerTypeSupportFacade;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeBusinessType;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceBannerInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@ParametersAreNonnullByDefault
@Api5Test
@RunWith(SpringRunner.class)
public class GetCreativesDelegateSmartTest {

    private static final ObjectFactory FACTORY = new ObjectFactory();
    @Autowired
    private Steps steps;
    @Autowired
    private GetCreativesValidationService validationService;
    @Autowired
    private GetResponseConverter getResponseConverter;
    @Autowired
    private CreativeRepository creativeRepository;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private ClientAccessCheckerTypeSupportFacade clientAccessCheckerTypeSupportFacade;

    private GenericApiService genericApiService;
    private GetCreativesDelegate delegate;

    private Long creativeId;
    private Creative creative;

    private static GetRequest createRequest(Long creativeId) {
        return new GetRequest()
                .withSelectionCriteria(
                        FACTORY.createCreativesSelectionCriteria()
                                .withIds(singleton(creativeId)))
                .withFieldNames(CreativeFieldEnum.values())
                .withSmartCreativeFieldNames(SmartCreativeFieldEnum.values());
    }

    @Before
    public void before() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        var bannerInfo = steps.performanceBannerSteps().createPerformanceBanner(
                new NewPerformanceBannerInfo()
                .withAdGroupInfo(adGroupInfo));
        ClientInfo clientInfo = adGroupInfo.getClientInfo();
        Long uid = adGroupInfo.getUid();
        ClientId clientId = clientInfo.getClientId();
        creativeId = bannerInfo.getCreativeId();
        creative = bannerInfo.getCreative();

        ApiUser user = new ApiUser()
                .withUid(uid)
                .withClientId(clientId);
        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(user);
        when(auth.getChiefSubclient()).thenReturn(user);
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));
        delegate = new GetCreativesDelegate(auth, validationService, getResponseConverter, creativeRepository,
                shardHelper, clientAccessCheckerTypeSupportFacade);
    }

    @Test
    public void get_checkResultFormat_success() {
        checkState(creative.getBusinessType() == CreativeBusinessType.RETAIL, "unexpected BusinessType");

        CreativeGetItem expectedItem = FACTORY.createCreativeGetItem()
                .withId(creativeId)
                .withName(creative.getName())
                .withSmartCreative(
                        FACTORY.createSmartCreativeGet()
                                .withCreativeGroupId(creative.getCreativeGroupId())
                                .withCreativeGroupName(creative.getGroupName())
                                .withBusinessType(BusinessTypeEnum.RETAIL));

        GetRequest request = createRequest(creativeId);
        GetResponse response = genericApiService.doAction(delegate, request);
        CreativeGetItem actualItem = response.getCreatives().get(0);
        assertThat(actualItem).is(matchedBy(beanDiffer(expectedItem).useCompareStrategy(onlyExpectedFields())));
    }

}
