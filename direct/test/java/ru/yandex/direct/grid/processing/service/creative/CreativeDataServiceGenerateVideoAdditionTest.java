package ru.yandex.direct.grid.processing.service.creative;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.canvas.client.CanvasClient;
import ru.yandex.direct.canvas.client.model.video.ModerateInfo;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoAspect;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoHtml;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoImage;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoSound;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoText;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoVideo;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.service.CreativeService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.grid.core.entity.banner.service.GridBannerService;
import ru.yandex.direct.grid.processing.model.cliententity.GdGenerateVideoAddition;
import ru.yandex.direct.grid.processing.model.cliententity.GdTypedCreative;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.creative.repository.CreativeConstants.TEXT_VIDEO_ADDITION_LAYOUT_IDS;
import static ru.yandex.direct.grid.processing.service.client.converter.ClientEntityConverter.toGdCreativeImplementation;

public class CreativeDataServiceGenerateVideoAdditionTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final Long AD_GROUP_ID = 1L;
    private static final Long NOT_TEXT_AD_GROUP_ID = 2L;
    private static final Long UNEXISTING_AD_GROUP_ID = 3L;
    private static final Long AD_ID = 1L;
    private static final Long EXISTING_CREATIVE_ID = 1L;
    private static final Long GENERATED_CREATIVE_ID = 2L;

    @Mock
    private ClientService clientService;

    @Mock
    private AdGroupService adGroupService;

    @Mock
    private AdGroupRepository adGroupRepository;

    @Mock
    private BannerService bannerService;

    @Mock
    private GridBannerService gridBannerService;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private CanvasClient canvasClient;

    @Mock
    private CreativeService creativeService;

    @InjectMocks
    private CreativeDataService creativeDataService;

    private GdGenerateVideoAddition input = new GdGenerateVideoAddition()
            .withAdGroupIds(List.of(AD_GROUP_ID));

    private Creative expectedExisting;
    private Creative expectedGenerated;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(1)
                .when(shardHelper)
                .getShardByClientId(any(ClientId.class));

        doReturn(
                new Client()
                        .withClientId(CLIENT_ID.asLong())
                        .withAutoVideo(true))
                .when(clientService)
                .getClient(eq(CLIENT_ID));

        doReturn(Set.of(AD_GROUP_ID))
                .when(adGroupRepository)
                .getClientExistingAdGroupIds(anyInt(), eq(CLIENT_ID), eq(List.of(AD_GROUP_ID)));

        doReturn(Map.of(AD_GROUP_ID, AdGroupType.BASE))
                .when(adGroupService)
                .getAdGroupTypes(eq(CLIENT_ID), eq(Set.of(AD_GROUP_ID)));

        doReturn(Map.of(AD_GROUP_ID, List.of(
                new TextBanner()
                        .withId(AD_ID)
                        .withCreativeId(EXISTING_CREATIVE_ID))))
                .when(bannerService)
                .getBannersByAdGroupIds(eq(List.of(AD_GROUP_ID)));

        doReturn(Map.of(AD_ID,
                new Creative()
                        .withType(CreativeType.VIDEO_ADDITION_CREATIVE)
                        .withId(EXISTING_CREATIVE_ID)
                        .withStatusModerate(StatusModerate.YES)))
                .when(gridBannerService)
                .getCreatives(anyInt(), eq(List.of(AD_ID)));

        doReturn(List.of(List.of(
                new ru.yandex.direct.canvas.client.model.video.Creative()
                        .withCreativeType(ru.yandex.direct.canvas.client.model.video.Creative.CreativeType.VIDEO_ADDITION)
                        .withCreativeId(GENERATED_CREATIVE_ID)
                        .withPresetId(TEXT_VIDEO_ADDITION_LAYOUT_IDS.lowerEndpoint().intValue())
                        .withModerationInfo(new ModerateInfo()
                                .withAspects(List.of(new ModerationInfoAspect()
                                        .withHeight(0)
                                        .withWidth(0)))
                                .withHtml(new ModerationInfoHtml())
                                .withSounds(List.of(new ModerationInfoSound()))
                                .withTexts(List.of(new ModerationInfoText()))
                                .withVideos(List.of(new ModerationInfoVideo()))
                                .withImages(List.of(new ModerationInfoImage()))))))
                .when(canvasClient)
                .generateAdditions(eq(CLIENT_ID.asLong()), any());

        doNothing().when(creativeService).synchronizeVideoAdditionCreatives(anyInt(), any(), anySet());

        doReturn(List.of(new Creative()
                .withType(CreativeType.VIDEO_ADDITION_CREATIVE)
                .withId(GENERATED_CREATIVE_ID)
                .withStatusModerate(StatusModerate.YES)))
                .when(creativeService).get(eq(CLIENT_ID), eq(List.of(GENERATED_CREATIVE_ID)),
                eq(List.of(CreativeType.VIDEO_ADDITION_CREATIVE)));

        expectedExisting = new Creative()
                .withId(EXISTING_CREATIVE_ID)
                .withType(CreativeType.VIDEO_ADDITION_CREATIVE)
                .withStatusModerate(StatusModerate.YES);

        expectedGenerated = new Creative()
                .withId(GENERATED_CREATIVE_ID)
                .withType(CreativeType.VIDEO_ADDITION_CREATIVE)
                .withStatusModerate(StatusModerate.YES);
    }

    @Test
    public void generateVideoAdditions_noAutoVideo() {
        doReturn(new Client().withClientId(CLIENT_ID.asLong()).withAutoVideo(false))
                .when(clientService)
                .getClient(eq(CLIENT_ID));

        check(is(emptyMap()));
    }

    @Test
    public void generateVideoAdditions_notTextGroup() {
        doReturn(Map.of(AD_GROUP_ID, AdGroupType.MOBILE_CONTENT))
                .when(adGroupService)
                .getAdGroupTypes(eq(CLIENT_ID), eq(Set.of(AD_GROUP_ID)));

        check(is(emptyMap()));
    }

    @Test
    public void generateVideoAdditions_hasBannerWithVideoAddition() {
        doReturn(Map.of(AD_ID,
                new Creative()
                        .withType(CreativeType.VIDEO_ADDITION_CREATIVE)
                        .withId(EXISTING_CREATIVE_ID)
                        .withStatusModerate(StatusModerate.YES)))
                .when(gridBannerService)
                .getCreatives(anyInt(), eq(List.of(AD_ID)));

        check(is(Map.of(AD_GROUP_ID, expectedExisting)));
    }

    @Test
    public void generateVideoAdditions_textAndNotTextAdGroup() {
        input.withAdGroupIds(List.of(AD_GROUP_ID, NOT_TEXT_AD_GROUP_ID));

        doReturn(Set.of(AD_GROUP_ID, NOT_TEXT_AD_GROUP_ID))
                .when(adGroupRepository)
                .getClientExistingAdGroupIds(anyInt(), eq(CLIENT_ID), eq(input.getAdGroupIds()));

        doReturn(Map.of(AD_GROUP_ID, AdGroupType.BASE,
                NOT_TEXT_AD_GROUP_ID, AdGroupType.CPM_BANNER))
                .when(adGroupService)
                .getAdGroupTypes(eq(CLIENT_ID), eq(Set.copyOf(input.getAdGroupIds())));

        doReturn(Map.of(AD_ID,
                new Creative()
                        .withType(CreativeType.VIDEO_ADDITION_CREATIVE)
                        .withId(EXISTING_CREATIVE_ID)
                        .withStatusModerate(StatusModerate.YES)))
                .when(gridBannerService)
                .getCreatives(anyInt(), eq(List.of(AD_ID)));

        check(is(Map.of(AD_GROUP_ID, expectedExisting)));
    }

    @Test
    public void generateVideoAdditions_unexistingAndTextAdGroup() {
        input.withAdGroupIds(List.of(AD_GROUP_ID, UNEXISTING_AD_GROUP_ID));

        doReturn(Set.of(AD_GROUP_ID))
                .when(adGroupRepository)
                .getClientExistingAdGroupIds(anyInt(), eq(CLIENT_ID), eq(input.getAdGroupIds()));

        doReturn(Map.of(AD_ID,
                new Creative()
                        .withType(CreativeType.VIDEO_ADDITION_CREATIVE)
                        .withId(EXISTING_CREATIVE_ID)
                        .withStatusModerate(StatusModerate.YES)))
                .when(gridBannerService)
                .getCreatives(anyInt(), eq(List.of(AD_ID)));

        check(is(Map.of(AD_GROUP_ID, expectedExisting)));
    }

    @Test
    public void generateVideoAdditions_hasNoBanners() {
        doReturn(emptyMap())
                .when(bannerService)
                .getBannersByAdGroupIds(eq(List.of(AD_GROUP_ID)));

        check(is(Map.of(AD_GROUP_ID, expectedGenerated)));
    }

    @Test
    public void generateVideoAdditions_hasNoBannersWithVideoAddition() {
        doReturn(Map.of(AD_ID,
                new Creative()
                        .withType(CreativeType.CANVAS)
                        .withId(EXISTING_CREATIVE_ID)
                        .withStatusModerate(StatusModerate.YES)))
                .when(gridBannerService)
                .getCreatives(anyInt(), eq(List.of(AD_ID)));

        check(is(Map.of(AD_GROUP_ID, expectedGenerated)));
    }

    private void check(Matcher<? super Map<Long, Creative>> matcher) {
        assertThat(creativeDataService.generateVideoAdditions(CLIENT_ID, input.getAdGroupIds()), matcher);
    }

    @Test
    public void generateVideoAddition_nullsInOrder1() {
        input.withAdGroupIds(List.of(NOT_TEXT_AD_GROUP_ID, AD_GROUP_ID));

        doReturn(Set.of(NOT_TEXT_AD_GROUP_ID, AD_GROUP_ID))
                .when(adGroupRepository)
                .getClientExistingAdGroupIds(anyInt(), eq(CLIENT_ID), eq(input.getAdGroupIds()));

        doReturn(Map.of(AD_GROUP_ID, AdGroupType.BASE,
                NOT_TEXT_AD_GROUP_ID, AdGroupType.CPM_BANNER))
                .when(adGroupService)
                .getAdGroupTypes(eq(CLIENT_ID), eq(Set.copyOf(input.getAdGroupIds())));

        List<GdTypedCreative> expectedList = new ArrayList<>();
        expectedList.add(null);
        expectedList.add(toGdCreativeImplementation(expectedExisting));

        assertThat(creativeDataService.generateVideoAddition(CLIENT_ID, input), is(expectedList));
    }

    @Test
    public void generateVideoAddition_nullsInOrder2() {
        input.withAdGroupIds(List.of(AD_GROUP_ID, UNEXISTING_AD_GROUP_ID));

        doReturn(Set.of(AD_GROUP_ID))
                .when(adGroupRepository)
                .getClientExistingAdGroupIds(anyInt(), eq(CLIENT_ID), eq(input.getAdGroupIds()));

        doReturn(Map.of(AD_GROUP_ID, AdGroupType.BASE))
                .when(adGroupService)
                .getAdGroupTypes(eq(CLIENT_ID), eq(Set.of(AD_GROUP_ID)));

        List<GdTypedCreative> expectedList = new ArrayList<>();
        expectedList.add(toGdCreativeImplementation(expectedExisting));
        expectedList.add(null);

        assertThat(creativeDataService.generateVideoAddition(CLIENT_ID, input), is(expectedList));
    }

    @Test
    public void generateVideoAddition_nullsInOrder3() {
        input.withAdGroupIds(List.of(NOT_TEXT_AD_GROUP_ID, AD_GROUP_ID, UNEXISTING_AD_GROUP_ID));

        doReturn(Set.of(NOT_TEXT_AD_GROUP_ID, AD_GROUP_ID))
                .when(adGroupRepository)
                .getClientExistingAdGroupIds(anyInt(), eq(CLIENT_ID), eq(input.getAdGroupIds()));

        doReturn(Map.of(AD_GROUP_ID, AdGroupType.BASE,
                NOT_TEXT_AD_GROUP_ID, AdGroupType.CPM_BANNER))
                .when(adGroupService)
                .getAdGroupTypes(eq(CLIENT_ID), eq(Set.of(NOT_TEXT_AD_GROUP_ID, AD_GROUP_ID)));

        List<GdTypedCreative> expectedList = new ArrayList<>();
        expectedList.add(null);
        expectedList.add(toGdCreativeImplementation(expectedExisting));
        expectedList.add(null);

        assertThat(creativeDataService.generateVideoAddition(CLIENT_ID, input), is(expectedList));
    }

}
