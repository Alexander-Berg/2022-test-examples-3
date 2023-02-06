package ru.yandex.autotests.direct.cmd.steps;

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum;
import com.yandex.direct.api.v5.campaigns.CampaignGetItem;

import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.BaseSteps;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.CampaignFakeInfo;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.model.api5.campaigns.CampaignsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.campaigns.GetRequestMap;
import ru.yandex.autotests.directapi.steps.UserSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.stream.Collectors.toList;

public class ApiAggregationSteps extends BaseSteps<UserSteps> {

    private UserSteps apiSteps() {
        return getContext();
    }

    @Step("Получение всех кампаний клиента {0}")
    public Long[] getAllCampaigns(String ulogin) {
        return apiSteps().campaignSteps()
                .getCampaigns(ulogin, new GetRequestMap()
                        .withSelectionCriteria(new CampaignsSelectionCriteriaMap())
                        .withFieldNames(CampaignFieldEnum.ID))
                .stream()
                .map(CampaignGetItem::getId)
                .collect(toList())
                .toArray(new Long[]{});
    }

    @Step("Удаление всех кампаний клиента {0}")
    public void deleteAllCampaigns(String ulogin) {
        apiSteps().campaignSteps()
                .getCampaigns(ulogin, new GetRequestMap()
                        .withSelectionCriteria(new CampaignsSelectionCriteriaMap())
                        .withFieldNames(CampaignFieldEnum.ID))
                .stream()
                .map(CampaignGetItem::getId)
                .forEach(cid -> deleteActiveCampaignQuietly(ulogin, cid));
    }

    @Step
    // Не работает со смартами
    public void deleteActiveCampaignsQuietly(String ulogin, Long... cids) {
        for (Long cid : cids) {
            deleteActiveCampaignQuietly(ulogin, cid);
        }
    }

    @Step
    // Не работает со смартами
    public void deleteActiveCampaignQuietly(String ulogin, Long cid) {
        try {
            makeCampaignReadyForDelete(cid);
            //этот метод не поглощает исключения, как через 4 апи
            apiSteps().campaignSteps().campaignsDelete(ulogin, cid);
        } catch (Throwable e) {
            System.err.println("Ошибка удаления кампании: " + e);
        }
    }

    @Step
    public void campaignsArchive(String ulogin, Long cid) {
        apiSteps().campaignSteps().campaignsArchive(ulogin, cid);
    }

    @Step
    public void unArchiveCampaign(String ulogin, Long cid) {
        apiSteps().campaignSteps().campaignsUnarchive(ulogin, cid);
    }

    @Step
    public void archiveBanner(String ulogin, Long bannerId) {
        apiSteps().adsSteps().adsArchive(ulogin, bannerId);
    }

    @Step
    public void moderateBanner(String ulogin, Long bannerId) {
        apiSteps().adsSteps().adsModerate(ulogin, bannerId);
    }

    @Step
    public void activateCampaignWithMoney(String ulogin, Long cid, Float money) {
        apiSteps().makeCampaignActiveV5(ulogin, cid);
        apiSteps().campaignFakeSteps().setCampaignSum(cid, money);
    }

    //Отличается от дарксайда отсутствием выставления типа кампании в текст из-за https://st.yandex-team.ru/DIRECT-65218
    @Step("Сбрасываем параметры кампании, чтобы удалить")
    public void makeCampaignReadyForDelete(Long... campaignIDs) {
        for (long campaignID : campaignIDs) {
            CampaignFakeInfo campaign = new CampaignFakeInfo();
            campaign.setCampaignID((int) campaignID);
            campaign.setSum(0f);
            campaign.setSumSpent(0f);
            campaign.setSumToPay(0f);
            campaign.setSumLast(0f);
            campaign.setShows(0);
            campaign.setClicks(0);
            campaign.setOrderID("");
            campaign.setStatusActive(Status.NO);
            campaign.setStatusBsSynced(Status.NO);
            campaign.setStatusModerate(Status.NO);
            campaign.setArchived(Status.NO);
            campaign.setCurrencyConverted(Status.NO);
            apiSteps().campaignFakeSteps().fakeCampaignParams(campaign);

            Integer shard = apiSteps().getDirectJooqDbSteps().shardingSteps().getShardByCidRaw(campaignID);
            if (shard != null) {
                // если кампанию еще не удалили
                DirectJooqDbSteps jooqDbSteps = apiSteps().getDirectJooqDbSteps().useShard(shard);
                jooqDbSteps.bsResyncQueueSteps().deleteCampaignFromBsResyncQueueByCid(campaignID);
                jooqDbSteps.bsExportQueueSteps().deleteBsExportQueue(campaignID);
            }
        }
    }
}
