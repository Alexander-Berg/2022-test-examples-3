package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoals;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TypedCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateAbstractInternalCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.rbac.ClientPerm;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toSmsFlags;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toTimeInterval;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
abstract class BaseCampaignMutationUpdateInternalCampaignGraphqlServiceTest {

    private static final String MUTATION_NAME = "updateCampaigns";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaigns, GdUpdateCampaignPayload>
            UPDATE_CAMPAIGN_MUTATION = new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
            GdUpdateCampaigns.class, GdUpdateCampaignPayload.class);

    protected static final DefaultCompareStrategy CAMPAIGN_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath(CommonCampaign.CREATE_TIME.name()),
                    newPath(CommonCampaign.AUTOBUDGET_FORECAST_DATE.name()),
                    newPath(CampaignWithMeaningfulGoals.RAW_MEANINGFUL_GOALS.name()),
                    newPath(CommonCampaign.SOURCE.name()),
                    newPath(CommonCampaign.METATYPE.name()),
                    newPath(CommonCampaign.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED.name()),
                    newPath(CommonCampaign.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED.name()),
                    newPath(CommonCampaign.TIME_TARGET.name() + "/originalTimeTarget"),
                    newPath(CommonCampaign.LAST_CHANGE.name()))
            .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer());

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    protected Steps steps;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    protected User operator;
    protected ClientInfo clientInfo;
    protected User subjectUser;

    protected TypedCampaignInfo campaignInfo;

    @Before
    public void setUp() {
        campaignInfo = createTypedCampaign();
        clientInfo = campaignInfo.getClientInfo();

        var operatorClientInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.INTERNAL_AD_ADMIN);
        operator = operatorClientInfo.getChiefUserInfo().getUser();
        subjectUser = clientInfo.getChiefUserInfo().getUser()
                .withPerms(EnumSet.of(ClientPerm.INTERNAL_AD_PRODUCT));
        TestAuthHelper.setDirectAuthentication(operator, subjectUser);
    }

    abstract TypedCampaignInfo createTypedCampaign();

    protected GdUpdateCampaignPayload runRequest(GdUpdateCampaignUnion apdateCampaignUnion) {
        var input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(apdateCampaignUnion));
        var gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator, subjectUser);

        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNull();
        assertThat(gdUpdateCampaignPayload.getUpdatedCampaigns()).hasSize(1);
        return gdUpdateCampaignPayload;
    }

    protected GdUpdateCampaignPayload runRequestWithValidationErrors(GdUpdateCampaignUnion apdateCampaignUnion) {
        var input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(apdateCampaignUnion));
        var gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION,
                input, operator, subjectUser);

        assertThat(gdUpdateCampaignPayload.getValidationResult()).isNotNull();
        return gdUpdateCampaignPayload;
    }

    protected <T extends BaseCampaign> T fetchSingleCampaignFromDb(GdUpdateCampaignPayload gdUpdateCampaignPayload) {
        var campaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                mapList(gdUpdateCampaignPayload.getUpdatedCampaigns(), GdUpdateCampaignPayloadItem::getId));

        assertThat(campaigns).hasSize(1);
        //noinspection unchecked
        return (T) campaigns.get(0);
    }

    /**
     * Метод заполняет для ожидаеомой модели кампании поля из CommonCampaign значениями из запроса
     */
    protected <T extends CommonCampaign> void fillCommonExpectedCampaign(T campaign,
                                                                         GdUpdateAbstractInternalCampaign request) {
        var smsSettings = request.getNotification().getSmsSettings();
        var emailSettings = request.getNotification().getEmailSettings();

        campaign
                .withName(request.getName())
                .withStartDate(request.getStartDate())
                .withEndDate(request.getEndDate())
                .withDisabledIps(request.getDisabledIps())
                .withSmsTime(toTimeInterval(smsSettings.getSmsTime()))
                .withSmsFlags(toSmsFlags(smsSettings.getEnableEvents()))
                .withEmail(emailSettings.getEmail())
                .withWarningBalance(emailSettings.getWarningBalance())
                .withEnableSendAccountNews(emailSettings.getSendAccountNews())
                .withEnablePausedByDayBudgetEvent(emailSettings.getStopByReachDailyBudget())
                .withTimeTarget(ifNotNull(request.getTimeTarget(), CampaignDataConverter::toTimeTarget))
                .withTimeZoneId(ifNotNull(request.getTimeTarget(), GdTimeTarget::getIdTimeZone))

                .withStatusBsSynced(CampaignStatusBsSynced.NO);
    }

}
