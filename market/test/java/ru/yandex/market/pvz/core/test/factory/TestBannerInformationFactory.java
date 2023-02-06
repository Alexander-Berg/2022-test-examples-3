package ru.yandex.market.pvz.core.test.factory;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.banner.DisplayType;
import ru.yandex.market.pvz.core.domain.banner.MessageType;
import ru.yandex.market.pvz.core.domain.banner_information.BannerInformation;
import ru.yandex.market.pvz.core.domain.banner_information.BannerInformationCommandService;
import ru.yandex.market.pvz.core.domain.banner_information.BannerInformationParams;
import ru.yandex.market.pvz.core.domain.banner_information.BannerInformationParamsMapper;
import ru.yandex.market.pvz.core.domain.banner_information.CampaignType;
import ru.yandex.market.pvz.core.test.factory.mapper.BannerInformationTestParamsMapper;

public class TestBannerInformationFactory {

    @Autowired
    private BannerInformationCommandService bannerInformationCommandService;

    @Autowired
    private BannerInformationTestParamsMapper bannerInformationTestParamsMapper;

    @Autowired
    private BannerInformationParamsMapper bannerInformationParamsMapper;

    public BannerInformationParams create(BannerInformationTestParams params) {
        BannerInformationParams banner = bannerInformationTestParamsMapper.map(params);
        return bannerInformationCommandService.create(banner);
    }

    public BannerInformationParams createStartShiftBanner() {
        return create(
                TestBannerInformationFactory.BannerInformationTestParams.builder()
                        .closable(true)
                        .frequency("0 0 8 * * ? *")
                        .durationInMinutes(1440)
                        .campaignType(CampaignType.TPL_OUTLET)
                        .messageType(MessageType.START_SHIFT.name())
                        .displayType(DisplayType.POPUP)
                        .build()
        );
    }

    public BannerInformationParams createIndebtednessBanner(CampaignType campaignType) {
        return create(
                TestBannerInformationFactory.BannerInformationTestParams.builder()
                        .closable(false)
                        .frequency(null)
                        .durationInMinutes(null)
                        .campaignType(campaignType)
                        .messageType(MessageType.INDEBTEDNESS.name())
                        .displayType(DisplayType.BANNER)
                        .buttonText(null)
                        .build()
        );
    }

    public BannerInformationParams createDeactivationBanner(MessageType messageType) {
        return create(
                TestBannerInformationFactory.BannerInformationTestParams.builder()
                        .closable(true)
                        .frequency("0 0 0/3 * * ? *")
                        .durationInMinutes(180)
                        .campaignType(CampaignType.TPL_PARTNER)
                        .messageType(messageType.name())
                        .displayType(DisplayType.BANNER)
                        .buttonText(null)
                        .body("Отключены ПВЗ: %s")
                        .build()
        );
    }

    public BannerInformationParams createNewPartnerSurveyBanner(MessageType messageType) {
        return create(
                TestBannerInformationFactory.BannerInformationTestParams.builder()
                        .closable(true)
                        .frequency("0 0 0/3 * * ? *")
                        .durationInMinutes(180)
                        .campaignType(CampaignType.TPL_PARTNER)
                        .messageType(messageType.name())
                        .displayType(DisplayType.BANNER)
                        .buttonText(null)
                        .body("Отключены ПВЗ: %s")
                        .build()
        );
    }

    public BannerInformationParams createDebtDisabledBanner(CampaignType campaignType) {
        return create(
                TestBannerInformationFactory.BannerInformationTestParams.builder()
                        .closable(false)
                        .frequency(null)
                        .durationInMinutes(null)
                        .campaignType(campaignType)
                        .messageType(MessageType.DISABLED.name())
                        .displayType(DisplayType.BANNER)
                        .buttonText(null)
                        .build()
        );
    }

    public BannerInformationParams parseFrequency(BannerInformationParams params, OffsetDateTime now) {
        BannerInformation banner = bannerInformationParamsMapper.map(params);
        banner.parseFrequencyExpression(now);
        BannerInformationParams bannerInformationParams = bannerInformationParamsMapper.map(banner);
        bannerInformationParams.setId(params.getId());
        return bannerInformationParams;
    }

    @Data
    @Builder
    public static class BannerInformationTestParams {

        public static final String DEFAULT_MESSAGE_TYPE = "Тип сообщения";
        public static final String DEFAULT_TITLE = "Заголовок";
        public static final String DEFAULT_BODY = "Тело с шаблоном под задолженность %.2f";
        public static final String DEFAULT_BUTTON_TEXT = "Текст кнопки";
        public static final String DEFAULT_COLOR = "#ffe0e0";
        public static final DisplayType DEFAULT_DISPLAY_TYPE = DisplayType.BANNER;
        public static final LocalDate DEFAULT_START_DATE = LocalDate.of(1990, 1, 1);
        public static final LocalDate DEFAULT_END_DATE = LocalDate.of(2100, 1, 1);
        public static final CampaignType DEFAULT_CAMPAIGN_TYPE = CampaignType.TPL_OUTLET;

        @Builder.Default
        private String messageType = DEFAULT_MESSAGE_TYPE;

        @Builder.Default
        private String title = DEFAULT_TITLE;

        @Builder.Default
        private String body = DEFAULT_BODY;

        @Builder.Default
        private String buttonText = DEFAULT_BUTTON_TEXT;

        @Builder.Default
        private String color = DEFAULT_COLOR;

        private List<Long> bannerPageIds;

        private boolean closable;

        @Builder.Default
        private DisplayType displayType = DEFAULT_DISPLAY_TYPE;

        private String frequency;

        private Integer durationInMinutes;

        @Builder.Default
        private LocalDate startDate = DEFAULT_START_DATE;

        @Builder.Default
        private LocalDate endDate = DEFAULT_END_DATE;

        @Builder.Default
        private CampaignType campaignType = DEFAULT_CAMPAIGN_TYPE;

        private List<String> bannerCampaignFeatures;

        private List<Long> campaignIds;

        private String notificationText;
    }
}
