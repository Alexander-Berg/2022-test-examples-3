package ru.yandex.direct.api.v5.entity.ads;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adextensiontypes.AdExtensionSetting;
import com.yandex.direct.api.v5.adextensiontypes.AdExtensionSettingItem;
import com.yandex.direct.api.v5.ads.AdBuilderAdUpdateItem;
import com.yandex.direct.api.v5.ads.AdUpdateItem;
import com.yandex.direct.api.v5.ads.CpcVideoAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.DynamicTextAdUpdate;
import com.yandex.direct.api.v5.ads.MobileAppAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.MobileAppAdUpdate;
import com.yandex.direct.api.v5.ads.MobileAppCpcVideoAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.MobileAppImageAdUpdate;
import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.ads.SmartAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.TextAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.TextAdUpdate;
import com.yandex.direct.api.v5.ads.TextImageAdUpdate;
import com.yandex.direct.api.v5.general.OperationEnum;
import one.util.streamex.IntStreamEx;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

@SuppressWarnings("WeakerAccess")
@ParametersAreNonnullByDefault
public class AdsUpdateTestData {

    public static final ObjectFactory jaxbElementsFactory = new ObjectFactory();

    public static final long SMART_TEST_AD_ID = 500L;

    /**
     * @param size размер массива update-элементов
     * @return Список {@link AdUpdateItem} заданного размера с валидными id
     * и update-элементами для всех возможных типов баннеров поочерёдно.
     */
    public static List<AdUpdateItem> listOfUpdateItems(int size) {
        List<Function<AdUpdateItem, AdUpdateItem>> addAd = asList(
                item -> item.withTextAd(validTextAdUpdate()),
                item -> item.withDynamicTextAd(validDynamicAdUpdate()),
                item -> item.withTextImageAd(validImageAdUpdate()),
                item -> item.withTextAdBuilderAd(validImageCreativeAdUpdate()),
                item -> item.withMobileAppAd(validMobileAppAdUpdate()),
                item -> item.withMobileAppImageAd(validMobileImageAdUpdate()),
                item -> item.withMobileAppCpcVideoAdBuilderAd(validMobileAppCpcVideoAdUpdate()),
                item -> item.withMobileAppAdBuilderAd(validMobileCreativeAdUpdate()),
                item -> item.withCpcVideoAdBuilderAd(validCpcVideoAdUpdate()),
                item -> item.withId(SMART_TEST_AD_ID).withSmartAdBuilderAd(validSmartAdUpdate()));
        return IntStreamEx.range(size)
                .mapToObj(i -> addAd.get(i % addAd.size()).apply(new AdUpdateItem().withId(i + 1)))
                .toList();
    }

    public static TextAdUpdate textAdUpdateWithCalloutsUpdate(OperationEnum... operations) {
        return validTextAdUpdate()
                .withCalloutSetting(jaxbElementsFactory.createTextAdUpdateBaseCalloutSetting(
                        calloutsUpdate(operations)));
    }

    public static DynamicTextAdUpdate dynamicAdUpdateWithCalloutsUpdate(OperationEnum... operations) {
        return validDynamicAdUpdate()
                .withCalloutSetting(jaxbElementsFactory.createTextAdUpdateBaseCalloutSetting(
                        calloutsUpdate(operations)));
    }

    public static AdExtensionSetting calloutsUpdate(OperationEnum... operations) {
        int i = 0;
        List<AdExtensionSettingItem> items = new ArrayList<>();
        for (OperationEnum operation : operations) {
            items.add(new AdExtensionSettingItem().withAdExtensionId(i++).withOperation(operation));
        }
        return new AdExtensionSetting().withAdExtensions(items);
    }

    public static TextAdUpdate validTextAdUpdate() {
        return new TextAdUpdate().withText("New text.");
    }

    public static DynamicTextAdUpdate validDynamicAdUpdate() {
        return new DynamicTextAdUpdate().withVCardId(jaxbElementsFactory.createTextAdUpdateBaseVCardId(555000L));
    }

    public static TextImageAdUpdate validImageAdUpdate() {
        return new TextImageAdUpdate().withAdImageHash(randomAlphanumeric(16));
    }

    public static TextAdBuilderAdUpdate validImageCreativeAdUpdate() {
        return new TextAdBuilderAdUpdate().withCreative(new AdBuilderAdUpdateItem().withCreativeId(3505050L));
    }

    public static MobileAppAdUpdate validMobileAppAdUpdate() {
        return new MobileAppAdUpdate().withText("New text.");
    }

    public static MobileAppImageAdUpdate validMobileImageAdUpdate() {
        return new MobileAppImageAdUpdate().withAdImageHash(randomAlphanumeric(16));
    }

    public static MobileAppAdBuilderAdUpdate validMobileCreativeAdUpdate() {
        return new MobileAppAdBuilderAdUpdate().withCreative(new AdBuilderAdUpdateItem().withCreativeId(98989898L));
    }

    public static CpcVideoAdBuilderAdUpdate validCpcVideoAdUpdate() {
        return new CpcVideoAdBuilderAdUpdate().withCreative(
                new AdBuilderAdUpdateItem().withCreativeId(98776655L))
//                .withHref("http://video.yandex.ru")
                ;
    }

    public static MobileAppCpcVideoAdBuilderAdUpdate validMobileAppCpcVideoAdUpdate() {
        return new MobileAppCpcVideoAdBuilderAdUpdate().withCreative(
                new AdBuilderAdUpdateItem()
                        .withCreativeId(5552L));
    }

    public static SmartAdBuilderAdUpdate validSmartAdUpdate() {
        return new SmartAdBuilderAdUpdate().withCreative(new AdBuilderAdUpdateItem().withCreativeId(9800898L));
    }
}
