package ru.yandex.autotests.direct.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.autotests.direct.cmd.steps.ClientSteps;
import ru.yandex.autotests.direct.cmd.steps.TopLevelSteps;
import ru.yandex.autotests.direct.cmd.steps.auth.AuthSteps;
import ru.yandex.autotests.direct.cmd.steps.auth.PassportSteps;
import ru.yandex.autotests.direct.cmd.steps.user.UserExternalServicesSteps;
import ru.yandex.autotests.direct.cmd.steps.autocorrection.AjaxDisableAutocorrectionWarningSteps;
import ru.yandex.autotests.direct.cmd.steps.autopayment.AutopaySettingsSteps;
import ru.yandex.autotests.direct.cmd.steps.banners.BannerSteps;
import ru.yandex.autotests.direct.cmd.steps.banners.BannersAdditionsSteps;
import ru.yandex.autotests.direct.cmd.steps.banners.SearchBannersSteps;
import ru.yandex.autotests.direct.cmd.steps.banners.StopResumeBannerSteps;
import ru.yandex.autotests.direct.cmd.steps.base.DirectStepsContext;
import ru.yandex.autotests.direct.cmd.steps.campaings.AjaxCampSteps;
import ru.yandex.autotests.direct.cmd.steps.campaings.CampaignGraphQlSteps;
import ru.yandex.autotests.direct.cmd.steps.campaings.CampaignSteps;
import ru.yandex.autotests.direct.cmd.steps.campaings.CampsMultiActionSteps;
import ru.yandex.autotests.direct.cmd.steps.campaings.CopyCampSteps;
import ru.yandex.autotests.direct.cmd.steps.campaings.SetCampDontShowMultiSteps;
import ru.yandex.autotests.direct.cmd.steps.campaings.StrategySteps;
import ru.yandex.autotests.direct.cmd.steps.client.ClientsSteps;
import ru.yandex.autotests.direct.cmd.steps.conditions.AjaxGetFilterSchemaSteps;
import ru.yandex.autotests.direct.cmd.steps.counters.AjaxCheckUserCountersSteps;
import ru.yandex.autotests.direct.cmd.steps.creatives.CreativesSteps;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelStatisticSteps;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelSteps;
import ru.yandex.autotests.direct.cmd.steps.feeds.AjaxDeleteFeedsSteps;
import ru.yandex.autotests.direct.cmd.steps.feeds.AjaxGetFeedHistorySteps;
import ru.yandex.autotests.direct.cmd.steps.feeds.AjaxGetFeedsSteps;
import ru.yandex.autotests.direct.cmd.steps.feeds.SaveFeedsSteps;
import ru.yandex.autotests.direct.cmd.steps.firsthelp.FirstHelpSteps;
import ru.yandex.autotests.direct.cmd.steps.forecast.ForecastSteps;
import ru.yandex.autotests.direct.cmd.steps.groups.AjaxUpdateShowConditionsSteps;
import ru.yandex.autotests.direct.cmd.steps.groups.GroupsSteps;
import ru.yandex.autotests.direct.cmd.steps.groups.dynamicconditions.AjaxEditDynamicConditionsSteps;
import ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps;
import ru.yandex.autotests.direct.cmd.steps.modifyuser.ModifyUserSteps;
import ru.yandex.autotests.direct.cmd.steps.pagesize.PageSizeSteps;
import ru.yandex.autotests.direct.cmd.steps.performancefilters.AjaxEditPerformanceFiltersSteps;
import ru.yandex.autotests.direct.cmd.steps.phrases.PhraseSteps;
import ru.yandex.autotests.direct.cmd.steps.provenewagencyclients.ProveNewAgencyClientsSteps;
import ru.yandex.autotests.direct.cmd.steps.representative.RepresentativeSteps;
import ru.yandex.autotests.direct.cmd.steps.retargeting.RetargetingSteps;
import ru.yandex.autotests.direct.cmd.steps.showdiag.ShowDiagSteps;
import ru.yandex.autotests.direct.cmd.steps.stat.StatSteps;
import ru.yandex.autotests.direct.cmd.steps.stepzero.StepZeroProcessSteps;
import ru.yandex.autotests.direct.cmd.steps.stepzero.StepZeroSteps;
import ru.yandex.autotests.direct.cmd.steps.teasers.AjaxSetRecomedationsEmailSteps;
import ru.yandex.autotests.direct.cmd.steps.transfer.TransferDoneSteps;
import ru.yandex.autotests.direct.cmd.steps.transfer.TransferSteps;
import ru.yandex.autotests.direct.cmd.steps.user.AjaxSetAutoResourcesSteps;
import ru.yandex.autotests.direct.cmd.steps.user.AjaxUserOptionsSteps;
import ru.yandex.autotests.direct.cmd.steps.user.UserSettingsSteps;
import ru.yandex.autotests.direct.cmd.steps.vcards.VCardsSteps;
import ru.yandex.autotests.direct.cmd.steps.wallet.WalletSteps;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.httpclientlite.context.ConnectionContext;

import static ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps.getInstance;

public class DirectCmdSteps {

    private static final Logger LOG = LoggerFactory.getLogger(DirectCmdSteps.class);

    private DirectStepsContext context;

    private ru.yandex.autotests.direct.httpclient.UserSteps oldSteps;

    public DirectCmdSteps(DirectTestRunProperties properties) {
        this.context = new DirectStepsContext().withProperties(properties);
        this.context().useConnectionContext(
                new ConnectionContext().
                        scheme("https").
                        host(properties.getDirectCmdHost().replace("https://", "")).
                        path("/registered/main.pl")
        );
    }

    public DirectCmdSteps() {
        this(DirectTestRunProperties.getInstance());
    }

    public Logger log() {
        return LOG;
    }

    public CampaignSteps campaignSteps() {
        return getInstance(CampaignSteps.class, context());
    }

    public CampaignGraphQlSteps campaignGraphQlSteps() {
        return getInstance(CampaignGraphQlSteps.class, context());
    }

    public AjaxCampSteps ajaxCampaignSteps() {
        return getInstance(AjaxCampSteps.class, context());
    }

    public AjaxSetAutoResourcesSteps ajaxSetAutoResourcesSteps() {
        return getInstance(AjaxSetAutoResourcesSteps.class, context());
    }

    public GroupsSteps groupsSteps() {
        return getInstance(GroupsSteps.class, context());
    }

    public PhraseSteps phrasesSteps() {
        return getInstance(PhraseSteps.class, context());
    }

    public StopResumeBannerSteps stopResumeBannerSteps() {
        return getInstance(StopResumeBannerSteps.class, context());
    }

    public SearchBannersSteps searchBannersSteps() {
        return getInstance(SearchBannersSteps.class, context());
    }

    public ForecastSteps forecastSteps() {
        return getInstance(ForecastSteps.class, context());
    }

    public ModifyUserSteps modifyUserSteps() {
        return getInstance(ModifyUserSteps.class, context());
    }

    public SaveFeedsSteps saveFeedsSteps() {
        return getInstance(SaveFeedsSteps.class, context());
    }

    public AjaxDeleteFeedsSteps ajaxDeleteFeedsSteps() {
        return getInstance(AjaxDeleteFeedsSteps.class, context());
    }

    public AjaxGetFeedsSteps ajaxGetFeedsSteps() {
        return getInstance(AjaxGetFeedsSteps.class, context());
    }

    public AjaxGetFeedHistorySteps ajaxGetFeedHistorySteps() {
        return getInstance(AjaxGetFeedHistorySteps.class, context());
    }

    public ExcelSteps excelSteps() {
        return getInstance(ExcelSteps.class, context());
    }

    public AjaxEditPerformanceFiltersSteps ajaxEditPerformanceFiltersSteps() {
        return getInstance(AjaxEditPerformanceFiltersSteps.class, context());
    }

    public AjaxGetFilterSchemaSteps ajaxGetFilterSchemaSteps() {
        return getInstance(AjaxGetFilterSchemaSteps.class, context());
    }

    public WalletSteps walletSteps() {
        return getInstance(WalletSteps.class, context());
    }

    public BannerImagesSteps bannerImagesSteps() {
        return getInstance(BannerImagesSteps.class, context());
    }

    public ClientSteps clientSteps() {
        return getInstance(ClientSteps.class, context());
    }

    public CopyCampSteps copyCampSteps() {
        return getInstance(CopyCampSteps.class, context());
    }

    public AjaxCheckUserCountersSteps ajaxCheckUserCountersSteps() {
        return getInstance(AjaxCheckUserCountersSteps.class, context());
    }

    public AjaxEditDynamicConditionsSteps ajaxEditDynamicConditionsSteps() {
        return getInstance(AjaxEditDynamicConditionsSteps.class, context());
    }

    public AjaxUpdateShowConditionsSteps ajaxUpdateShowConditionsSteps() {
        return getInstance(AjaxUpdateShowConditionsSteps.class, context());
    }

    public UserSettingsSteps userSettingsSteps() {
        return getInstance(UserSettingsSteps.class, context());
    }

    public AjaxDisableAutocorrectionWarningSteps ajaxDisableAutocorrectionWarningSteps() {
        return getInstance(AjaxDisableAutocorrectionWarningSteps.class, context());
    }

    public VCardsSteps vCardsSteps() {
        return getInstance(VCardsSteps.class, context());
    }

    public StepZeroProcessSteps stepZeroProcessSteps() {
        return getInstance(StepZeroProcessSteps.class, context());
    }

    public CreativesSteps creativesSteps() {
        return getInstance(CreativesSteps.class, context());
    }

    public TransferSteps transferSteps() {
        return getInstance(TransferSteps.class, context());
    }

    public TransferDoneSteps transferDoneSteps() {
        return getInstance(TransferDoneSteps.class, context());
    }

    public BannerSteps bannerSteps() {
        return getInstance(BannerSteps.class, context());
    }

    public ProveNewAgencyClientsSteps proveNewAgencyClientsSteps() {
        return getInstance(ProveNewAgencyClientsSteps.class, context());
    }

    // deprecated вместо удаления, чтобы не менять старые тесты, которые используют эти степы
    @Deprecated
    public PassportSteps onPassport() {
        return getInstance(PassportSteps.class, context());
    }

    public AuthSteps authSteps() {
        return getInstance(AuthSteps.class, context());
    }

    public ru.yandex.autotests.direct.httpclient.UserSteps oldSteps() {
        if (oldSteps == null) {
            oldSteps = new ru.yandex.autotests.direct.httpclient.UserSteps(this);
        }
        return oldSteps;
    }

    public void setOldSteps(ru.yandex.autotests.direct.httpclient.UserSteps oldUserSteps) {
        this.oldSteps = oldUserSteps;
    }

    public DirectStepsContext context() {
        return context;
    }

    public StatSteps statSteps() {
        return getInstance(StatSteps.class, context());
    }

    public StrategySteps strategySteps() {
        return getInstance(StrategySteps.class, context());
    }

    public StepZeroSteps stepZeroSteps() {
        return getInstance(StepZeroSteps.class, context());
    }

    public BannersAdditionsSteps bannersAdditionsSteps() {
        return getInstance(BannersAdditionsSteps.class, context());
    }

    public RetargetingSteps retargetingSteps() {
        return getInstance(RetargetingSteps.class, context());
    }

    public SetCampDontShowMultiSteps setCampDontShowMultiSteps() {
        return getInstance(SetCampDontShowMultiSteps.class, context());
    }

    public AutopaySettingsSteps autopaySettingsSteps() {
        return getInstance(AutopaySettingsSteps.class, context());
    }

    public TopLevelSteps topLevelSteps() {
        return getInstance(TopLevelSteps.class, context());
    }

    public RepresentativeSteps representativeSteps() {
        return getInstance(RepresentativeSteps.class, context());
    }

    public AjaxUserOptionsSteps ajaxUserOptionsSteps() {
        return getInstance(AjaxUserOptionsSteps.class, context());
    }

    public AjaxSetRecomedationsEmailSteps ajaxSetRecomedationsEmailSteps() {
        return getInstance(AjaxSetRecomedationsEmailSteps.class, context());
    }

    public ClientsSteps сlientsSteps() {
        return getInstance(ClientsSteps.class, context());
    }

    public ShowDiagSteps showDiagSteps() {
        return getInstance(ShowDiagSteps.class, context());
    }

    public PageSizeSteps pageSizeSteps() {
        return getInstance(PageSizeSteps.class, context());
    }

    public FirstHelpSteps firstHelpSteps() {
        return getInstance(FirstHelpSteps.class, context());
    }

    public CampsMultiActionSteps campsMultiActionSteps() {
        return getInstance(CampsMultiActionSteps.class, context());
    }

    public ExcelStatisticSteps excelStatisticSteps() {
        return getInstance(ExcelStatisticSteps.class, context());
    }

    public UserExternalServicesSteps userExternalServicesSteps() {
        return UserExternalServicesSteps.getInstance();
    }
}
