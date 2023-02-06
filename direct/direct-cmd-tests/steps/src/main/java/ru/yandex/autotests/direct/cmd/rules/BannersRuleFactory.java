package ru.yandex.autotests.direct.cmd.rules;

import com.google.common.collect.ImmutableMap;

import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;

public class BannersRuleFactory {

    private enum BannerRuleType {
        TEXT {
            @Override
            public BannersRule getBannersRule() {
                return new TextBannersRule();
            }
        },
        DYNAMIC {
            @Override
            public BannersRule getBannersRule() {
                return new DynamicBannersRule();
            }
        },
        PERFORMANCE {
            @Override
            public BannersRule getBannersRule() {
                return new PerformanceBannersRule();
            }
        },
        MOBILE {
            @Override
            public BannersRule getBannersRule() {
                return new MobileBannersRule();
            }
        },
        MCBANNER {
            @Override
            public BannersRule getBannersRule() { return new MCBannerRule(); }
        };

        private static ImmutableMap<CampaignTypeEnum, BannerRuleType> lookup;
        static {
            ImmutableMap.Builder<CampaignTypeEnum, BannerRuleType> builder = ImmutableMap.builder();
            builder.put(CampaignTypeEnum.TEXT, BannerRuleType.TEXT);
            builder.put(CampaignTypeEnum.DTO, BannerRuleType.DYNAMIC);
            builder.put(CampaignTypeEnum.DMO, BannerRuleType.PERFORMANCE);
            builder.put(CampaignTypeEnum.MOBILE, BannerRuleType.MOBILE);
            builder.put(CampaignTypeEnum.MCBANNER, BannerRuleType.MCBANNER);
            lookup = builder.build();
        }

        public static BannerRuleType findBannerRuleTypeByCampType(CampaignTypeEnum campType) {
            return lookup.get(campType);
        }

        public abstract BannersRule getBannersRule();
    }

    public static BannersRule getBannersRuleBuilderByCampType(CampaignTypeEnum campType) {
        BannerRuleType ruleType = BannerRuleType.findBannerRuleTypeByCampType(campType);
        if (ruleType == null) {
            throw new IllegalStateException("не найдена рула создания баннеров для кампании типа " + campType);
        }
        return ruleType.getBannersRule();
    }
}
