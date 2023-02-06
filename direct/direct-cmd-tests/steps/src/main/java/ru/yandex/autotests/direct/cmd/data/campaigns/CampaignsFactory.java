package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.direct.cmd.data.CmdBeansMaps;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;

public class CampaignsFactory {
    public static SaveCampRequest getCampWithSpecailRegion(CampaignTypeEnum mediaType) {
        SaveCampRequest saveCampRequest = BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_TEMPLATE.get(mediaType), SaveCampRequest.class);
        return saveCampRequest;
    }

    public static SaveCampRequest getCampWithRegionAllWorld(CampaignTypeEnum mediaType) {
        SaveCampRequest saveCampRequest = BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_TEMPLATE.get(mediaType), SaveCampRequest.class);
        return saveCampRequest;
    }

    public static SaveCampRequest getCampWithPriceCoefficientZeroPercentTest(CampaignTypeEnum mediaType) {
        SaveCampRequest saveCampRequest = BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_TEMPLATE.get(mediaType), SaveCampRequest.class);
        return saveCampRequest;
    }
    public static SaveCampRequest getCampWithDisableBehaviorPercentTest(CampaignTypeEnum mediaType) {
        SaveCampRequest saveCampRequest = BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_TEMPLATE.get(mediaType), SaveCampRequest.class);
        return saveCampRequest;
    }


    public static SaveCampRequest getCampWithRelatedKeywordsTest(CampaignTypeEnum mediaType) {
        SaveCampRequest saveCampRequest = BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_TEMPLATE.get(mediaType), SaveCampRequest.class);
        return saveCampRequest;
    }

    public static SaveCampRequest getCampWithEasyInterface(CampaignTypeEnum mediaType) {
        SaveCampRequest saveCampRequest = BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_TEMPLATE.get(mediaType), SaveCampRequest.class);
        return saveCampRequest;
    }

    public static SaveCampRequest getCampWithStrategyBlock(CampaignTypeEnum mediaType) {
        SaveCampRequest saveCampRequest = BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_TEMPLATE.get(mediaType), SaveCampRequest.class);
        return saveCampRequest;
    }
}
