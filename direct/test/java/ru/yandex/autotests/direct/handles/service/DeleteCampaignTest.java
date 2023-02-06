package ru.yandex.autotests.direct.handles.service;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.autotests.direct.handles.beans.DeleteCampaignsBean;

public class DeleteCampaignTest {
    @Test
    @Ignore("поставить нужный айди кампании и проверить")
    public void deleteCampaignsByID() {
        DeleteCampaignsService deleteCampaignsService = new DeleteCampaignsService();
        DeleteCampaignsBean deleteCampaignsBean = new DeleteCampaignsBean();
        deleteCampaignsBean.setCampaigns("79249118");
        deleteCampaignsBean.setStage("TS");
        deleteCampaignsService.deleteCampaignsByID(deleteCampaignsBean);
    }

    @Test
    @Ignore("поставить нужный login и проверить")
    public void deleteCampaignsByLogin() {
        DeleteCampaignsService deleteCampaignsService = new DeleteCampaignsService();
        DeleteCampaignsBean deleteCampaignsBean = new DeleteCampaignsBean();
        deleteCampaignsBean.setStage("TS");
        deleteCampaignsBean.setLogin("vananos-direct-tester-1");
        deleteCampaignsService.deleteCampaignsByLogin(deleteCampaignsBean);
    }


}
