package ru.yandex.autotests.direct.cmd.steps.campaings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.jsoup.nodes.Document;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.campaigns.AjaxSaveDayBudgetRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.CreateABTestRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.CreateABTestResponse;
import ru.yandex.autotests.direct.cmd.data.campaigns.EditCampRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.OrderCampRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.RemoderateCampRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.SendMediaCampaignRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowAbTestResponse;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampsRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampsResponse;
import ru.yandex.autotests.direct.cmd.data.campaigns.StopABTestRequest;
import ru.yandex.autotests.direct.cmd.data.campunarc.CampUnarcRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.ContactInfoRequest;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.ChooseInterfaceTypeRequest;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.data.showcamp.DeleteCampRequest;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampRequest;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatRequest;
import ru.yandex.autotests.direct.cmd.data.showcampstat.ShowCampStatResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.util.BeanLoadHelper.loadCmdBean;
import static ru.yandex.autotests.irt.testutils.allure.AllureUtils.addJsonAttachment;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class CampaignSteps extends DirectBackEndSteps {
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    @Step("GET cmd = showCampMultiEdit")
    public ShowCampMultiEditResponse getShowCampMultiEdit(ShowCampMultiEditRequest request) {
        return get(CMD.SHOW_CAMP_MULTI_EDIT, request, ShowCampMultiEditResponse.class);
    }

    @Step("Начало АБ-тестирования (POST cmd = createABTest; login = {0}; campaignId: {1}, {2}; percentage = {3};" +
            "date_start = {4}; date_finish = {5}")
    public CreateABTestResponse createABTest(String login, Long campaignIdFirst, Long campaignIdSecond,
                                             Integer percentage,
                                             Date dateStart, Date dateFinish) {
        CreateABTestRequest createABTestRequest = new CreateABTestRequest()
                .withCid(campaignIdFirst)
                .withSecondaryCid(campaignIdSecond)
                .withPrimaryPercent(percentage)
                .withDateStart(new SimpleDateFormat(DATE_FORMAT).format(dateStart))
                .withDateFinish(new SimpleDateFormat(DATE_FORMAT).format(dateFinish))
                .withUlogin(login);
        return postCreateABTest(createABTestRequest);
    }

    @Step("Показ АБ-теститрования (GET cmd = showExperiments; login = {0})")
    public ShowAbTestResponse showExperiment(String login) {
        return get(CMD.SHOW_EXPERIMENTS, new BasicDirectRequest().withUlogin(login), ShowAbTestResponse.class);
    }

    @Step("Начало АБ-тестирования (POST cmd = createABTest)")
    public CreateABTestResponse postCreateABTest(CreateABTestRequest createABTestRequest) {
        return post(CMD.AJAX_CREATE_EXPERIMENT, createABTestRequest, CreateABTestResponse.class);
    }

    @Step("Завершение АБ-тестирования (POST cmd = ajaxStopExperimentt; login = {0}; experimentId = {1};)")
    public Void stopABTest(String login, Long experimentId) {
        StopABTestRequest stopABTestRequest = new StopABTestRequest().withExperimentId(experimentId).withUlogin(login);
        return get(CMD.AJAX_STOP_EXPERIMENT, stopABTestRequest, Void.class);
    }

    @Step("Открытие редактирования всех групп кампании (GET cmd = showCampMultiEdit; login = {0}; campaignId = {1}")
    public ShowCampMultiEditResponse getShowCampMultiEdit(String login, Long campaignId) {
        List<Long> groupIds = getShowCamp(login, campaignId.toString()).getGroups()
                .stream()
                .map(Banner::getAdGroupId)
                .collect(toList());

        ShowCampMultiEditRequest request = ShowCampMultiEditRequest.forCampaignAndGroups(login, campaignId, groupIds);
        return getShowCampMultiEdit(request);
    }

    @Step("Получение объекта кампании (GET cmd = showCampMultiEdit; login = {0}; campaignId = {1}")
    public Campaign getCampaign(String login, Long campaignId) {
        return getShowCampMultiEdit(login, campaignId).getCampaign();
    }

    @Step("Открытие редактирования групп кампании (GET cmd = showCampMultiEdit; login = {0}; campaignId = {1}")
    public ShowCampMultiEditResponse getShowCampMultiEdit(String login, long campaignId, List<Long> groupIds) {
        ShowCampMultiEditRequest request = ShowCampMultiEditRequest.forCampaignAndGroups(login, campaignId, groupIds);
        return getShowCampMultiEdit(request);
    }

    @Step("Открытие редактирования одного баннера " +
            "(GET cmd = showCampMultiEdit; login = {0}; campaignId = {1}; groupId = {2}; bannerId = {3}")
    public ShowCampMultiEditResponse getShowCampMultiEdit(String login, long campaignId, long groupId, long bannerId) {
        ShowCampMultiEditRequest request = ShowCampMultiEditRequest.forSingleBanner(
                login, campaignId, groupId, bannerId);
        return getShowCampMultiEdit(request);
    }

    @Step("GET cmd = showCamp (Открываем страницу кампании с номером {1} для логина {0})")
    public ShowCampResponse getShowCamp(String login, String campaignID) {
        ShowCampRequest params = new ShowCampRequest();
        params.setUlogin(login);
        params.setCid(campaignID);
        params.setTab("all");
        return get(CMD.SHOW_CAMP, params, ShowCampResponse.class);
    }

    @Step("GET cmd = showCamp (Открываем страницу кампании)")
    public ShowCampResponse getShowCamp(ShowCampRequest showCampRequest) {
        return get(CMD.SHOW_CAMP, showCampRequest, ShowCampResponse.class);
    }

    @Step("GET cmd = showCamps (открываем страницу редактирования кампании)")
    public ShowCampsResponse getShowCamps(ShowCampsRequest request) {
        return get(CMD.SHOW_CAMPS, request, ShowCampsResponse.class);
    }

    @Step("GET cmd = showCamps (открываем страницу редактирования кампании) для клиента {0}")
    public List<Long> getClientCampaigns(String client) {
        return Optional.of(getShowCamps(new ShowCampsRequest().withTab("all").withUlogin(client)).getCampaigns())
                .orElseThrow(() -> new DirectCmdStepsException("Не удалось получить кампании"))
                .stream()
                .map(c -> Long.valueOf(c.getCid()))
                .collect(toList());
    }


    @Step("GET cmd = showCamps (открываем страницу редактирования кампании) для клиента {0}")
    public ShowCampsResponse getShowCamps(String client) {
        return getShowCamps(new ShowCampsRequest().withUlogin(client));
    }

    @Step("GET cmd = showCamps (открываем страницу редактирования кампании) для клиента {0}")
    public ShowCampsResponse getShowCamps(String client, String tab) {
        return getShowCamps(new ShowCampsRequest().withTab(tab).withUlogin(client));
    }


    @Step("GET cmd = editCamp (открываем страницу редактирования кампании)")
    public EditCampResponse getEditCamp(EditCampRequest request) {
        return get(CMD.EDIT_CAMP, request, EditCampResponse.class);
    }

    @Step("GET cmd = editCamp (cid = {0} ulogin = {1}) (открываем страницу редактирования кампании)")
    public EditCampResponse getEditCamp(long campaignId, String login) {
        EditCampRequest request = new EditCampRequest()
                .withCampaignId(campaignId)
                .withUlogin(login);
        return getEditCamp(request);
    }

    @Step("POST cmd=saveNewCamp (создаем новую кампанию)")
    public RedirectResponse postSaveNewCamp(SaveCampRequest request) {
        addJsonAttachment("Параметры кампании", JsonUtils.toStringLow(request));
        return post(CMD.SAVE_NEW_CAMP, request, RedirectResponse.class);
    }

    @Step("POST cmd=saveNewCamp (Сохраняем новую кампанию с невалидными данными)")
    public CampaignErrorResponse postSaveNewCampInvalidData(SaveCampRequest request) {
        return post(CMD.SAVE_NEW_CAMP, request, CampaignErrorResponse.class);
    }

    @Step("Создаем новую текстовую кампанию")
    public Long saveNewDefaultTextCampaign(String agency, String ulogin) {
        return extractCidFromSaveCampResponse(postSaveNewCamp(
                loadCmdBean(CmdBeans.SAVE_NEW_TEXT_CAMP_DEFAULT, SaveCampRequest.class)
                        .withFor_agency(agency)
                        .withUlogin(ulogin))
        );
    }

    @Step("Создаем новую текстовую кампанию")
    public Long saveNewDefaultTextCampaign(String ulogin) {
        return extractCidFromSaveCampResponse(postSaveNewCamp(
                loadCmdBean(CmdBeans.SAVE_NEW_TEXT_CAMP_DEFAULT, SaveCampRequest.class)
                        .withUlogin(ulogin))
        );
    }

    @Step("Создаем новую текстовую кампанию")
    public Long saveNewDefaultDynamicCampaign(String agency, String ulogin) {
        return extractCidFromSaveCampResponse(postSaveNewCamp(
                loadCmdBean(CmdBeans.SAVE_NEW_DYNAMIC_CAMP_FULL, SaveCampRequest.class)
                        .withFor_agency(agency)
                        .withUlogin(ulogin))
        );
    }

    @Step("Создаем новую текстовую кампанию")
    public Long saveNewDefaultDynamicCampaign(String ulogin) {
        return extractCidFromSaveCampResponse(postSaveNewCamp(
                loadCmdBean(CmdBeans.SAVE_NEW_DYNAMIC_CAMP_FULL, SaveCampRequest.class)
                        .withUlogin(ulogin))
        );
    }

    @Step("Создаем новую кампанию")
    /**
     * Не забывайте вручную удалять созданную кампанию!
     */
    public Long saveNewCampaign(SaveCampRequest request) {
        // Для старого интерфейса возвращается url вида ?cid=126609001&cmd=addBannerMultiEdit&from_newCamp=1,
        // для нового - ?campaigns-ids=112201413&from_old_interface=1&is-new=1&is-new-empty-group=1
        return extractCidFromSaveCampResponse(postSaveNewCamp(request));
    }

    public static Long extractCidFromSaveCampResponse(RedirectResponse response) {
        Long cid = response.getLocationParamAsLong(LocationParam.CID);
        return cid != null ? cid : response.getLocationParamAsLong(LocationParam.CAMPAIGNS_IDS);
    }

    @Step("Сохраняем изменения кампании")
    public RedirectResponse postSaveCamp(SaveCampRequest request) {
        addJsonAttachment("Параметры кампании", JsonUtils.toStringLow(request));
        return post(CMD.SAVE_CAMP, request, RedirectResponse.class);
    }

    @Step("POST cmd=saveCamp (Сохраняем кампанию с невалидными данными)")
    public CampaignErrorResponse postSaveCampInvalidData(SaveCampRequest request) {
        return post(CMD.SAVE_CAMP, request, CampaignErrorResponse.class);
    }

    @Step("Разархивируем кампанию {0}")
    public void unArchiveCampaign(String ulogin, Long cid) {
        CampUnarcRequest request = new CampUnarcRequest()
                .withCid(String.valueOf(cid))
                .withTab("arch")
                .withUlogin(ulogin);
        getCampUnarc(request);
    }

    @Step("GET cmd=campUnarc (разархивируем кампании)")
    public CommonResponse getCampUnarc(CampUnarcRequest campUnarcRequest) {
        addJsonAttachment("Параметры кампании", JsonUtils.toStringLow(campUnarcRequest));
        return get(CMD.CAMP_UNARC, campUnarcRequest, CommonResponse.class);
    }

    @Step("GET cmd=campUnarc архивируем кампанию {1} клиента {0}")
    public CommonResponse getCampArc(String ulogin, Long cid) {
        CampUnarcRequest request = new CampUnarcRequest()
                .withCid(cid.toString())
                .withUlogin(ulogin);
        return get(CMD.CAMP_ARC, request, CommonResponse.class);
    }

    @Step("GET cmd=stopCamp останавливаем кампанию {1} клиента {0}")
    public CommonResponse getStopCamp(String ulogin, Long cid) {
        CampUnarcRequest request = new CampUnarcRequest()
                .withCid(cid.toString())
                .withUlogin(ulogin);
        return get(CMD.STOP_CAMP, request, CommonResponse.class);
    }

    @Step("GET cmd=delCamp (удаляем кампанию с номером {1} для логина {0})")
    public void deleteCampaign(String login, Long campaignID) {
        DeleteCampRequest params = new DeleteCampRequest()
                .withCid(campaignID.toString())
                .withUlogin(login);
        get(CMD.DEL_CAMP, params, Void.class);
    }

    @Step("POST cmd=saveCampEasy (сохраняем компанию в легком интерфейсе)")
    public RedirectResponse postSaveCampEasy(SaveCampRequest request) {
        addJsonAttachment("Параметры кампании", JsonUtils.toStringLow(request));
        return post(CMD.SAVE_CAMP_EASY, request, RedirectResponse.class);
    }

    @Step("GET cmd=showContactInfo")
    public RedirectResponse getShowContactInfo(String login, Long cid, Long bid) {
        ContactInfoRequest contactInfoRequest = new ContactInfoRequest();
        contactInfoRequest.setBid(bid);
        contactInfoRequest.setCid(cid);
        contactInfoRequest.setUlogin(login);
        return get(CMD.SHOW_CONTACT_INFO, contactInfoRequest, RedirectResponse.class);
    }

    @Step("GET cmd=showCampStat (открываем статистику по кампании)")
    public ShowCampStatResponse getShowCampStat(ShowCampStatRequest showCampRequest) {
        return get(CMD.SHOW_CAMP_STAT, showCampRequest, ShowCampStatResponse.class);
    }

    @Step("GET cmd=chooseInterfaceType (открываем страницу выбора типа интерфейса)")
    public void getСhooseInterfaceType() {
        get(CMD.CHOOSE_INTERFACE_TYPE, null, Document.class);
    }

    @Step("POST cmd=chooseInterfaceType (выбираем тип интерфейса)")
    public RedirectResponse postSwitchEasiness(ChooseInterfaceTypeRequest request) {
        return post(CMD.SWITCH_EASINESS, request, RedirectResponse.class);
    }

    @Step("POST cmd=remoderateCamp (перемодерируем кампанию)")
    public void postRemoderateCamp(RemoderateCampRequest request) {
        post(CMD.REMODERATE_CAMP, request, Void.class);
    }

    @Step("перемодерируем кампанию {0} у клиента {1}")
    public void remoderateCamp(Long cid, String uLogin) {
        postRemoderateCamp(new RemoderateCampRequest().withCid(cid).withUlogin(uLogin));
    }

    @Step("GET: cmd=sendMDMedia перемодерируем МКБ кампанию {0} у клиента {1}")
    public void sendMediaCampaign(Long cid, Long mgid, String uLogin) {
        SendMediaCampaignRequest request = new SendMediaCampaignRequest()
                .withCid(cid)
                .withMgid(mgid)
                .withUlogin(uLogin)
                .withTab("off");
        get(CMD.SEND_MEDIA_CAMPAIGN, request, Void.class);
    }

    @Step("POST: cmd=orderCamp отправка на модерацию кампании {0} у клиента {1}")
    public void orderCamp(Long cid, String uLogin) {
        OrderCampRequest request = new OrderCampRequest()
                .withCid(cid)
                .withAccept("accept")
                .withAgree("yes")
                .withUlogin(uLogin);
        post(CMD.ORDER_CAMP, request, Void.class);
    }

    public void setDayBudget(Long cid, DayBudget dayBudget, String login) {
        AjaxSaveDayBudgetRequest request = new AjaxSaveDayBudgetRequest()
                .withCid(String.valueOf(cid))
                .withDayBudget(dayBudget)
                .withUlogin(login);
        CommonResponse response = postAjaxSaveDayBudget(request);
        assumeThat("Успешно установлен дневной бюджет", response.getOk(), equalTo("1"));
    }

    @Step("POST: cmd=ajaxSaveDayBudget установка древного бюджета")
    public CommonResponse postAjaxSaveDayBudget(AjaxSaveDayBudgetRequest request) {
        return post(CMD.AJAX_SAVE_DAY_BUDGET, request, CommonResponse.class);
    }

    @Step("POST: cmd=ajaxSaveDayBudget установка древного бюджета")
    public ErrorResponse postAjaxSaveDayBudgetErrorResponse(AjaxSaveDayBudgetRequest request) {
        return post(CMD.AJAX_SAVE_DAY_BUDGET, request, ErrorResponse.class);
    }

    @Step("Удаляем все кампании клиенту, оставляя заданное число")
    public void deleteClientCampaignsExceptLast(String client, Long leaveCampsNumber) {
        List<Long> clientCampaigns = getClientCampaigns(client);
        if (clientCampaigns != null && !clientCampaigns.isEmpty() && clientCampaigns.size() > leaveCampsNumber
                .intValue()) {
            for (int i = 0; i < clientCampaigns.size() - leaveCampsNumber.intValue(); i++) {
                deleteCampaign(client, clientCampaigns.get(i));
            }
        }
    }
}
