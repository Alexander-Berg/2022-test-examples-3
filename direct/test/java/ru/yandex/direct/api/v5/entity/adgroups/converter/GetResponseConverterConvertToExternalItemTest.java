package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupGetItem;
import com.yandex.direct.api.v5.adgroups.AdGroupSubtypeEnum;
import com.yandex.direct.api.v5.adgroups.AppAvailabilityStatusEnum;
import com.yandex.direct.api.v5.adgroups.SourceProcessingStatusEnum;
import com.yandex.direct.api.v5.adgroups.SourceTypeGetEnum;
import com.yandex.direct.api.v5.adgroups.TargetCarrierEnum;
import com.yandex.direct.api.v5.adgroups.TargetDeviceTypeEnum;
import com.yandex.direct.api.v5.general.AdGroupTypesEnum;
import com.yandex.direct.api.v5.general.ExtensionModeration;
import com.yandex.direct.api.v5.general.MobileOperatingSystemTypeEnum;
import com.yandex.direct.api.v5.general.ServingStatusEnum;
import com.yandex.direct.api.v5.general.StatusEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.model.StatusIconModerate;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Api5Test
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class GetResponseConverterConvertToExternalItemTest {
    @Autowired
    public GetResponseConverter converter;

    // NB - конвертация части полей тестируется на примере только TextAdGroup потому, что все реализации для
    // конкретных типов групп наследуют реализацию аксессоров/мутаторов из базового абстрактного класса AdGroup

    @Test
    public void convertToExternalItem_idIsConverted() {
        Long id = 1L;

        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withId(id));
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    public void convertToExternalItem_campaignIdIsConverted() {
        Long campaignId = 10L;

        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withCampaignId(campaignId));
        assertThat(result.getCampaignId()).isEqualTo(campaignId);
    }

    @Test
    public void convertToExternalItem_typeIsConverted() {
        AdGroupType type = AdGroupType.BASE;
        AdGroupTypesEnum convertedType = AdGroupTypesEnum.TEXT_AD_GROUP;

        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withType(type));
        assertThat(result.getType()).isEqualTo(convertedType);
    }

    @Test
    public void convertToExternalItem_subtypeIsConverted() {
        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup());
        assertThat(result.getSubtype()).isEqualTo(AdGroupSubtypeEnum.NONE);
    }

    @Test
    public void convertToExternalItem_nameIsConverted_getNotEmptyString() {
        String name = "Adgroup 1";

        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withName(name));
        assertThat(result.getName()).isEqualTo(name);
    }

    @Test
    public void convertToExternalItem_nameIsConverted_getEmptyString() {
        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withName(null));
        assertThat(result.getName()).isEqualTo("");
    }

    @Test
    public void convertToExternalItem_trackingParamsIsConverted_getNotEmptyString() {
        String trackingParams = "from=direct&ad={ad_id}";

        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withTrackingParams(trackingParams));
        assertThat(result.getTrackingParams()).isEqualTo(trackingParams);
    }

    @Test
    public void convertToExternalItem_trackingParamsIsConverted_getEmptyString() {
        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withTrackingParams(null));
        assertThat(result.getTrackingParams()).isEqualTo("");
    }

    @Test
    public void convertToExternalItem_minusKeywordsIsConverted() {
        List<String> minusKeywords = asList("очень", "остроумная", "фраза");

        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withMinusKeywords(minusKeywords));
        assertThat(result.getNegativeKeywords().getValue().getItems())
                .containsExactly(minusKeywords.toArray(new String[0]));
    }

    @Test
    public void convertToExternalItem_regionIdsIsConverted_regionIdsIsNotEmpty() {
        List<Long> regionIds = asList(225L, 213L);

        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withGeo(regionIds));
        assertThat(result.getRegionIds()).isEqualTo(regionIds);
    }

    @Test
    public void convertToExternalItem_regionIdsIsConverted_regionIdsIsEmpty() {
        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withGeo(Collections.emptyList()));
        assertThat(result.getRegionIds()).isEqualTo(Collections.emptyList());
    }

    // TODO: может ли adgroup.getGeo() == null?

    @Test
    public void convertToExternalItem_restrictedGeoIsConverted() {
        List<Long> restrictedGeo = asList(187L, 983L);

        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withRestrictedGeo(restrictedGeo));
        assertThat(result.getRestrictedRegionIds().getValue().getItems())
                .containsExactly(restrictedGeo.toArray(new Long[0]));
    }

    @Test
    public void convertToExternalItem_statusIsConverted() {
        StatusModerate statusModerate = StatusModerate.YES;
        StatusPostModerate statusPostModerate = StatusPostModerate.YES;
        StatusEnum expectedStatus = StatusEnum.ACCEPTED;

        AdGroupGetItem result = converter.convertToExternalItem(
                buidTextAdGroup().withStatusModerate(statusModerate).withStatusPostModerate(statusPostModerate));
        assertThat(result.getStatus()).isEqualTo(expectedStatus);
    }

    @Test
    public void convertToExternalItem_bsRarelyLoadedIsConverted() {
        Boolean bsRarelyLoaded = true;
        ServingStatusEnum expectedStatus = ServingStatusEnum.RARELY_SERVED;

        AdGroupGetItem result = converter.convertToExternalItem(buidTextAdGroup().withBsRarelyLoaded(bsRarelyLoaded));
        assertThat(result.getServingStatus()).isEqualTo(expectedStatus);
    }

    @Test
    public void convertToExternalItem_storeUrlIsConverted() {
        String storeUrl = "storeUrl";

        AdGroupGetItem result = converter.convertToExternalItem(buidMobileAppAdGroup().withStoreUrl(storeUrl));
        assertThat(result.getMobileAppAdGroup().getStoreUrl()).isEqualTo(storeUrl);
    }

    @Test
    public void convertToExternalItem_deviceTypeTargetingIsConverted() {
        Set<MobileContentAdGroupDeviceTypeTargeting> deviceTypeTargeting =
                Collections.singleton(MobileContentAdGroupDeviceTypeTargeting.PHONE);
        List<TargetDeviceTypeEnum> targetDeviceType =
                Collections.singletonList(TargetDeviceTypeEnum.DEVICE_TYPE_MOBILE);

        AdGroupGetItem result = converter.
                convertToExternalItem(buidMobileAppAdGroup().withDeviceTypeTargeting(deviceTypeTargeting));
        assertThat(result.getMobileAppAdGroup().getTargetDeviceType()).isEqualTo(targetDeviceType);
    }

    @Test
    public void convertToExternalItem_targetCarrierIsConverted() {
        Set<MobileContentAdGroupNetworkTargeting> networkTargeting =
                Collections.singleton(MobileContentAdGroupNetworkTargeting.CELLULAR);
        TargetCarrierEnum targetCarrier = TargetCarrierEnum.WI_FI_AND_CELLULAR;

        AdGroupGetItem result = converter.
                convertToExternalItem(buidMobileAppAdGroup().withNetworkTargeting(networkTargeting));
        assertThat(result.getMobileAppAdGroup().getTargetCarrier()).isEqualTo(targetCarrier);
    }

    @Test
    public void convertToExternalItem_appOperatingSystemTypeIsConverted() {
        OsType osType = OsType.ANDROID;
        MobileOperatingSystemTypeEnum expectedType = MobileOperatingSystemTypeEnum.ANDROID;

        MobileContentAdGroup adGroup = buidMobileAppAdGroup();

        adGroup.getMobileContent().withOsType(osType);

        AdGroupGetItem result = converter.convertToExternalItem(adGroup);
        assertThat(result.getMobileAppAdGroup().getAppOperatingSystemType()).isEqualTo(expectedType);
    }

    @Test
    public void convertToExternalItem_minimalOperatingSystemVersionIsConverted() {
        String systemVersion = "System V";

        AdGroupGetItem result = converter
                .convertToExternalItem(buidMobileAppAdGroup().withMinimalOperatingSystemVersion(systemVersion));
        assertThat(result.getMobileAppAdGroup().getTargetOperatingSystemVersion()).isEqualTo(systemVersion);
    }

    @Test
    public void convertToExternalItem_mobileAppIconModerationStatusIsConverted() {
        MobileContent mobileContent = new MobileContent().withIconHash("iconHash").withStatusIconModerate(
                StatusIconModerate.SENDING);
        StatusEnum moderationStatus = StatusEnum.MODERATION;

        AdGroupGetItem result = converter
                .convertToExternalItem(buidMobileAppAdGroup().withMobileContent(mobileContent));
        assertThat(result.getMobileAppAdGroup().getAppIconModeration().getValue())
                .isEqualToComparingFieldByFieldRecursively(
                        new ExtensionModeration().withStatus(moderationStatus).withStatusClarification(""));
    }

    @Test
    public void convertToExternalItem_mobileAppAvailabilityStatusIsConverted() {
        MobileContent mobileContent =
                new MobileContent().withModifyTime(LocalDateTime.now()).withIsAvailable(Boolean.TRUE);
        AppAvailabilityStatusEnum appAvailabilityStatus = AppAvailabilityStatusEnum.AVAILABLE;

        AdGroupGetItem result = converter
                .convertToExternalItem(buidMobileAppAdGroup().withMobileContent(mobileContent));
        assertThat(result.getMobileAppAdGroup().getAppAvailabilityStatus()).isEqualTo(appAvailabilityStatus);
    }

    @Test
    public void convertToExternalItem_domainUrlIsConverted_getNotEmptyString() {
        String url = "https://ya.ru";

        AdGroupGetItem result = converter.convertToExternalItem(buidDynamicTextAdGroup().withDomainUrl(url));
        assertThat(result.getDynamicTextAdGroup().getDomainUrl()).isEqualTo(url);
    }

    @Test
    public void convertToExternalItem_domainUrlIsConverted_getEmptyString() {
        AdGroupGetItem result = converter.convertToExternalItem(buidDynamicTextAdGroup().withDomainUrl(null));
        assertThat(result.getDynamicTextAdGroup().getDomainUrl()).isEqualTo("");
    }

    @Test
    public void convertToExternalItem_domainUrlProcessingStatusIsConverted() {
        var status = StatusBLGenerated.YES;
        SourceProcessingStatusEnum expectedStatus = SourceProcessingStatusEnum.PROCESSED;

        AdGroupGetItem result = converter.convertToExternalItem(buidDynamicTextAdGroup().withStatusBLGenerated(status));
        assertThat(result.getDynamicTextAdGroup().getDomainUrlProcessingStatus()).isEqualTo(expectedStatus);
    }

    @Test
    public void convertToExternalItem_sourceIsConverted_getNotEmptyString() {
        Long feedId = 888L;

        AdGroupGetItem result = converter.convertToExternalItem(buidDynamicFeedAdGroup().withFeedId(feedId));
        assertThat(result.getDynamicTextFeedAdGroup().getSource()).isEqualTo(feedId.toString());
    }

    @Test
    public void convertToExternalItem_sourceIsConverted_getEmptyString() {
        AdGroupGetItem result = converter.convertToExternalItem(buidDynamicFeedAdGroup().withFeedId(null));
        assertThat(result.getDynamicTextFeedAdGroup().getSource()).isEqualTo("");
    }

    @Test
    public void convertToExternalItem_sourceTypeIsConverted() {
        Long feedId = 888L;
        SourceTypeGetEnum sourceType = SourceTypeGetEnum.RETAIL_FEED;

        AdGroupGetItem result = converter.convertToExternalItem(buidDynamicFeedAdGroup().withFeedId(feedId));
        assertThat(result.getDynamicTextFeedAdGroup().getSourceType()).isEqualTo(sourceType);
    }

    @Test
    public void convertToExternalItem_sourceProcessingStatusIsConverted() {
        var status = StatusBLGenerated.YES;
        SourceProcessingStatusEnum expectedStatus = SourceProcessingStatusEnum.PROCESSED;

        AdGroupGetItem result = converter.convertToExternalItem(buidDynamicFeedAdGroup().withStatusBLGenerated(status));
        assertThat(result.getDynamicTextFeedAdGroup().getSourceProcessingStatus()).isEqualTo(expectedStatus);
    }

    @Test
    public void convertToExternalItem_logoExtensionIsConverted() {
        AdGroupGetItem result = converter.convertToExternalItem(buildPerformanceAdGroup()
            .withBanners(List.of(buildPerformanceMainBanner()
                    .withLogoImageHash("logoImageHash")
                    .withLogoStatusModerate(BannerLogoStatusModerate.YES))));

        assertThat(result.getSmartAdGroup()).satisfies(adGroup -> {
            assertThat(adGroup).isNotNull();
            assertSoftly(softly -> {
                softly.assertThat(adGroup.getLogoExtensionHash()).satisfies(logoExtensionHash -> {
                    assertThat(logoExtensionHash).isNotNull();
                    assertThat(logoExtensionHash.getValue()).isEqualTo("logoImageHash");
                });
                softly.assertThat(adGroup.getLogoExtensionModeration()).satisfies(logoExtensionModeration -> {
                    assertThat(logoExtensionModeration).isNotNull();
                    assertThat(logoExtensionModeration.getValue()).isNotNull();
                    assertThat(logoExtensionModeration.getValue().getStatus()).isEqualTo(StatusEnum.ACCEPTED);
                });
            });
        });
    }

    private TextAdGroup buidTextAdGroup() {
        return new TextAdGroup()
                .withId(0L)
                .withCampaignId(0L)
                .withType(AdGroupType.BASE)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NEW)
                .withBsRarelyLoaded(Boolean.FALSE);
    }

    private MobileContentAdGroup buidMobileAppAdGroup() {
        return new MobileContentAdGroup()
                .withId(0L)
                .withCampaignId(0L)
                .withType(AdGroupType.DYNAMIC)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NEW)
                .withBsRarelyLoaded(Boolean.FALSE)
                .withDeviceTypeTargeting(Collections.singleton(MobileContentAdGroupDeviceTypeTargeting.PHONE))
                .withNetworkTargeting(Collections.singleton(MobileContentAdGroupNetworkTargeting.CELLULAR))
                .withMobileContent(new MobileContent().withOsType(OsType.ANDROID).withIconHash("iconHash")
                        .withStatusIconModerate(StatusIconModerate.YES));
    }

    private DynamicTextAdGroup buidDynamicTextAdGroup() {
        return new DynamicTextAdGroup()
                .withId(0L)
                .withCampaignId(0L)
                .withType(AdGroupType.DYNAMIC)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NEW)
                .withBsRarelyLoaded(Boolean.FALSE)
                .withStatusBLGenerated(StatusBLGenerated.PROCESSING);
    }

    private DynamicFeedAdGroup buidDynamicFeedAdGroup() {
        return new DynamicFeedAdGroup()
                .withId(0L)
                .withCampaignId(0L)
                .withType(AdGroupType.DYNAMIC)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NEW)
                .withBsRarelyLoaded(Boolean.FALSE)
                .withStatusBLGenerated(StatusBLGenerated.PROCESSING);
    }

    private PerformanceAdGroup buildPerformanceAdGroup() {
        return new PerformanceAdGroup()
                .withId(0L)
                .withCampaignId(0L)
                .withType(AdGroupType.PERFORMANCE)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NEW)
                .withBsRarelyLoaded(Boolean.FALSE)
                .withStatusBLGenerated(StatusBLGenerated.PROCESSING);
    }

    private PerformanceBannerMain buildPerformanceMainBanner() {
        return new PerformanceBannerMain()
                .withId(0L)
                .withAdGroupId(0L);
    }
}
