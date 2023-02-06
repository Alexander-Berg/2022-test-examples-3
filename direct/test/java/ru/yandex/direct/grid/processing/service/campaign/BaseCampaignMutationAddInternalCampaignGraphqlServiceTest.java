package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoals;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddAbstractInternalCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.rbac.ClientPerm;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toSmsFlags;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toTimeInterval;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
public class BaseCampaignMutationAddInternalCampaignGraphqlServiceTest {

    private static final String MUTATION_NAME = "addCampaigns";
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
            + "    addedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final TemplateMutation<GdAddCampaigns, GdAddCampaignPayload> ADD_CAMPAIGN_MUTATION =
            new TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdAddCampaigns.class, GdAddCampaignPayload.class);

    protected static final DefaultCompareStrategy CAMPAIGN_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath(CommonCampaign.CREATE_TIME.name()),
                    newPath(CampaignWithMeaningfulGoals.RAW_MEANINGFUL_GOALS.name()),
                    newPath(CommonCampaign.AUTOBUDGET_FORECAST_DATE.name()),
                    newPath(CommonCampaign.TIME_TARGET.name() + "/originalTimeTarget"),
                    newPath(CommonCampaign.LAST_CHANGE.name()))
            .forFields(newPath(CommonCampaign.ID.name())).useMatcher(notNullValue())
            .forFields(newPath(CampaignWithPackageStrategy.STRATEGY_ID.name())).useMatcher(notNullValue())
            .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer())
            .forFields(newPath(CommonCampaign.SOURCE.name())).useMatcher(equalTo(CampaignSource.DIRECT))
            .forFields(newPath(CommonCampaign.METATYPE.name())).useMatcher(equalTo(CampaignMetatype.DEFAULT_));

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    private Steps steps;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    protected User operator;
    protected ClientInfo clientInfo;
    protected User subjectUser;

    @SuppressWarnings("ConstantConditions")
    @Before
    public void setUp() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        subjectUser = clientInfo.getChiefUserInfo().getUser()
                .withPerms(EnumSet.of(ClientPerm.INTERNAL_AD_PRODUCT));

        var operatorClientInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.INTERNAL_AD_ADMIN);
        operator = operatorClientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator, subjectUser);
    }

    protected GdAddCampaignPayload runRequest(GdAddCampaignUnion addCampaignUnion) {
        var input = new GdAddCampaigns()
                .withCampaignAddItems(List.of(addCampaignUnion));
        var gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator, subjectUser);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);
        return gdAddCampaignPayload;
    }

    protected GdAddCampaignPayload runRequestWithValidationErrors(GdAddCampaignUnion addCampaignUnion) {
        var input = new GdAddCampaigns()
                .withCampaignAddItems(List.of(addCampaignUnion));
        var gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator, subjectUser);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNotNull();
        return gdAddCampaignPayload;
    }

    protected <T extends BaseCampaign> T fetchSingleCampaignFromDb(GdAddCampaignPayload gdAddCampaignPayload) {
        var campaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                mapList(gdAddCampaignPayload.getAddedCampaigns(), GdAddCampaignPayloadItem::getId));

        assertThat(campaigns).hasSize(1);
        //noinspection unchecked
        return (T) campaigns.get(0);
    }

    /**
     * Метод создает модель кампании внутренней рекламы и заполняет CommonCampaign поля значениями из запроса
     */
    protected <T extends CommonCampaign> T getCommonExpectedCampaign(GdAddAbstractInternalCampaign request,
                                                                     Supplier<T> modelSupplier) {
        var smsSettings = request.getNotification().getSmsSettings();
        var emailSettings = request.getNotification().getEmailSettings();

        //noinspection unchecked
        T campaign = (T) modelSupplier.get()
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

                .withAgencyId(0L)
                .withOrderId(0L)
                .withWalletId(0L)
                .withUid(subjectUser.getUid())
                .withClientId(clientInfo.getClientId().asLong())
                .withFio(operator.getFio())
                .withCurrency(CurrencyCode.RUB)

                .withStatusActive(false)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusEmpty(false)
                .withStatusModerate(CampaignStatusModerate.READY)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withStatusShow(false)
                .withStatusArchived(false)
                .withSumToPay(BigDecimal.ZERO)
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO)
                .withSumLast(BigDecimal.ZERO)
                .withHasTurboApp(false)
                .withIsVirtual(false)
                .withPaidByCertificate(false)
                .withEnableCompanyInfo(false)
                .withEnableCpcHold(false)
                .withHasExtendedGeoTargeting(false)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasTitleSubstitution(false)
                .withIsServiceRequested(false);

        if (campaign instanceof CampaignWithPackageStrategy) {
            ((CampaignWithPackageStrategy) campaign).setStrategyId(0L);
        }
        return campaign;
    }

}
