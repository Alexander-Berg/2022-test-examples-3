package ru.yandex.autotests.direct.cmd.steps.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.GsonBuilder;
import org.hamcrest.Matcher;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorsResponse;
import ru.yandex.autotests.direct.cmd.data.commons.JsonRedirectResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent.EditAdGroupsMobileContentRequest;
import ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent.EditAdGroupsMobileContentResponse;
import ru.yandex.autotests.direct.cmd.data.editdynamicadgroups.EditDynamicAdGroupsRequest;
import ru.yandex.autotests.direct.cmd.data.editdynamicadgroups.EditDynamicAdGroupsResponse;
import ru.yandex.autotests.direct.cmd.data.mediaplan.SendOptimizeRequest;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.EditAdGroupsPerformanceRequest;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.EditAdGroupsPerformanceResponse;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.saveadgrouptags.SaveAdGroupTagsRequest;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.steps.campaings.CampaignSteps;
import ru.yandex.autotests.direct.cmd.steps.pagesize.PageSizeSteps;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.support.gson.NullValuesAdapterFactory;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;

public class GroupsSteps extends DirectBackEndSteps {

    @Step("Открытие превью группы на странице кампании (GET cmd = getAdGroup; ulogin = {0}; groupId = {1})")
    public Group getAdGroup(String ulogin, final Long groupId) {
        return post(CMD.GET_AD_GROUP, new BasicDirectRequest() {
            @SerializeKey("adgroup_id")
            private String adGroupId = groupId.toString();
        }.withUlogin(ulogin), Group.class);
    }

    @Step("POST cmd = saveTextAdGroups (сохранение текстовой группы)")
    public RedirectResponse addNewTextAdGroup(String ulogin, Long campaignId, Group group) {
        group.setCampaignID(campaignId.toString());
        group.getBanners().forEach(b -> b.withCid(campaignId));

        GroupsParameters groupRequest = GroupsParameters.forCamp(ulogin, campaignId, group, "1");
        return postSaveTextAdGroups(groupRequest);
    }

    @Step("POST cmd = saveTextAdGroups (сохранение текстовой группы)")
    public RedirectResponse postSaveTextAdGroups(GroupsParameters request) {
        return post(CMD.SAVE_TEXT_ADGROUPS, request, RedirectResponse.class);
    }

    @Step("POST cmd = saveTextAdGroups ожидаем ошибку")
    public GroupErrorsResponse postSaveTextAdGroupsInvalidData(GroupsParameters request) {
        return post(CMD.SAVE_TEXT_ADGROUPS, request, GroupErrorsResponse.class);
    }

    public JsonRedirectResponse postSavePerformanceAdGroups(String login, String campaignId, List<Group> group) {
        GroupsParameters params = new GroupsParameters();
        params.setCid(campaignId);
        params.setUlogin(login);
        params.setJsonGroups(new GsonBuilder().registerTypeAdapterFactory(new NullValuesAdapterFactory())
                .create().toJson(group));
        return postSavePerformanceAdGroups(params);
    }

    /**
     * Подготовка полученной от сервера группы для отправки ее снова (редактирование группы)
     *
     * @param group        группа для модификации
     * @param campaignType тип кампании, в котороой находится группа
     */
    public void prepareGroupForUpdate(Group group, CampaignTypeEnum campaignType) {
        if (group.getRetargetings() == null) {
            group.setRetargetings(emptyList());
        }
        if (group.getTags() == null) {
            if (campaignType == CampaignTypeEnum.TEXT || campaignType == CampaignTypeEnum.MOBILE) {
                group.setTags(emptyMap());
            } else {
                group.setTags(emptyList());
            }

        }
        if (campaignType == CampaignTypeEnum.DMO || campaignType == CampaignTypeEnum.DTO) {
            if (group.getHrefParams() == null) {
                group.setHrefParams("");
            }
            if (group.getMinusWords() == null) {
                group.setMinusWords(emptyList());
            }
        }
        if (campaignType == CampaignTypeEnum.DTO) {
            if (group.getBanners() != null) {
                prepareImageForBanner(group);
            }
        }
        if (campaignType == CampaignTypeEnum.MCBANNER) {
            group.withKeywords(group.getPhrases());

            group.getBanners().stream().forEach(banner -> {
                banner.withBody(null);
                banner.withDomain(null);
                banner.withTitle(null);
            });
            group.getKeywords().stream().forEach(keyword -> {
                keyword.withPrice(null);
                keyword.withPriceContext(null);
            });
            group.withRelevanceMatch(null)
                    .withRetargetings(null)
                    .withPhrases(null)
                    .withTags(new ArrayList<>());
        }
    }

    private void prepareImageForBanner(Group group) {
        for (Banner banner : group.getBanners()) {
            if (banner.getImage() == null) {
                banner.setImage("");
            }
        }
    }

    @Step("Получение данных динамичской группы (login = {0}, campaignId = {1}")
    public EditDynamicAdGroupsResponse getEditDynamicAdGroups(String login, Long campaignId, Long... groupIds) {
        String ids = Stream.of(groupIds)
                .map(String::valueOf)
                .collect(joining(","));

        EditDynamicAdGroupsRequest request = new EditDynamicAdGroupsRequest();
        request.setAdGroupIds(ids);
        request.setBannerStatus("all");
        request.setCid(campaignId);
        request.setUlogin(login);
        return getEditDynamicAdGroups(request);
    }

    @Step("Получение данных новой ГО группы для кампании cid: {0} пользователя: {1}")
    public ShowCampMultiEditResponse getAddMcBannerGroups(Long cid, String ulogin) {
        GroupsParameters groupsParameters = new GroupsParameters();
        groupsParameters.setCid(cid.toString());
        groupsParameters.setNewGroup("1");
        groupsParameters.setUlogin(ulogin);
        return getAddMcBannerGroups(groupsParameters);
    }

    @Step("Получение данных ГО группы")
    public ShowCampMultiEditResponse getAddMcBannerGroups(GroupsParameters parameters) {
        return get(CMD.ADD_MCBANNER_AD_GROUPS, parameters, ShowCampMultiEditResponse.class);
    }

    @Step("Сохраняем ДМО группу")
    public JsonRedirectResponse postSavePerformanceAdGroups(GroupsParameters params) {
        JsonRedirectResponse redirectResponse =
                post(CMD.SAVE_PERFORMANCE_AD_GROUPS, params, JsonRedirectResponse.class);
        if (redirectResponse.getResult() == null) {
            throw new DirectCmdStepsException("Error while saving group");
        }
        return redirectResponse;
    }

    @Step("Сохраняем ДМО группу с невалидными данными")
    public GroupErrorsResponse postSavePerformanceAdGroupsErrorResponse(GroupsParameters params) {
        return post(CMD.SAVE_PERFORMANCE_AD_GROUPS, params, GroupErrorsResponse.class);
    }

    @Step("Сохраняем мобильную группу")
    public RedirectResponse postSaveMobileAdGroups(GroupsParameters params) {
        return post(CMD.SAVE_MOBILE_ADGROUPS, params, RedirectResponse.class);
    }

    @Step("Сохраняем мобильную группу с невалидными данными")
    public ErrorResponse postSaveMobileAdGroupsInvalidData(GroupsParameters params) {
        return post(CMD.SAVE_MOBILE_ADGROUPS, params, ErrorResponse.class);
    }

    @Step("Сохраняем группу ГО на поиске")
    public JsonRedirectResponse postSaveMcbannerAdGroups(GroupsParameters params) {
        JsonRedirectResponse redirectResponse =
                post(CMD.SAVE_MCBANNER_AD_GROUPS, params, JsonRedirectResponse.class);
        if (redirectResponse.getResult() == null) {
            throw new DirectCmdStepsException("Error while saving group");
        }
        return redirectResponse;
    }

    @Step("Сохраняем динамическую группу")
    public JsonRedirectResponse postSaveDynamicAdGroups(GroupsParameters params) {
        JsonRedirectResponse redirectResponse = post(CMD.SAVE_DYNAMIC_AD_GROUPS, params, JsonRedirectResponse.class);
        if (redirectResponse.getResult() == null) {
            throw new DirectCmdStepsException("Error while saving group");
        }
        return redirectResponse;
    }

    @Step("Сохраняем динамический баннер с невалидными данными")
    public GroupErrorsResponse postSaveDynamicAdGroupsInvalidData(GroupsParameters params) {
        return post(CMD.SAVE_DYNAMIC_AD_GROUPS, params, GroupErrorsResponse.class);
    }

    @Step("Получаем ДМО группу на редактирование")
    public EditAdGroupsPerformanceResponse getEditAdGroupsPerformance(EditAdGroupsPerformanceRequest request) {
        return get(CMD.EDIT_AD_GROUPS_PERFORMANCE, request, EditAdGroupsPerformanceResponse.class);
    }

    public EditAdGroupsPerformanceResponse getEditAdGroupsPerformance(String login, String campaignID,
            String adGroupIds, String bannerStatus, String bids)
    {
        EditAdGroupsPerformanceRequest editAdGroupsPerformanceRequest = new EditAdGroupsPerformanceRequest();
        editAdGroupsPerformanceRequest.setUlogin(login);
        editAdGroupsPerformanceRequest.setCid(campaignID);
        editAdGroupsPerformanceRequest.setAdGroupIds(adGroupIds);
        editAdGroupsPerformanceRequest.setBannerStatus(bannerStatus);
        editAdGroupsPerformanceRequest.setBids(bids);
        return getEditAdGroupsPerformance(editAdGroupsPerformanceRequest);
    }

    public EditAdGroupsPerformanceResponse getEditAdGroupsPerformance(String login, String campaignID,
            String adGroupIds, String bids)
    {
        return getEditAdGroupsPerformance(login, campaignID, adGroupIds, "all", bids);
    }

    public EditAdGroupsPerformanceResponse getAddAdGroupsPerformance(String login, String campaignID) {
        GroupsParameters groupsParameters = new GroupsParameters();
        groupsParameters.setNewGroup("1");
        groupsParameters.setUlogin(login);
        groupsParameters.setCid(campaignID);
        return getAddAdGroupsPerformance(groupsParameters);
    }

    @Step("GET cmd = editAdGroupsMobileContent (Получаем мобильную группу)")
    public EditAdGroupsMobileContentResponse getEditAdGroupsMobileContent(EditAdGroupsMobileContentRequest request) {
        return get(CMD.EDIT_ADGROUPS_MOBILE_CONTENT, request, EditAdGroupsMobileContentResponse.class);
    }

    @Step("Получаем ДМО группу")
    public EditAdGroupsPerformanceResponse getAddAdGroupsPerformance(GroupsParameters request) {
        return get(CMD.ADD_AD_GROUPS_PERFORMANCE, request, EditAdGroupsPerformanceResponse.class);
    }

    @Step("Получаем динамическую группу (контроллер editDynamicAdGroups)")
    public EditDynamicAdGroupsResponse getEditDynamicAdGroups(EditDynamicAdGroupsRequest editDynamicAdGroupsRequest) {
        return get(CMD.EDIT_DYNAMIC_AD_GROUPS, editDynamicAdGroupsRequest, EditDynamicAdGroupsResponse.class);
    }

    @Step("Проверяем параметры динамической группы")
    public void shouldSeeBannerParameters(Group actualGroups,
            Matcher<Group> matcher)
    {
        assertThat("Группы баннеров соответвуют ожиданиям", actualGroups, matcher);
    }

    @Step("POST cmd=sendOptimize (отправляем группу на оптимизацию)")
    public RedirectResponse postSendOptimize(SendOptimizeRequest request) {
        return post(CMD.SEND_OPTIMIZE, request, RedirectResponse.class);
    }

    @Step("POST cmd=sendOptimize (отправляем группу на оптимизацию) "
            + "client: {0}, campaignId: {1}, adGroupId: {2}, requsetId{3}")
    public RedirectResponse postSendOptimize(String client,
            Long campaignId, Long adGroupId,
            Long optimizeRequestID)
    {
        SendOptimizeRequest sendOptimizeParameters = SendOptimizeRequest.create(
                campaignId,
                adGroupId,
                optimizeRequestID).withRulogin(client);
        sendOptimizeParameters.setUlogin(client);
        return postSendOptimize(sendOptimizeParameters);
    }

    @Step("Post cmd=saveAdgroupTags (сохраняем с невалидными данными)")
    public ErrorsResponse postSaveAdgroupTagsInvalidData(SaveAdGroupTagsRequest saveAdGroupTagsRequest) {
        return post(CMD.SAVE_ADGROUP_TAGS, saveAdGroupTagsRequest, ErrorsResponse.class);
    }

    @Step("Добавление баннера в группу {2} кампании {1} клиента {0}")
    public void addBannerToGroup(String ulogin, Long cid, Long groupId, Banner banner) {
        Group group = getGroupSafe(ulogin, cid, groupId);
        group.getBanners().add(banner);

        GroupsParameters params = GroupsParameters.forExistingCamp(ulogin, cid, group);
        postSaveTextAdGroups(params);
    }

    @Step("Получение фраз группы {2} из кампании {1} клиента {0}")
    public List<Phrase> getPhrases(String ulogin, Long cid, Long groupId) {
        return getGroupSafe(ulogin, cid, groupId).getPhrases();
    }

    @Step("Получение групп кампании {1} клиента {0}")
    public List<Group> getGroups(String ulogin, Long cid) {
        try {
            pageSizeSteps().setGroupsOnShowCamp(ulogin, cid, "200");
        } catch (Exception e) {
            //nope
        }
        return campaignSteps().getShowCampMultiEdit(ulogin, cid).getCampaign().getGroups();
    }

    @Step("Получение баннеров клиента {0} из кампании {1} для группы {2}")
    public List<Banner> getBanners(String ulogin, Long cid, Long groupId) {
        return getGroupSafe(ulogin, cid, groupId).getBanners();
    }

    @Step("Получение баннеров клиента {0} из кампании {1} из всех групп")
    public List<Banner> getBanners(String ulogin, Long cid) {
        List<Banner> result = new ArrayList<>();
        for (Group group : getGroups(ulogin, cid)) {
            result.addAll(group.getBanners());
        }
        return result;
    }

    @Step("Получение баннера клиента {0} из кампании {1} по id {2}")
    public Banner getBanner(String ulogin, Long cid, Long bid) {
        return getBanners(ulogin, cid).stream()
                .filter(b -> b.getBid().equals(bid))
                .findFirst()
                .orElse(null);
    }

    @Step("Получение баннера клиента {0} из кампании {1} по id {2}")
    public Banner getBannerSafe(String ulogin, Long cid, Long bid) {
        Banner banner = getBanner(ulogin, cid, bid);
        if (banner == null) {
            throw new DirectCmdStepsException("Баннер " + bid + "не найден в кампании" + cid);
        } else {
            return banner;
        }
    }

    @Step("Получение группы клиента {0} из кампании {1} по id {2}")
    public Group getGroup(String ulogin, Long cid, Long groupId) {
        return getGroups(ulogin, cid).stream()
                .filter(g -> g.getAdGroupID().equals(groupId.toString()))
                .findFirst()
                .orElse(null);
    }

    @Step("Получение группы клиента {0} из кампании {1} по id {2}")
    public Group getGroupSafe(String ulogin, Long cid, Long groupId) {
        Group group = getGroup(ulogin, cid, groupId);
        if (group == null) {
            throw new DirectCmdStepsException("Группа " + groupId + " не найдена в кампании " + cid);
        } else {
            return group;
        }
    }


    protected CampaignSteps campaignSteps() {
        return getInstance(CampaignSteps.class, getContext());
    }

    protected PageSizeSteps pageSizeSteps() {
        return getInstance(PageSizeSteps.class, getContext());
    }
}
