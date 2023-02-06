package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.campaign.service.MailTextCreatorService;
import ru.yandex.direct.core.entity.campaign.service.type.add.AddServicedCampaignService;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class CommonCampaignUpdateOperationSupportEnrichTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MailTextCreatorService mailTextCreatorService;
    @Mock
    private AddServicedCampaignService addServicedCampaignService;
    @Mock
    private ClientService clientService;
    @Mock
    private CampaignService campaignService;
    @Mock
    private UserService userService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommonCampaignUpdateOperationSupport updateOperationSupport;

    private ModelChanges<CommonCampaign> modelChanges;
    private CommonCampaign campaignFromDb;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    private final RestrictedCampaignsUpdateOperationContainer container =
            RestrictedCampaignsUpdateOperationContainer.create(1,null,null,null,null);

    @Before
    public void initTestData() {
        long campaignId = RandomNumberUtils.nextPositiveLong();
        modelChanges = new ModelChanges<>(campaignId, CommonCampaign.class);

        campaignFromDb = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignId)
                .withHasTurboApp(false)
                .withEnableSendAccountNews(CampaignConstants.DEFAULT_ENABLE_SEND_ACCOUNT_NEWS)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withIsServiceRequested(false);
    }

    @Test
    public void enrich_whenEnableSendAccountNewsValueIsNull() {
        modelChanges.process(null, CommonCampaign.ENABLE_SEND_ACCOUNT_NEWS);

        AppliedChanges<CommonCampaign> appliedChanges = modelChanges.applyTo(campaignFromDb);
        updateOperationSupport.onChangesApplied(container, List.of(appliedChanges));
        assertThat(appliedChanges.changed(CommonCampaign.ENABLE_SEND_ACCOUNT_NEWS))
                .isFalse();
    }

    @Test
    public void enrich_whenWarningBalanceValueIsNull() {
        modelChanges.process(null, CommonCampaign.WARNING_BALANCE);

        AppliedChanges<CommonCampaign> appliedChanges = modelChanges.applyTo(campaignFromDb);

        updateOperationSupport.onChangesApplied(container, List.of(appliedChanges));
        assertThat(appliedChanges.changed(CommonCampaign.WARNING_BALANCE))
                .isFalse();
    }

    @Test
    public void enrich_whenHasTurboAppIsNull() {
        modelChanges.process(null, CommonCampaign.HAS_TURBO_APP);

        AppliedChanges<CommonCampaign> appliedChanges = modelChanges.applyTo(campaignFromDb);

        updateOperationSupport.onChangesApplied(container, List.of(appliedChanges));
        assertThat(appliedChanges.changed(CommonCampaign.HAS_TURBO_APP))
                .isFalse();
    }
}
