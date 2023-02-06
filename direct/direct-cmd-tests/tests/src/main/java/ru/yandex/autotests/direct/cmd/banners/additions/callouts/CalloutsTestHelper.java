package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.editdynamicadgroups.EditDynamicAdGroupsResponse;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.model.User;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/*
* todo javadoc
*/
public class CalloutsTestHelper {

    public static final Integer MAX_CALLOUT_LENGTH = 25;
    public static final Integer MAX_CALLOUTS_FOR_BANNER = 50;
    public static final Integer MAX_CALLOUTS_FOR_CLIENT = 1000;

    private String ulogin;
    private DirectCmdSteps steps;
    private String cid;

    public CalloutsTestHelper(String ulogin, DirectCmdSteps steps, String cid) {
        this.ulogin = ulogin;
        this.steps = steps;
        this.cid = cid;
    }

    public void overrideCid(String cid) {
        this.cid = cid;
    }

    public void clearCalloutsForClient() {
        String clientIdStr = User.get(ulogin).getClientID();
        assumeThat("Клиент есть в базе", clientIdStr, notNullValue());
        Long clientId = Long.valueOf(clientIdStr);
        TestEnvironment.newDbSteps().useShardForLogin(ulogin).bannerAdditionsSteps().clearCalloutsForClient(clientId);
    }

    public Group getDynamicFirstGroup() {
        Banner banner = getFirstBanner();

        EditDynamicAdGroupsResponse groupResp =
                steps.groupsSteps().getEditDynamicAdGroups(ulogin, banner.getCid(), banner.getAdGroupId());

        return Optional.ofNullable(groupResp.getCampaign())
                .orElseThrow(() -> new DirectCmdStepsException("Не удалось получить параметры кампании " + cid))
                .getGroups()
                .stream()
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("Группа не найдена"));
    }

    public Group getFirstGroup() {
        Banner banner = getFirstBanner();

        ShowCampMultiEditRequest request = ShowCampMultiEditRequest.forSingleBanner(ulogin, Long.valueOf(cid),
                banner.getAdGroupId(), banner.getBid());

        ShowCampMultiEditResponse groupResp = steps.campaignSteps().getShowCampMultiEdit(request);

        Group firstGroup = Optional.ofNullable(groupResp.getCampaign())
                .orElseThrow(() -> new DirectCmdStepsException("Не удалось получить параметры кампании " + cid))
                .getGroups()
                .stream()
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("Группа не найдена"));
        if (firstGroup.getRetargetings() == null) firstGroup.setRetargetings(Collections.emptyList());
        firstGroup.setTags(new Object());
        return firstGroup;
    }

    public Banner getFirstBanner() {
        return steps.campaignSteps().getShowCamp(ulogin, cid).getGroups().get(0);
    }

    public Group existingGroupAndSet(String... callouts) {
        Group group = getFirstGroup();
        addCalloutsToGroup(group, callouts);
        return group;
    }

    public Group existingDynamicGroupAndSet(String... callouts) {
        Group group = getDynamicFirstGroup();
        group.setTags(new Object[]{});
        group.setHrefParams("");

        group.getBanners().get(0).getContactInfo().setOGRN(
                group.getBanners().get(0).getContactInfo().getOrgDetails().getOGRN()
        );
        group.getBanners().get(0).setHashFlags(null);
        group.getBanners().get(0).setImage("");

        addCalloutsToGroup(group, callouts);
        return group;
    }

    public Group newDynamicGroupAndSet(String... callouts) {
        Group group = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_DYNAMIC_DEFAULT2, Group.class);
        addCalloutsToGroup(group, callouts);
        return group;
    }

    public Group newGroupAndSet(String... callouts) {
        Group group = GroupsFactory.getDefaultTextGroup();
        addCalloutsToGroup(group, callouts);
        return group;
    }

    public void addCalloutsToGroup(Group group, String... callouts) {
        List<Callout> toSave = Stream.of(callouts)
                .map(c -> new Callout().withCalloutText(c))
                .collect(toList());

        for (Banner banner : group.getBanners()) {
            banner.withCallouts(toSave);
        }
    }

    public GroupsParameters getRequestFor(Group group) {
        return GroupsParameters.forNewCamp(
                ulogin, Long.valueOf(cid), group);
    }

    public GroupsParameters getRequestForDynamic(Group group) {
        return GroupsParameters.forExistingCamp(ulogin, Long.valueOf(cid), group);
    }

    public void saveCalloutsForDynamic(GroupsParameters request) {
        String cid = steps.groupsSteps()
                .postSaveDynamicAdGroups(request).getLocationParam(LocationParam.CID);

        assumeThat("группы в кампании сохранились", cid, equalTo(this.cid));
    }

    public void saveCallouts(GroupsParameters request) {
        steps.groupsSteps().postSaveTextAdGroups(request).getLocationParam(LocationParam.CID);
    }

    public List<String> getCalloutsList(ShowCampResponse response) {
        List<Callout> callouts = Optional.ofNullable(response.getGroups())
                .orElseThrow(() -> new DirectCmdStepsException("В ответе нет групп"))
                .stream()
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("В ответе группы пустые"))
                .getCallouts();
        return Optional.ofNullable(callouts)
                .orElseThrow(() -> new DirectCmdStepsException("В первой группе отсутствуют дополнения"))
                .stream()
                .map(Callout::getCalloutText)
                .collect(Collectors.toList());
    }

    public List<String> getCalloutsList(Campaign response) {
        List<Callout> callouts = Optional.ofNullable(response.getGroups())
                .orElseThrow(() -> new DirectCmdStepsException("В ответе нет групп"))
                .stream()
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("В ответе группы пустые"))
                .getBanners()
                .stream()
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("В ответев первой группе баннеры пустые"))
                .getCallouts();
        return Optional.ofNullable(callouts)
                .orElseThrow(() -> new DirectCmdStepsException("В первой группе отсутствуют дополнения"))
                .stream()
                .map(Callout::getCalloutText)
                .collect(Collectors.toList());
    }

    public List<String> getCalloutsList(ShowCampMultiEditResponse response) {
        Campaign camp = Optional.ofNullable(response.getCampaign())
                .orElseThrow(() -> new DirectCmdStepsException("В ответе нет кампании"));
        Group group = Optional.ofNullable(camp.getGroups())
                .orElseThrow(() -> new DirectCmdStepsException("В ответе нет групп"))
                .stream()
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("В ответе группы пустые"));
        List<Callout> callouts = Optional.ofNullable(group.getBanners())
                .orElseThrow(() -> new DirectCmdStepsException("В ответе нет баннеров"))
                .stream()
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("В ответе нет баннеров"))
                .getCallouts();

        return Optional.ofNullable(callouts)
                .orElseThrow(() -> new DirectCmdStepsException("В первой группе отсутствуют дополнения"))
                .stream()
                .map(Callout::getCalloutText)
                .collect(Collectors.toList());
    }
}
