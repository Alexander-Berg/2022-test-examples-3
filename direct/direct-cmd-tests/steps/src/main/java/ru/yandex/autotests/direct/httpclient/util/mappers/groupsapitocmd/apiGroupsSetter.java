package ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd;

import ru.yandex.autotests.direct.httpclient.data.CmdBeans.*;
import ru.yandex.autotests.direct.httpclient.data.banners.lite.EditBannerEasyResponseBean;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.BeanMapper;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.common.api45.BannerInfo;
import ru.yandex.autotests.directapi.common.api45.BannerPhraseInfo;
import ru.yandex.autotests.directapi.common.api45.CampaignStrategy;

import java.util.*;
import java.util.stream.Stream;

import static ch.lambdaj.Lambda.*;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created by shmykov on 14.04.15.
 */
public class apiGroupsSetter {

    public static void setLiteGroupsFromApiBanner(EditBannerEasyResponseBean responseBean, BannerInfo banner) {
        setLiteGroupsFromApiBanner(responseBean, banner, null);
    }

    public static void setLiteGroupsFromApiBanner(EditBannerEasyResponseBean responseBean, BannerInfo banner, Currency currency) {
        responseBean.setGroups(new ArrayList<LightGroupCmdBean>());
        List<LightGroupCmdBean> groups = responseBean.getGroups();
        groups.add(convertApiBannerToLightGroupCmdBean(banner, currency));
    }

    public static LightGroupCmdBean convertApiBannerToLightGroupCmdBean(BannerInfo banner) {
        return convertApiBannerToLightGroupCmdBean(banner, null);
    }

    public static LightGroupCmdBean convertApiBannerToLightGroupCmdBean(BannerInfo banner, Currency currency) {
        LightGroupCmdBean group = BeanMapper.map(banner, LightGroupCmdBean.class, new GroupApiToCmdBeanMappingBuilder());
        group.setBanners(new ArrayList<BannerCmdBean>());
        group.getBanners().add((BeanMapper.map(banner, BannerCmdBean.class, new BannerApiToCmdBeanMappingBuilder())));
        BannerCmdBean bannerCmd = group.getBanners().get(0);
        group.setFirstAvailableBid(String.valueOf(banner.getBannerID()));
        //в легком этих полей нет
        bannerCmd.setStatusShow(null);
        bannerCmd.setStatusBannerModerate(null);
        bannerCmd.setSitelinks(null);
        group.setPhrases(new ArrayList<PhraseCmdBean>());
        for (BannerPhraseInfo phrase : banner.getPhrases()) {
            group.getPhrases().add((BeanMapper.map(phrase, PhraseCmdBean.class, new PhraseApiToCmdBeanMappingBuilder(currency))));
        }
        return group;
    }

    public static void addBudgetInfoFromApiStrategy(LightGroupCmdBean lightGroup, CampaignStrategy strategy, Currency currency) {
        BeanMapper.map(strategy, lightGroup, new CampaignStrategyToBudgetMappingBuilder(currency));

    }

    public static void setGroupsFromBanners(GroupsCmdBean groups, BannerInfo... banners) {
        setGroupsFromBanners(groups, null, banners);
    }

    public static void setGroupsFromBanners(GroupsCmdBean groups, Currency currency, BannerInfo... banners) {
        Collection<Long> adGroupIds = sort(selectDistinct(extract(banners, on(BannerInfo.class).getAdGroupID())), on(Long.class).longValue());
        if (groups.getGroups() == null) {
            groups.setGroups(new ArrayList<GroupCmdBean>());
        }
        for (Long adGroupId : adGroupIds) {
            GroupCmdBean group = convertApiBannersToGroup(filter(having(on(BannerInfo.class).getAdGroupID(), equalTo(adGroupId)), banners), currency);
            if (group.getMinusKeywords() == null) group.setMinusKeywords(Collections.emptyList());
            
            group.getBanners().stream().filter(t -> t.getSitelinks().size() < 4)
                    .forEach(t -> t.getSitelinks().addAll(getAdditionalSiteLinks(4 - t.getSitelinks().size())));
            group.setTags(emptyMap());
            group.setRetargetings(Collections.emptyList());
            groups.getGroups().add(group);
        }
    }

    public static List<SiteLinksCmdBean> getAdditionalSiteLinks(int size) {
        List<SiteLinksCmdBean> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(SiteLinksCmdBean.getEmptySiteLink());
        }
        return result;
    }

    private static GroupCmdBean convertApiBannersToGroup(List<BannerInfo> banners, Currency clientCurrency) {
        GroupCmdBean group = BeanMapper.map(banners.get(0), GroupCmdBean.class, new GroupApiToCmdBeanMappingBuilder(clientCurrency));
        group.setBanners(new ArrayList<BannerCmdBean>());
        for (BannerInfo banner : banners) {
            group.getBanners().add((BeanMapper.map(banner, BannerCmdBean.class, new BannerApiToCmdBeanMappingBuilder())));

        }
        group.setPhrases(new ArrayList<PhraseCmdBean>());
        List<BannerPhraseInfo> phrases = new ArrayList<>();
        for (BannerInfo banner : banners) {
            phrases.addAll(Arrays.asList(banner.getPhrases()));
        }
        for (Object phrase : selectDistinct(phrases, "phraseID")) {
            group.getPhrases().add((BeanMapper.map(phrase, PhraseCmdBean.class, new PhraseApiToCmdBeanMappingBuilder(clientCurrency))));
        }
        return group;
    }
}