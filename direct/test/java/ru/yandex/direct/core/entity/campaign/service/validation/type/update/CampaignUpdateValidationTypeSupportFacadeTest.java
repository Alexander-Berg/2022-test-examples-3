package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.type.add.AddServicedCampaignService;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.AddServicedCampaignInfo;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignUpdateValidationTypeSupportFacadeTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private NetAcl netAcl;

    @Mock
    private AddServicedCampaignService addServicedCampaignService;

    @Mock
    private SspPlatformsRepository sspPlatformsRepository;

    @Spy
    @InjectMocks
    private CommonCampaignUpdateValidationTypeSupport commonCampaignValidationTypeSupport;

    @Mock
    private FeatureService featureService;

    @Spy
    @InjectMocks
    private CampaignWithAllowedPageIdsUpdateValidationTypeSupport withAllowedPageIdsUpdateValidationTypeSupport;

    private CampaignUpdateValidationTypeSupportFacade validationTypeSupportFacade;
    private Long operatorUid;
    private ClientId clientId;
    private RestrictedCampaignsUpdateOperationContainerImpl container;

    private static Object[] parametrizedCampaignTypes() {
        return new Object[][]{
                {CampaignType.TEXT, true},
                {CampaignType.PERFORMANCE, true},
                {CampaignType.DYNAMIC, true},
                {CampaignType.MCBANNER, false},
                {CampaignType.MOBILE_CONTENT, true},
        };
    }

    @Before
    public void initTestData() {
        List<CampaignUpdateValidationTypeSupport<? extends BaseCampaign>> supports =
                List.of(commonCampaignValidationTypeSupport, withAllowedPageIdsUpdateValidationTypeSupport);
        validationTypeSupportFacade = new CampaignUpdateValidationTypeSupportFacade(supports);

        operatorUid = RandomNumberUtils.nextPositiveLong();
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        container = new RestrictedCampaignsUpdateOperationContainerImpl(0, operatorUid, clientId, null,
                null, null, new CampaignOptions(), null, emptyMap());
        when(addServicedCampaignService.getServicedInfoForClientCampaigns(any(CampaignValidationContainer.class)))
                .thenReturn(new AddServicedCampaignInfo().withIsServiced(false));
    }


    @Test
    public void checkCallPreValidate_ForExpectedSupport() {
        long campaignId = RandomNumberUtils.nextPositiveLong();
        var modelChanges = new ModelChanges<>(campaignId, commonCampaignValidationTypeSupport.getTypeClass());
        container.setCampaignType(campaignId, CampaignType.MCBANNER);
        validationTypeSupportFacade.preValidate(container, new ValidationResult<>(List.of(modelChanges)));

        //noinspection unchecked
        verify(commonCampaignValidationTypeSupport)
                .preValidate(eq(container), any(ValidationResult.class));

        //noinspection unchecked
        verify(withAllowedPageIdsUpdateValidationTypeSupport, times(0))
                .preValidate(eq(container), any(ValidationResult.class));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}, with support pageIds: {1}")
    public void checkCallPreValidate_ForAllSupports(CampaignType campaignType, boolean supportPageIds) {
        long campaignId = RandomNumberUtils.nextPositiveLong();
        var modelChanges = new ModelChanges<>(campaignId, TextCampaign.class);
        container.setCampaignType(campaignId, CampaignType.TEXT);
        validationTypeSupportFacade.preValidate(container, new ValidationResult<>(List.of(modelChanges)));

        //noinspection unchecked
        verify(commonCampaignValidationTypeSupport)
                .preValidate(eq(container), any(ValidationResult.class));
        if (supportPageIds) {
            //noinspection unchecked
            verify(withAllowedPageIdsUpdateValidationTypeSupport)
                    .preValidate(eq(container), any(ValidationResult.class));
        }
    }

    @Test
    public void checkCallValidateBeforeApply_ForExpectedSupport() {
        long campaignId = RandomNumberUtils.nextPositiveLong();
        var modelChanges = new ModelChanges<>(campaignId, commonCampaignValidationTypeSupport.getTypeClass());
        container.setCampaignType(campaignId, CampaignType.MCBANNER);
        Map<Long, BaseCampaign> unmodifiedModels = Collections.emptyMap();
        validationTypeSupportFacade.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChanges)), unmodifiedModels);

        //noinspection unchecked
        verify(commonCampaignValidationTypeSupport)
                .validateBeforeApply(eq(container), any(ValidationResult.class),
                        eq(Collections.emptyMap()));

        //noinspection unchecked
        verify(withAllowedPageIdsUpdateValidationTypeSupport, times(0))
                .validateBeforeApply(eq(container), any(ValidationResult.class),
                        eq(Collections.emptyMap()));
    }

    @Test
    public void checkCallValidateBeforeApply_ForAllSupports() {
        long campaignId = RandomNumberUtils.nextPositiveLong();
        var modelChanges = new ModelChanges<>(campaignId, TextCampaign.class);
        container.setCampaignType(campaignId, CampaignType.TEXT);
        Map<Long, BaseCampaign> unmodifiedModels = Collections.emptyMap();
        validationTypeSupportFacade.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChanges)), unmodifiedModels);

        //noinspection unchecked
        verify(commonCampaignValidationTypeSupport)
                .validateBeforeApply(eq(container), any(ValidationResult.class),
                        eq(Collections.emptyMap()));
        //noinspection unchecked
        verify(withAllowedPageIdsUpdateValidationTypeSupport)
                .validateBeforeApply(eq(container), any(ValidationResult.class),
                        eq(Collections.emptyMap()));
    }

    @Test
    public void checkCallValidate_ForExpectedSupport() {
        var model = new ContentPromotionCampaign()
                .withId(RandomNumberUtils.nextPositiveLong());
        validationTypeSupportFacade.validate(container, new ValidationResult<>(List.of(model)),
                Map.of(0, new ModelChanges<>(model.getId(), ContentPromotionCampaign.class).applyTo(model)));

        //noinspection unchecked
        verify(commonCampaignValidationTypeSupport)
                .validate(eq(container), any(ValidationResult.class));

        //noinspection unchecked
        verify(withAllowedPageIdsUpdateValidationTypeSupport, times(0))
                .validate(eq(container), any(ValidationResult.class));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void checkCallValidate_ForAllSupports(CampaignType campaignType, boolean supportPageIds) {
        BaseCampaign model = newCampaignByCampaignType(campaignType)
                .withId(RandomNumberUtils.nextPositiveLong());
        validationTypeSupportFacade.validate(container, new ValidationResult<>(List.of(model)),
                Map.of(0, new ModelChanges<>(model.getId(), BaseCampaign.class).applyTo(model)));

        //noinspection unchecked
        verify(commonCampaignValidationTypeSupport)
                .validate(eq(container), any(ValidationResult.class));
        if (supportPageIds) {
            //noinspection unchecked
            verify(withAllowedPageIdsUpdateValidationTypeSupport)
                    .validate(eq(container), any(ValidationResult.class));
        }
    }

}
