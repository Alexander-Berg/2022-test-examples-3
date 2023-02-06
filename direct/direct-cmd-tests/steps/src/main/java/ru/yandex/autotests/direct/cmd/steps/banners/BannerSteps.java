package ru.yandex.autotests.direct.cmd.steps.banners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.banners.AdWarningFlag;
import ru.yandex.autotests.direct.cmd.data.banners.ArchiveBannerRequest;
import ru.yandex.autotests.direct.cmd.data.banners.BannerStatuses;
import ru.yandex.autotests.direct.cmd.data.banners.ChangeFlagsAjaxRequest;
import ru.yandex.autotests.direct.cmd.data.banners.DeleteBannerRequest;
import ru.yandex.autotests.direct.cmd.data.banners.SendModerateRequest;
import ru.yandex.autotests.direct.cmd.data.banners.SetBannersStatusesRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.utils.model.PerlBoolean;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class BannerSteps extends DirectBackEndSteps {

    @Step("GET cmd = delBanner (Удаление баннера)")
    public CommonResponse getDeleteBanner(DeleteBannerRequest request) {
        return get(CMD.DEL_BANNER, request, CommonResponse.class);
    }

    @Step("Удаление баннера у клиента {3} cid {0} adgroupIds {1} bid {2}")
    public CommonResponse deleteBanner(String cid, String adgroupIds, String bid, String ulogin) {
        return getDeleteBanner(new DeleteBannerRequest()
                .withDeleteWholeGroup("0")
                .withCid(cid)
                .withAdgroupIds(adgroupIds)
                .withBid(bid)
                .withUlogin(ulogin));
    }

    @Step("Добавление флага предупреждений на баннере")
    public void addBannerFlag(Long bid, AdWarningFlag flag) {
        changeBannerFlagSafe(new ChangeFlagsAjaxRequest().withBid(bid.toString()).addFlag(flag));
    }

    @Step("Удаление флага предупреждений на баннере")
    public void removeBannerFlag(Long bid, AdWarningFlag flag) {
        changeBannerFlagSafe(new ChangeFlagsAjaxRequest().withBid(bid.toString()).removeFlag(flag));
    }

    @Step("Изменение флага предупреждений на баннере")
    public void changeBannerFlagSafe(ChangeFlagsAjaxRequest request) {
        CommonResponse response = changeBannerFlag(request);
        assumeThat("в ответе success 1", response.getSuccess(), equalTo(PerlBoolean.ONE.toString()));
    }

    @Step("Изменение флага предупреждений на баннере")
    public CommonResponse changeBannerFlag(ChangeFlagsAjaxRequest request) {
        return get(CMD.CHANGE_FLAGS_AJAX, request, CommonResponse.class);
    }

    @Step("Установка статусов баннеров группы {0}")
    public CommonResponse setBannersStatusShow(Long groupId, List<Long> bids, PerlBoolean status) {
        Map<String, BannerStatuses> statusesMap = new HashMap<>();
        BannerStatuses statuses = new BannerStatuses().withStatusShow(status.toString());
        for (Long bid : bids) {
            statusesMap.put(bid.toString(), statuses);
        }
        SetBannersStatusesRequest request = new SetBannersStatusesRequest()
                .withAdGroupId(groupId.toString())
                .withStatuses(statusesMap);
        CommonResponse response = postSetBannerStatuses(request);

        assumeThat("статусы баннера установлены успешно", response.getSuccess(), equalTo("1"));
        return response;
    }

    @Step("Установка статусов баннера")
    public CommonResponse postSetBannerStatuses(SetBannersStatusesRequest request) {
        return post(CMD.SET_BANNERS_STATUSES, request, CommonResponse.class);
    }

    @Step("Архивация баннера {3} в кампании {1} для клиента {0}")
    public CommonResponse archiveBanner(String ulogin, Long cid, Long groupId, Long bannerId) {
        CommonResponse response = postArchiveBanner(new ArchiveBannerRequest()
                .withAdGroupIds(groupId.toString())
                .withBid(bannerId.toString())
                .withCid(cid.toString())
                .withUlogin(ulogin));
        assumeThat("баннер успешно заархиварован", response.getStatus(), equalTo("success"));
        return response;
    }

    @Step("Архивация баннера")
    public CommonResponse postArchiveBanner(ArchiveBannerRequest request) {
        return post(CMD.ARCHIVE_BANNER, request, CommonResponse.class);
    }

    @Step("Разархивация баннера {3} в кампании {1} для клиента {0}")
    public CommonResponse unarchiveBanner(String ulogin, Long cid, Long groupId, Long bannerId) {
        CommonResponse response = postUnarchiveBanner(new ArchiveBannerRequest()
                .withAdGroupIds(groupId.toString())
                .withBid(bannerId.toString())
                .withCid(cid.toString())
                .withUlogin(ulogin));
        assumeThat("баннер успешно разархиварован", response.getStatus(), equalTo("success"));
        return response;
    }

    @Step("Разархивация баннера")
    public CommonResponse postUnarchiveBanner(ArchiveBannerRequest request) {
        return post(CMD.UNARCHIVE_BANNER, request, CommonResponse.class);
    }

    @Step("Отправка на модерацию баннера {3} в кампании {1} для клиента {0}")
    public CommonResponse sendModerate(String ulogin, Long campaignId, Long groupId, Long bannerId) {
        return getSendModerate(
                new SendModerateRequest()
                        .withBannerId(bannerId)
                        .withGroupId(groupId)
                        .withCampaignId(campaignId)
                        .withUlogin(ulogin));
    }


    @Step("Отправка на модерацию баннера")
    public CommonResponse getSendModerate(SendModerateRequest request) {
        return get(CMD.SEND_MODERATE, request, CommonResponse.class);
    }

}
