package ru.yandex.direct.api.v5.entity.creatives.converter;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.yandex.direct.api.v5.creatives.CpcVideoCreativeGet;
import com.yandex.direct.api.v5.creatives.CpmVideoCreativeGet;
import com.yandex.direct.api.v5.creatives.CreativeGetItem;
import com.yandex.direct.api.v5.creatives.CreativeTypeEnum;
import com.yandex.direct.api.v5.creatives.VideoExtensionCreativeGet;
import com.yandex.direct.api.v5.general.YesNoEnum;
import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.creatives.container.CreativeGetContainer;
import ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeType;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.creatives.converter.GetResponseConverter.convertToApiItem;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.CPC_VIDEO_CREATIVE_DURATION;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.CPM_VIDEO_CREATIVE_DURATION;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.CREATIVE_ASSOCIATED;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.CREATIVE_HEIGHT;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.CREATIVE_ID;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.CREATIVE_IS_ADAPTIVE;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.CREATIVE_NAME;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.CREATIVE_PREVIEW_URL;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.CREATIVE_THUMBNAIL_URL;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.CREATIVE_TYPE;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.CREATIVE_WIDTH;
import static ru.yandex.direct.api.v5.entity.creatives.delegate.CreativeAnyFieldEnum.VIDEO_EXTENSION_CREATIVE_DURATION;

public class GetResponseConverterTest {

    private static final Set<CreativeAnyFieldEnum> GENERAL_FIELDS = ImmutableSet.of(
            CREATIVE_ID,
            CREATIVE_NAME,
            CREATIVE_ASSOCIATED,
            CREATIVE_HEIGHT,
            CREATIVE_WIDTH,
            CREATIVE_PREVIEW_URL,
            CREATIVE_THUMBNAIL_URL,
            CREATIVE_TYPE,
            CREATIVE_IS_ADAPTIVE);

    private PropertyFilter propertyFilter = new PropertyFilter();
    private GetResponseConverter getResponseConverter = new GetResponseConverter(propertyFilter);

    @Test
    public void filterProperties_FullItem_GetIdAndName() {
        CreativeGetItem fullItem = fullCpcVideoCreativeGetItem();

        Set<CreativeAnyFieldEnum> fieldEnums = ImmutableSet.of(CREATIVE_ID, CREATIVE_NAME);
        CreativeGetItem expected = new CreativeGetItem()
                .withId(fullItem.getId())
                .withName(fullItem.getName());

        List<CreativeGetItem> items = singletonList(fullItem);

        getResponseConverter.filterProperties(items, fieldEnums);
        assertThat(items)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }

    @Test
    public void filterProperties_FullCpcVideoCreativeItem_AllFields() {
        CreativeGetItem fullItem = fullCpcVideoCreativeGetItem();
        CreativeGetItem expected = fullCpcVideoCreativeGetItem();

        Set<CreativeAnyFieldEnum> fieldEnums = StreamEx.of(GENERAL_FIELDS).append(CPC_VIDEO_CREATIVE_DURATION).toSet();
        List<CreativeGetItem> items = singletonList(fullItem);

        getResponseConverter.filterProperties(items, fieldEnums);
        assertThat(items)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }

    @Test
    public void filterProperties_FullCpmVideoCreativeItem_AllFields() {
        CreativeGetItem fullItem = fullCpmVideoCreativeGetItem();
        CreativeGetItem expected = fullCpmVideoCreativeGetItem();

        Set<CreativeAnyFieldEnum> fieldEnums = StreamEx.of(GENERAL_FIELDS).append(CPM_VIDEO_CREATIVE_DURATION).toSet();
        List<CreativeGetItem> items = singletonList(fullItem);

        getResponseConverter.filterProperties(items, fieldEnums);
        assertThat(items)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }

    @Test
    public void filterProperties_FullVideoExtensionCreativeItem_AllFields() {
        CreativeGetItem fullItem = fullVideoExtensionCreativeGetItem();
        CreativeGetItem expected = fullVideoExtensionCreativeGetItem();

        Set<CreativeAnyFieldEnum> fieldEnums =
                StreamEx.of(GENERAL_FIELDS).append(VIDEO_EXTENSION_CREATIVE_DURATION).toSet();
        List<CreativeGetItem> items = singletonList(fullItem);

        getResponseConverter.filterProperties(items, fieldEnums);
        assertThat(items)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }


    @Test
    public void convertToApiItem_FullContainerToCreativeGetItem() {
        CreativeGetItem expected = fullCpcVideoCreativeGetItem();

        Creative creative = new Creative()
                .withId(expected.getId())
                .withType(CreativeType.CPC_VIDEO_CREATIVE)
                .withName(expected.getName())
                .withPreviewUrl(expected.getPreviewUrl())
                .withLivePreviewUrl(expected.getThumbnailUrl())
                .withWidth(expected.getWidth().longValue())
                .withHeight(expected.getHeight().longValue())
                .withDuration(expected.getCpcVideoCreative().getDuration().longValue())
                .withIsAdaptive(expected.getIsAdaptive() == YesNoEnum.YES);

        CreativeGetContainer container = new CreativeGetContainer().withCreative(creative)
                .withUsedInAds(true);

        CreativeGetItem item = convertToApiItem(container);
        assertThat(item)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void convertToApiItem_FullCpmVideoCreative_ToFullApiItem() {
        CreativeGetItem expected = fullCpmVideoCreativeGetItem();

        Creative creative = new Creative()
                .withId(expected.getId())
                .withType(CreativeType.CPM_VIDEO_CREATIVE)
                .withName(expected.getName())
                .withPreviewUrl(expected.getPreviewUrl())
                .withLivePreviewUrl(expected.getThumbnailUrl())
                .withWidth(expected.getWidth().longValue())
                .withHeight(expected.getHeight().longValue())
                .withDuration(expected.getCpmVideoCreative().getDuration().longValue())
                .withIsAdaptive(expected.getIsAdaptive() == YesNoEnum.YES);

        CreativeGetContainer container = new CreativeGetContainer().withCreative(creative)
                .withUsedInAds(true);

        CreativeGetItem item = convertToApiItem(container);
        assertThat(item)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void convertToApiItem_FullVideoExtensionCreative_ToFullApiItem() {
        CreativeGetItem expected = fullVideoExtensionCreativeGetItem();

        Creative creative = new Creative()
                .withId(expected.getId())
                .withType(CreativeType.VIDEO_ADDITION_CREATIVE)
                .withName(expected.getName())
                .withPreviewUrl(expected.getPreviewUrl())
                .withLivePreviewUrl(expected.getThumbnailUrl())
                .withWidth(expected.getWidth().longValue())
                .withHeight(expected.getHeight().longValue())
                .withDuration(expected.getVideoExtensionCreative().getDuration().longValue())
                .withIsAdaptive(expected.getIsAdaptive() == YesNoEnum.YES);

        CreativeGetContainer container = new CreativeGetContainer().withCreative(creative)
                .withUsedInAds(true);

        CreativeGetItem item = convertToApiItem(container);
        assertThat(item)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    private CreativeGetItem fullCpcVideoCreativeGetItem() {
        return new CreativeGetItem()
                .withId(1L)
                .withName("cpc test name")
                .withType(CreativeTypeEnum.CPC_VIDEO_CREATIVE)
                .withWidth(200)
                .withHeight(300)
                .withCpcVideoCreative(new CpcVideoCreativeGet().withDuration(15))
                .withThumbnailUrl("http://preview.screenshot.url/cpc")
                .withPreviewUrl("http://preview.original.url/cpc")
                .withAssociated(YesNoEnum.YES)
                .withIsAdaptive(YesNoEnum.NO);
    }

    private CreativeGetItem fullCpmVideoCreativeGetItem() {
        return new CreativeGetItem()
                .withId(1L)
                .withName("cpm test name")
                .withType(CreativeTypeEnum.CPM_VIDEO_CREATIVE)
                .withWidth(400)
                .withHeight(600)
                .withCpmVideoCreative(new CpmVideoCreativeGet().withDuration(15))
                .withThumbnailUrl("http://preview.screenshot.url/cpm")
                .withPreviewUrl("http://preview.original.url/cpm")
                .withAssociated(YesNoEnum.YES)
                .withIsAdaptive(YesNoEnum.NO);
    }

    private CreativeGetItem fullVideoExtensionCreativeGetItem() {
        return new CreativeGetItem()
                .withId(1L)
                .withName("video addition test name")
                .withType(CreativeTypeEnum.VIDEO_EXTENSION_CREATIVE)
                .withWidth(100)
                .withHeight(150)
                .withVideoExtensionCreative(new VideoExtensionCreativeGet().withDuration(15))
                .withThumbnailUrl("http://preview.screenshot.url/video_ext")
                .withPreviewUrl("http://preview.original.url/video_ext")
                .withAssociated(YesNoEnum.YES)
                .withIsAdaptive(YesNoEnum.NO);
    }
}
