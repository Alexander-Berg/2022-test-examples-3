package ru.yandex.autotests.direct.httpclient;

import org.apache.http.client.HttpClient;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.steps.base.DirectStepsContext;
import ru.yandex.autotests.direct.httpclient.core.DirectRequestBuilder;
import ru.yandex.autotests.direct.httpclient.core.DirectResponseHandler;
import ru.yandex.autotests.direct.httpclient.steps.AdvertizeSteps;
import ru.yandex.autotests.direct.httpclient.steps.AgSearchSteps;
import ru.yandex.autotests.direct.httpclient.steps.AgencySteps;
import ru.yandex.autotests.direct.httpclient.steps.ChangeManagerOfAgencySteps;
import ru.yandex.autotests.direct.httpclient.steps.ChangeManagerOfClientSteps;
import ru.yandex.autotests.direct.httpclient.steps.ClientSteps;
import ru.yandex.autotests.direct.httpclient.steps.CommonSteps;
import ru.yandex.autotests.direct.httpclient.steps.ManageSTeamleadersSteps;
import ru.yandex.autotests.direct.httpclient.steps.ManageTeamleadersSteps;
import ru.yandex.autotests.direct.httpclient.steps.ModifyUserSteps;
import ru.yandex.autotests.direct.httpclient.steps.ProveNewAgencyClientsSteps;
import ru.yandex.autotests.direct.httpclient.steps.ShowClientsSteps;
import ru.yandex.autotests.direct.httpclient.steps.ShowManagerMyClientsSteps;
import ru.yandex.autotests.direct.httpclient.steps.ShowStaffSteps;
import ru.yandex.autotests.direct.httpclient.steps.ShowUserEmailsSteps;
import ru.yandex.autotests.direct.httpclient.steps.banners.AjaxApplyRejectCorrectionsSteps;
import ru.yandex.autotests.direct.httpclient.steps.banners.AjaxEditAdGroupDynamicConditionsSteps;
import ru.yandex.autotests.direct.httpclient.steps.banners.AjaxGetBannersCountSteps;
import ru.yandex.autotests.direct.httpclient.steps.banners.AjaxGetTransitionsByPhrasesSteps;
import ru.yandex.autotests.direct.httpclient.steps.banners.AjaxSaveAdGroupTags;
import ru.yandex.autotests.direct.httpclient.steps.banners.AjaxUpdateShowConditionsSteps;
import ru.yandex.autotests.direct.httpclient.steps.banners.GetAdGroupSteps;
import ru.yandex.autotests.direct.httpclient.steps.banners.GroupsSteps;
import ru.yandex.autotests.direct.httpclient.steps.banners.ManageVCardsSteps;
import ru.yandex.autotests.direct.httpclient.steps.banners.SaveMediaBannerSteps;
import ru.yandex.autotests.direct.httpclient.steps.banners.SearchBannersSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.AjaxSaveCampDescriptionSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.AjaxStopResumeCampSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.CampUnarcSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.CampaignsSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.CopyCampSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.DelCampSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.EditCampSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.SaveCampEasySteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.SaveCampSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.SetAutoPriceAjaxSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.ShowCampsSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.ShowContactInfoSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.ShowMediaCampSteps;
import ru.yandex.autotests.direct.httpclient.steps.client.SaveSettingsSteps;
import ru.yandex.autotests.direct.httpclient.steps.client.UserSettingsSteps;
import ru.yandex.autotests.direct.httpclient.steps.firsthelp.FirstHelpSteps;
import ru.yandex.autotests.direct.httpclient.steps.mobileappbanners.EditAdGroupsMobileContentSteps;
import ru.yandex.autotests.direct.httpclient.steps.newclient.AjaxRegisterLoginSteps;
import ru.yandex.autotests.direct.httpclient.steps.newclient.AjaxSuggestLoginSteps;
import ru.yandex.autotests.direct.httpclient.steps.newclient.AjaxValidateLoginSteps;
import ru.yandex.autotests.direct.httpclient.steps.newclient.AjaxValidatePasswordSteps;
import ru.yandex.autotests.direct.httpclient.steps.newclient.ShowRegisterLoginPageSteps;
import ru.yandex.autotests.direct.httpclient.steps.payment.PayForAllSteps;
import ru.yandex.autotests.direct.httpclient.steps.payment.PaySteps;
import ru.yandex.autotests.direct.httpclient.steps.retargeting.AjaxDeleteRetargetingCondSteps;
import ru.yandex.autotests.direct.httpclient.steps.retargeting.AjaxGetGoalsForRetargetingSteps;
import ru.yandex.autotests.direct.httpclient.steps.retargeting.AjaxSaveRetargetingCondSteps;
import ru.yandex.autotests.direct.httpclient.steps.retargeting.ShowRetargetingCondSteps;
import ru.yandex.autotests.direct.httpclient.steps.sandbox.APISandboxSteps;
import ru.yandex.autotests.direct.httpclient.steps.stepzero.StepZeroProcessSteps;
import ru.yandex.autotests.direct.httpclient.steps.stepzero.StepZeroSteps;
import ru.yandex.autotests.direct.httpclient.steps.strategy.StrategySteps;
import ru.yandex.autotests.direct.httpclient.steps.transfer.TransferDoneSteps;
import ru.yandex.autotests.direct.httpclient.steps.transfer.TransferSteps;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.httpclient.lite.core.config.HttpClientConnectionConfig;
import ru.yandex.autotests.httpclient.lite.core.config.HttpStepsConfig;
import ru.yandex.autotests.httpclientlite.context.ConnectionContext;

import static ru.yandex.autotests.httpclient.lite.core.steps.BackEndBaseSteps.getInstance;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class UserSteps {

    protected HttpStepsConfig config;
    protected DirectCmdSteps directCmdSteps;

    public HttpStepsConfig getConfig() {
        return config;
    }

    @Deprecated
    public static UserSteps getUserStepsWithGetVars(String url) {
        UserSteps steps = new UserSteps();
        steps.config.getClientConfig().host(url);
        steps.directCmdSteps.context().getConnectionContext().host(url);
        return steps;
    }

    @Deprecated
    public static UserSteps getUserStepsWithGetVars() {
        return new UserSteps();
    }

    @Deprecated
    public UserSteps(Boolean shit) {
        this(DirectTestRunProperties.getInstance());
    }

    public UserSteps() {
        this(DirectTestRunProperties.getInstance());
    }

    public UserSteps(DirectCmdSteps steps) {
        HttpClientConnectionConfig clientConfig = new HttpClientConnectionConfig();
        String host = steps.context().getProperties().getDirectCmdHost().replace("https://", "");
        clientConfig.host(host);
        clientConfig.scheme("https");
        clientConfig.path("/registered/main.pl");
        this.config = new HttpStepsConfig()
                .useClientConfig(clientConfig)
                .useRequestBuilder(new DirectRequestBuilder(clientConfig, true))
                .useHandler(new DirectResponseHandler());

        this.directCmdSteps = steps;
        this.directCmdSteps.setOldSteps(this);
        this.config.useHttpClient(directCmdSteps.context().getHttpClient());
    }

    public UserSteps(DirectTestRunProperties properties) {
        HttpClientConnectionConfig clientConfig = new HttpClientConnectionConfig();
        String host = properties.getDirectCmdHost().replace("https://", "");
        clientConfig.host(host);
        clientConfig.scheme("https");
        clientConfig.path("/registered/main.pl");
        this.config = new HttpStepsConfig()
                .useClientConfig(clientConfig)
                .useRequestBuilder(new DirectRequestBuilder(clientConfig, true))
                .useHandler(new DirectResponseHandler());

        this.directCmdSteps = new DirectCmdSteps(properties);
        this.directCmdSteps.setOldSteps(this);
        this.config.useHttpClient(directCmdSteps.context().getHttpClient());
    }

    private DirectStepsContext configToContext(HttpStepsConfig config) {
        DirectStepsContext newStepsContext = new DirectStepsContext()
                .withProperties(DirectTestRunProperties.getInstance());
        newStepsContext.useConnectionContext(
                new ConnectionContext().
                        scheme(config.getClientConfig().getScheme()).
                        host(config.getClientConfig().getHost()).
                        port(config.getClientConfig().getPort()).
                        path(config.getClientConfig().getPath()));
        return newStepsContext;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.config.useHttpClient(httpClient);
    }

    public AgencySteps agencySteps() {
        return getInstance(AgencySteps.class, config);
    }

    public ClientSteps clientSteps() {
        return getInstance(ClientSteps.class, config);
    }

    public FirstHelpSteps firstHelpSteps() {
        return getInstance(FirstHelpSteps.class, config);
    }

    public APISandboxSteps apiSandboxSteps() {
        return getInstance(APISandboxSteps.class, config);
    }

    public CampaignsSteps campaignsSteps() {
        return getInstance(CampaignsSteps.class, config);
    }

    public ru.yandex.autotests.direct.cmd.steps.auth.PassportSteps onPassport() {
        return directCmdSteps.onPassport();
    }

    public StrategySteps strategySteps() {
        return getInstance(StrategySteps.class, config);
    }

    public GroupsSteps groupsSteps() {
        return getInstance(GroupsSteps.class, config);
    }

    public ShowCampsSteps onShowCamps() {
        return getInstance(ShowCampsSteps.class, config);
    }

    public ShowClientsSteps onShowClients() {
        return getInstance(ShowClientsSteps.class, config);
    }

    public ShowManagerMyClientsSteps onShowManagerMyClients() {
        return getInstance(ShowManagerMyClientsSteps.class, config);
    }

    public ShowUserEmailsSteps onShowUserEmails() {
        return getInstance(ShowUserEmailsSteps.class, config);
    }

    public AgSearchSteps onAgSearch() {
        return getInstance(AgSearchSteps.class, config);
    }

    public ChangeManagerOfClientSteps onChangeManagerOfClient() {
        return getInstance(ChangeManagerOfClientSteps.class, config);
    }

    public ChangeManagerOfAgencySteps onChangeManagerOfAgency() {
        return getInstance(ChangeManagerOfAgencySteps.class, config);
    }

    public ModifyUserSteps onModifyUser() {
        return getInstance(ModifyUserSteps.class, config);
    }

    public ShowStaffSteps onShowStaff() {
        return getInstance(ShowStaffSteps.class, config);
    }

    public ManageTeamleadersSteps onManageTeamleaders() {
        return getInstance(ManageTeamleadersSteps.class, config);
    }

    public ManageSTeamleadersSteps onManageSTeamleaders() {
        return getInstance(ManageSTeamleadersSteps.class, config);
    }

    public CommonSteps commonSteps() {
        return getInstance(CommonSteps.class, config);
    }

    public AjaxSaveRetargetingCondSteps retargetingSteps() {
        return getInstance(AjaxSaveRetargetingCondSteps.class, config);
    }

    public AjaxDeleteRetargetingCondSteps ajaxDeleteRetargetingSteps() {
        return getInstance(AjaxDeleteRetargetingCondSteps.class, config);
    }

    public AjaxGetGoalsForRetargetingSteps getGoalsForRetargetingSteps() {
        return getInstance(AjaxGetGoalsForRetargetingSteps.class, config);
    }

    public ShowRetargetingCondSteps onShowRetargetingCond() {
        return getInstance(ShowRetargetingCondSteps.class, config);
    }

    public AdvertizeSteps onAdvertize() {
        return getInstance(AdvertizeSteps.class, config);
    }

    public AjaxValidateLoginSteps onAjaxValidateLogin() {
        return getInstance(AjaxValidateLoginSteps.class, config);
    }

    public ShowRegisterLoginPageSteps onShowRegisterLoginPage() {
        return getInstance(ShowRegisterLoginPageSteps.class, config);
    }

    public AjaxSuggestLoginSteps onAjaxSuggestLogin() {
        return getInstance(AjaxSuggestLoginSteps.class, config);
    }

    public AjaxValidatePasswordSteps onAjaxValidatePassword() {
        return getInstance(AjaxValidatePasswordSteps.class, config);
    }

    public AjaxRegisterLoginSteps onAjaxRegisterLogin() {
        return getInstance(AjaxRegisterLoginSteps.class, config);
    }

    public AjaxStopResumeCampSteps ajaxStopResumeCampSteps() {
        return getInstance(AjaxStopResumeCampSteps.class, config);
    }

    public AjaxSaveCampDescriptionSteps ajaxSaveCampDescriptionSteps() {
        return getInstance(AjaxSaveCampDescriptionSteps.class, config);
    }

    public AjaxSaveAdGroupTags ajaxSaveAdGroupTagsSteps() {
        return getInstance(AjaxSaveAdGroupTags.class, config);
    }

    public EditCampSteps onEditCamp() {
        return getInstance(EditCampSteps.class, config);
    }

    public SaveCampSteps onSaveCamp() {
        return getInstance(SaveCampSteps.class, config);
    }

    public UserSettingsSteps onUserSettings() {
        return getInstance(UserSettingsSteps.class, config);
    }

    public SaveSettingsSteps onSaveSettings() {
        return getInstance(SaveSettingsSteps.class, config);
    }

    public StepZeroProcessSteps stepZeroProcessSteps() {
        return getInstance(StepZeroProcessSteps.class, config);
    }

    public StepZeroSteps stepZeroSteps() {
        return getInstance(StepZeroSteps.class, config);
    }

    public SaveCampEasySteps onSaveCampEasy() {
        return getInstance(SaveCampEasySteps.class, config);
    }

    public CopyCampSteps onCopyCamp() {
        return getInstance(CopyCampSteps.class, config);
    }

    public CampUnarcSteps onCampUnarc() {
        return getInstance(CampUnarcSteps.class, config);
    }

    public ProveNewAgencyClientsSteps onProveNewAgencyClients() {
        return getInstance(ProveNewAgencyClientsSteps.class, config);
    }

    public AjaxUpdateShowConditionsSteps ajaxUpdatePhrasesAndPricesSteps() {
        return getInstance(AjaxUpdateShowConditionsSteps.class, config);
    }

    public AjaxEditAdGroupDynamicConditionsSteps ajaxEditAdGroupDynamicConditions() {
        return getInstance(AjaxEditAdGroupDynamicConditionsSteps.class, config);
    }

    public AjaxApplyRejectCorrectionsSteps ajaxApplyRejectCorrections() {
        return getInstance(AjaxApplyRejectCorrectionsSteps.class, config);
    }

    public AjaxGetTransitionsByPhrasesSteps ajaxGetTransitionsByPhrasesSteps() {
        return getInstance(AjaxGetTransitionsByPhrasesSteps.class, config);
    }

    public ManageVCardsSteps manageVCardsSteps() {
        return getInstance(ManageVCardsSteps.class, config);
    }

    public DelCampSteps delCampSteps() {
        return getInstance(DelCampSteps.class, config);
    }

    public SetAutoPriceAjaxSteps setAutoPriceAjaxSteps() {
        return getInstance(SetAutoPriceAjaxSteps.class, config);
    }

    public AjaxGetBannersCountSteps ajaxGetBannersCount() {
        return getInstance(AjaxGetBannersCountSteps.class, config);
    }

    public SaveMediaBannerSteps saveMediaBannerSteps() {
        return getInstance(SaveMediaBannerSteps.class, config);
    }

    public ShowContactInfoSteps showContactInfoSteps() {
        return getInstance(ShowContactInfoSteps.class, config);
    }

    public ShowMediaCampSteps showMediaCampSteps() {
        return getInstance(ShowMediaCampSteps.class, config);
    }

    public PaySteps paySteps() {
        return getInstance(PaySteps.class, config);
    }

    public SearchBannersSteps searchBannersSteps() {
        return getInstance(SearchBannersSteps.class, config);
    }

    public PayForAllSteps payForAllSteps() {
        return getInstance(PayForAllSteps.class, config);
    }

    public TransferSteps transferSteps() {
        return getInstance(TransferSteps.class, config);
    }

    public TransferDoneSteps transferDoneSteps() {
        return getInstance(TransferDoneSteps.class, config);
    }

    public GetAdGroupSteps getAdGroupSteps() {
        return getInstance(GetAdGroupSteps.class, config);
    }

    public EditAdGroupsMobileContentSteps getEditAdGroupsMobileContentSteps() {
        return getInstance(EditAdGroupsMobileContentSteps.class, config);
    }


}
