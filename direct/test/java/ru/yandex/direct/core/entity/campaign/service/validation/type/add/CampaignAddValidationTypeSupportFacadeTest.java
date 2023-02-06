package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.add.AddServicedCampaignService;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.AddServicedCampaignInfo;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignAddValidationTypeSupportFacadeTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private NetAcl netAcl;

    @Mock
    private SspPlatformsRepository sspPlatformsRepository;

    @Mock
    private AddServicedCampaignService addServicedCampaignService;

    @Spy
    @InjectMocks
    private CommonCampaignAddValidationTypeSupport commonCampaignValidationTypeSupport;

    @Mock
    private FeatureService featureService;

    @Spy
    @InjectMocks
    private CampaignWithAllowedPageIdsAddValidationTypeSupport campaignValidationWithAllowedPageIds;

    private CampaignAddValidationTypeSupportFacade validationTypeSupportFacade;
    private Long operatorUid;
    private ClientId clientId;
    private CampaignValidationContainer container;

    private static Object[] parametrizedCampaignTypes() {
        return new Object[][]{
                {CampaignType.TEXT, true},
                {CampaignType.PERFORMANCE, false},
                {CampaignType.DYNAMIC, true},
                {CampaignType.MCBANNER, false},
                {CampaignType.MOBILE_CONTENT, true},
        };
    }

    @Before
    public void initTestData() {
        List<CampaignAddValidationTypeSupport<? extends BaseCampaign>> supports =
                List.of(commonCampaignValidationTypeSupport, campaignValidationWithAllowedPageIds);
        validationTypeSupportFacade = new CampaignAddValidationTypeSupportFacade(supports);

        operatorUid = RandomNumberUtils.nextPositiveLong();
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        container = CampaignValidationContainer.create(0, operatorUid, clientId);
    }


    @Test
    public void checkCallPreValidate_ForExpectedSupport() {
        var model = new ContentPromotionCampaign()
                .withId(RandomNumberUtils.nextPositiveLong());
        validationTypeSupportFacade.preValidate(container, new ValidationResult<>(List.of(model)));

        //noinspection unchecked
        verify(commonCampaignValidationTypeSupport)
                .preValidate(eq(container), any(ValidationResult.class));

        //noinspection unchecked
        verify(campaignValidationWithAllowedPageIds, times(0))
                .preValidate(eq(container), any(ValidationResult.class));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}, with support pageIds: {1}")
    public void checkCallPreValidate_ForAllSupports(CampaignType campaignType, boolean supportPageIds) {
        var model = newCampaignByCampaignType(campaignType);
        validationTypeSupportFacade.preValidate(container, new ValidationResult<>(List.of(model)));

        //noinspection unchecked
        verify(commonCampaignValidationTypeSupport)
                .preValidate(eq(container), any(ValidationResult.class));
        if (supportPageIds) {
            //noinspection unchecked
            verify(campaignValidationWithAllowedPageIds)
                    .preValidate(eq(container), any(ValidationResult.class));
        }
    }

    @Test
    public void checkCallValidate_ForExpectedSupport() {
        var model = new ContentPromotionCampaign()
                .withId(RandomNumberUtils.nextPositiveLong());
        when(addServicedCampaignService.getServicedInfoForNewCampaigns(container, List.of(model)))
                .thenReturn(List.of(new AddServicedCampaignInfo()));
        validationTypeSupportFacade.validate(container, new ValidationResult<>(List.of(model)));

        //noinspection unchecked
        verify(commonCampaignValidationTypeSupport).validate(eq(container), any(ValidationResult.class));

        //noinspection unchecked
        verify(campaignValidationWithAllowedPageIds, times(0))
                .validate(eq(container), any(ValidationResult.class));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void checkCallValidate_ForAllSupports(CampaignType campaignType, boolean supportPageIds) {
        var model = newCampaignByCampaignType(campaignType);
        when(addServicedCampaignService.getServicedInfoForNewCampaigns(container, List.of(model)))
                .thenReturn(List.of(new AddServicedCampaignInfo()));
        validationTypeSupportFacade.validate(container, new ValidationResult<>(List.of(model)));

        //noinspection unchecked
        verify(commonCampaignValidationTypeSupport).validate(eq(container), any(ValidationResult.class));
        if (supportPageIds) {
            //noinspection unchecked
            verify(campaignValidationWithAllowedPageIds).validate(eq(container), any(ValidationResult.class));
        }
    }

}
