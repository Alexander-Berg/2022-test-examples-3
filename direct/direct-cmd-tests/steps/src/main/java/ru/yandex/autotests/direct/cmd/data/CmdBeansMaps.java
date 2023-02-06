package ru.yandex.autotests.direct.cmd.data;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;

public class CmdBeansMaps {
    public static final Map<CampaignTypeEnum, String> MEDIA_TYPE_TO_TEMPLATE = ImmutableMap.<CampaignTypeEnum, String>builder()
            .put(CampaignTypeEnum.DTO, CmdBeans.SAVE_NEW_DYNAMIC_CAMP_FULL)
            .put(CampaignTypeEnum.DMO, CmdBeans.SAVE_NEW_PERFORMANCE_CAMP_DEFAULT)
            .put(CampaignTypeEnum.MOBILE, CmdBeans.SAVE_NEW_MOBILE_CAMP_DEFAULT_NEW)
            .put(CampaignTypeEnum.TEXT, CmdBeans.SAVE_NEW_TEXT_CAMP_DEFAULT)
            .put(CampaignTypeEnum.MCBANNER, CmdBeans.SAVE_NEW_MCBANNER_CAMP_DEFAULT)
            .put(CampaignTypeEnum.CPM_BANNER, CmdBeans.SAVE_NEW_CPM_CAMP_DEFAULT)
            .put(CampaignTypeEnum.CPM_DEALS, CmdBeans.SAVE_NEW_CPM_DEALS_CAMP_DEFAULT)
            .put(CampaignTypeEnum.INTERNAL_FREE, CmdBeans.SAVE_NEW_INTERNAL_FREE_CAMP_DEFAULT)
            .put(CampaignTypeEnum.INTERNAL_DISTRIB, CmdBeans.SAVE_NEW_INTERNAL_DISTRIB_CAMP_DEFAULT)
            .put(CampaignTypeEnum.INTERNAL_AUTOBUDGET, CmdBeans.SAVE_NEW_INTERNAL_AUTOBUDBET_CAMP_DEFAULT)
            .build();

    public static final Map<CampaignTypeEnum, String> MEDIA_TYPE_TO_GROUP_TEMPLATE = ImmutableMap.<CampaignTypeEnum, String>builder()
            .put(CampaignTypeEnum.DTO, CmdBeans.COMMON_REQUEST_GROUP_DYNAMIC_DEFAULT2)
            .put(CampaignTypeEnum.DMO, CmdBeans.COMMON_REQUEST_GROUP_PERFORMANCE_DEFAULT2)
            .put(CampaignTypeEnum.MOBILE, CmdBeans.COMMON_REQUEST_GROUP_MOBILE_DEFAULT2)
            .put(CampaignTypeEnum.TEXT, CmdBeans.COMMON_REQUEST_GROUP_TEXT_DEFAULT2)
            .build();
}
