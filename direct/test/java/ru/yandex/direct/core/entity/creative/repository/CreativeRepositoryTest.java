package ru.yandex.direct.core.entity.creative.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.jooq.exception.DataAccessException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.entity.creative.model.ModerationInfo;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoHtml;
import ru.yandex.direct.core.entity.creative.model.SourceMediaType;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.creative.model.YabsData;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmOutdoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreativeRepositoryTest {
    private Long nextCreativeId;
    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CreativeRepository repoUnderTest;

    private ClientInfo clientInfo;
    private CreativeInfo canvasInfo;
    private CreativeInfo videoAdditionInfo;
    private CreativeInfo html5CreativeInfo;

    private int shard;
    private ClientId clientId;

    private static Creative canvasWithNoDefaultValues(Long creativeId, ClientId clientId) {
        return new Creative()
                .withId(creativeId)
                .withName("new canvas_creative")
                .withStockCreativeId(creativeId)
                .withClientId(clientId.asLong())
                .withType(CreativeType.CANVAS)
                .withIsGenerated(false)
                .withPreviewUrl("http://yandex.ru")
                .withLivePreviewUrl("http://yandex.ru")
                .withExpandedPreviewUrl("http://expanded.ru")
                .withArchiveUrl("https://storage.yandex.ru/some-domain/some-creative-archive.zip")
                .withYabsData(new YabsData().withHtml5(true).withBasePath("https://some-mds.yandex-tam.ru/resources/"))
                .withModerationInfo(new ModerationInfo().withHtml(new ModerationInfoHtml()
                        .withUrl("https://some-mds.yandex-team.ru/tysh-pysh/preview?compact=1")))
                .withHeight(480L)
                .withWidth(360L)
                .withStatusModerate(StatusModerate.YES)
                .withModerateTryCount(0L)
                .withLayoutId(1L)
                .withIsAdaptive(false)
                .withIsBrandLift(false)
                .withHasPackshot(false);
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();

        Long creativeId1 = steps.creativeSteps().getNextCreativeId();
        canvasInfo = steps.creativeSteps().addDefaultCanvasCreative(clientInfo, creativeId1);

        Long creativeId2 = steps.creativeSteps().getNextCreativeId();
        videoAdditionInfo = steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo, creativeId2);

        Long creativeId3 = steps.creativeSteps().getNextCreativeId();
        html5CreativeInfo = steps.creativeSteps().addDefaultHtml5Creative(clientInfo, creativeId3);

        nextCreativeId = steps.creativeSteps().getNextCreativeId();
    }

    @Test
    public void getExistentIds_CreativeIdsExists() {
        List<Long> creativeIdsToFind =
                asList(canvasInfo.getCreativeId(), videoAdditionInfo.getCreativeId(), nextCreativeId);
        List<Long> expectedCreativeIds = asList(canvasInfo.getCreativeId(), videoAdditionInfo.getCreativeId());

        Collection<Long> creativeIds = repoUnderTest.getExistingClientCreativeIds(shard, clientId, creativeIdsToFind,
                asList(CreativeType.CANVAS, CreativeType.VIDEO_ADDITION_CREATIVE));

        assertThat("список полученных ид креативов соответствует ожидаемому",
                new ArrayList<>(creativeIds),
                containsInAnyOrder(expectedCreativeIds.toArray()));
    }

    @Test
    public void add_OneCreative() {
        Creative creative = canvasWithNoDefaultValues(nextCreativeId, clientInfo.getClientId());
        repoUnderTest.add(shard, singletonList(creative));
        Collection<Long> ids = repoUnderTest.getExistingClientCreativeIds(shard, clientId,
                singletonList(nextCreativeId), singleton(CreativeType.CANVAS));
        assertThat("креатив добавился", ids, contains(equalTo(nextCreativeId)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void add_TwoCreatives() {
        Creative canvasCreative1 = canvasWithNoDefaultValues(nextCreativeId, clientInfo.getClientId());
        Long nextCreativeId2 = steps.creativeSteps().getNextCreativeId();
        Creative canvasCreative2 = canvasWithNoDefaultValues(nextCreativeId2, clientInfo.getClientId());
        repoUnderTest.add(shard, asList(canvasCreative1, canvasCreative2));
        Collection<Long> ids = repoUnderTest.getExistingClientCreativeIds(shard, clientId,
                asList(canvasCreative1.getId(), canvasCreative2.getId()), singleton(CreativeType.CANVAS));
        assertThat("добавилось два креатива", ids, contains(equalTo(canvasCreative1.getId()),
                equalTo(canvasCreative2.getId())));
    }

    @Test(expected = DataAccessException.class)
    public void add_DuplicateKey() {
        Creative creative1 = videoAdditionInfo.getCreative()
                .withName("duplicate Video")
                .withPreviewUrl("http://yandex.ru");
        Creative creative2 = canvasInfo.getCreative()
                .withName("duplicate")
                .withModerationInfo(new ModerationInfo().withHtml(new ModerationInfoHtml().withUrl(
                        canvasInfo.getCreative().getModerationInfo().getHtml().getUrl() + "?duplicated"
                )))
                .withPreviewUrl("http://direct.yandex.ru");
        Creative creative3 = html5CreativeInfo.getCreative()
                .withName("duplicate html5 creative")
                .withArchiveUrl("http://storage.yandex.ru/some-domain/updated_archive.zip")
                .withYabsData(new YabsData().withBasePath(
                        html5CreativeInfo.getCreative().getYabsData().getBasePath() + "/duplicated"
                ));
        repoUnderTest.add(shard, asList(creative1, creative2, creative3));
    }

    @Test
    public void add_OneCanvasCreativeWithNullStockCreativeId() {
        Creative creative = canvasWithNoDefaultValues(nextCreativeId, clientInfo.getClientId())
                .withStockCreativeId(null);
        repoUnderTest.add(shard, singletonList(creative));
        Collection<Creative> creatives = repoUnderTest.getCreatives(shard, singletonList(nextCreativeId));
        List<Creative> creativeList = new ArrayList<>(creatives);
        assumeThat("креатив добавился", creativeList, hasSize(1));
        assertThat("stockCreativeId равен creativeId", creativeList.get(0).getStockCreativeId(),
                equalTo(creative.getId()));
    }

    @Test
    public void add_OneCanvasCreativeWithNullLivePreviewUrl() {
        Creative creative = canvasWithNoDefaultValues(nextCreativeId, clientInfo.getClientId())
                .withLivePreviewUrl(null);
        repoUnderTest.add(shard, singletonList(creative));
        Collection<Creative> creatives = repoUnderTest.getCreatives(shard, singletonList(nextCreativeId));
        List<Creative> creativeList = new ArrayList<>(creatives);
        assumeThat("креатив добавился", creativeList, hasSize(1));
        assertThat("livePreviewUrl остался равен null", creativeList.get(0).getLivePreviewUrl(),
                equalTo(null));
    }

    @Test
    public void add_OneCanvasCreativeWithLivePreviewUrl() {
        Creative creative = canvasWithNoDefaultValues(nextCreativeId, clientInfo.getClientId())
                .withLivePreviewUrl("http://video.yandex.ru");
        repoUnderTest.add(shard, singletonList(creative));
        Collection<Creative> creatives = repoUnderTest.getCreatives(shard, singletonList(nextCreativeId));
        List<Creative> creativeList = new ArrayList<>(creatives);
        assumeThat("креатив добавился", creativeList, hasSize(1));
        assertThat("livePreviewUrl canvas установлен", creativeList.get(0).getLivePreviewUrl(),
                equalTo("http://video.yandex.ru"));
    }

    @Test
    public void add_OneCanvasCreativeWithExpandedPreviewUrl() {
        Creative creative = canvasWithNoDefaultValues(nextCreativeId, clientInfo.getClientId())
                .withExpandedPreviewUrl("http://expanded.ru");
        repoUnderTest.add(shard, singletonList(creative));
        Collection<Creative> creatives = repoUnderTest.getCreatives(shard, singletonList(nextCreativeId));
        List<Creative> creativeList = new ArrayList<>(creatives);
        assumeThat("креатив добавился", creativeList, hasSize(1));
        assertThat("expandedPreviewUrl установлен", creativeList.get(0).getExpandedPreviewUrl(),
                equalTo(("http://expanded.ru")));
    }

    //change
    @Test
    public void update_CanvasNoChange() {
        Creative creative = canvasInfo.getCreative();
        AppliedChanges<Creative> canvasAppliedChanges =
                changeCreative(creative, creative.getName(), creative.getPreviewUrl(), creative.getLivePreviewUrl(),
                        creative.getArchiveUrl(), creative.getYabsData(), creative.getModerationInfo());
        repoUnderTest.update(shard, singletonList(canvasAppliedChanges));

        Creative changedCreative =
                repoUnderTest.getCreatives(shard, singletonList(canvasInfo.getCreativeId())).get(0);

        assertThat("изменения в креативе соответствуют ожидаемым",
                changedCreative, beanDiffer(creative));
    }

    @Test
    public void update_Html5NoChange() {
        Creative html5Creative = canvasInfo.getCreative();
        AppliedChanges<Creative> canvasAppliedChanges =
                changeCreative(html5Creative, html5Creative.getName(), html5Creative.getPreviewUrl(),
                        html5Creative.getLivePreviewUrl(), html5Creative.getArchiveUrl(), html5Creative.getYabsData(),
                        html5Creative.getModerationInfo());
        repoUnderTest.update(shard, singletonList(canvasAppliedChanges));

        Creative changedCreative =
                repoUnderTest.getCreatives(shard, singletonList(canvasInfo.getCreativeId())).get(0);

        assertThat("изменения html5-креативе соответствуют ожидаемым",
                changedCreative, beanDiffer(html5Creative));
    }

    @Test
    public void update_OneCreative() {
        AppliedChanges<Creative> canvasAppliedChanges =
                changeCreative(canvasInfo.getCreative(), "new canvas", "http://direct.yandex.ru",
                        "http://direct.yandex.ru", "http://mds.yandex.ru/archive.zip");
        repoUnderTest.update(shard, singletonList(canvasAppliedChanges));

        Creative changedCreative =
                repoUnderTest.getCreatives(shard, singletonList(canvasInfo.getCreativeId())).get(0);

        assertThat("изменения в креативе соответствуют ожидаемым",
                changedCreative, beanDiffer(canvasInfo.getCreative()));
    }

    @Test
    public void update_TwoCreatives() {
        AppliedChanges<Creative> canvasAppliedChanges =
                changeCreative(canvasInfo.getCreative(), "new canvas", "http://direct.yandex.ru",
                        "http://direct.yandex.ru");
        AppliedChanges<Creative> videoAdditionAppliedChanges =
                changeCreative(videoAdditionInfo.getCreative(), "new video-addition", "http://yandex.ru",
                        "http://video.yandex.ru");
        repoUnderTest.update(shard, asList(canvasAppliedChanges, videoAdditionAppliedChanges));

        List<Creative> changedCreatives = repoUnderTest
                .getCreatives(shard, Arrays.asList(canvasInfo.getCreativeId(), videoAdditionInfo.getCreativeId()));

        assertThat("изменения в креативе соответствуют ожидаемым", changedCreatives,
                containsInAnyOrder(beanDiffer(canvasInfo.getCreative()), beanDiffer(videoAdditionInfo.getCreative())));
    }

    @Test
    public void update_Creative_NullLivePreview() {
        AppliedChanges<Creative> canvasAppliedChanges =
                changeCreative(canvasInfo.getCreative(), "new canvas", "http://direct.yandex.ru",
                        null);
        repoUnderTest.update(shard, singletonList(canvasAppliedChanges));

        Creative changedCreative =
                repoUnderTest.getCreatives(shard, singletonList(canvasInfo.getCreativeId())).get(0);

        assertThat("livePreviewUrl остался равен null",
                changedCreative.getLivePreviewUrl(), equalTo(null));
    }

    @Test
    public void getExistingClientCreativeIdsById() {
        Collection<Long> ids =
                repoUnderTest.getExistingClientCreativeIds(shard, clientId, singleton(canvasInfo.getCreativeId()),
                        emptyList());
        assertThat(ids, contains(canvasInfo.getCreativeId()));
    }

    @Test
    public void getExistingClientCreativeIdsByIds() {
        Collection<Long> ids = repoUnderTest.getExistingClientCreativeIds(shard, clientId,
                asList(canvasInfo.getCreativeId(), videoAdditionInfo.getCreativeId(),
                        html5CreativeInfo.getCreativeId()), emptyList());
        assertThat(ids, contains(canvasInfo.getCreativeId(), videoAdditionInfo.getCreativeId(),
                html5CreativeInfo.getCreativeId()));
    }

    @Test
    public void getExistingClientCreativeIds_ClientIdAndIds_GetOnlyOwnCreativeIds() {
        ClientInfo newClient = steps.clientSteps().createDefaultClient();
        Creative canvasCreative = defaultCanvas(newClient.getClientId(), nextCreativeId);

        Collection<Long> ids = repoUnderTest.getExistingClientCreativeIds(shard, clientId,
                asList(canvasInfo.getCreativeId(), videoAdditionInfo.getCreativeId(), canvasCreative.getId()),
                emptyList());
        assertThat(ids, contains(canvasInfo.getCreativeId(), videoAdditionInfo.getCreativeId()));
    }

    @Test
    public void getExistingClientCreativeIdsByIdsAndTypes() {
        Collection<Long> ids = repoUnderTest.getExistingClientCreativeIds(shard, clientId,
                asList(canvasInfo.getCreativeId(), videoAdditionInfo.getCreativeId(),
                        html5CreativeInfo.getCreativeId()),
                asList(CreativeType.CANVAS, CreativeType.HTML5_CREATIVE));
        assertThat(ids, contains(canvasInfo.getCreativeId(), html5CreativeInfo.getCreativeId()));
    }

    @Test
    public void getCreativesByIdsAndType() {
        Collection<Long> ids = repoUnderTest.getExistingClientCreativeIds(shard, clientId,
                asList(canvasInfo.getCreativeId(), videoAdditionInfo.getCreativeId()),
                asList(CreativeType.CANVAS, CreativeType.VIDEO_ADDITION_CREATIVE));
        assumeThat(ids, hasSize(2));

        List<Creative> creatives = repoUnderTest
                .getCreativesWithType(shard, clientInfo.getClientId(), ids, CreativeType.VIDEO_ADDITION_CREATIVE);
        List<Long> creativesByIdsAndType = mapList(creatives, Creative::getId);
        assertThat("Найден только видео креатив", creativesByIdsAndType,
                allOf(contains(videoAdditionInfo.getCreativeId()), not(contains(canvasInfo.getCreativeId()))));
    }

    @Test
    public void getCreativesByIdsAndTypes() {
        Collection<Long> ids = repoUnderTest.getExistingClientCreativeIds(shard, clientId,
                asList(canvasInfo.getCreativeId(), videoAdditionInfo.getCreativeId(),
                        html5CreativeInfo.getCreativeId()),
                asList(CreativeType.CANVAS, CreativeType.VIDEO_ADDITION_CREATIVE, CreativeType.HTML5_CREATIVE));
        assumeThat(ids, hasSize(3));

        List<Creative> creatives = repoUnderTest.getCreativesWithTypes(shard, clientInfo.getClientId(), ids,
                asList(CreativeType.VIDEO_ADDITION_CREATIVE, CreativeType.CANVAS));
        List<Long> creativesByIdsAndType = mapList(creatives, Creative::getId);
        assertThat("Найдены 2 креатива", creativesByIdsAndType, contains(canvasInfo.getCreativeId(),
                videoAdditionInfo.getCreativeId()));
    }

    @Test
    public void getCreativesByIds() {
        List<Creative> creatives = repoUnderTest.getCreatives(shard, asList(canvasInfo.getCreativeId(),
                videoAdditionInfo.getCreativeId()));
        List<Long> creativesByIdsAndType = mapList(creatives, Creative::getId);
        assertThat("Найдены 2 креатива", creativesByIdsAndType, contains(canvasInfo.getCreativeId(),
                videoAdditionInfo.getCreativeId()));
    }

    @Test
    public void getCreativesByBannerIds() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        CpmBannerInfo banner = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(null, null, videoAdditionInfo.getCreativeId()), adGroupInfo);
        Long bannerId = banner.getBannerId();

        Map<Long, Creative> creativesByBannerIds =
                repoUnderTest.getCreativesByBannerIds(dslContextProvider.ppc(shard), singletonList(bannerId));

        assertThat(creativesByBannerIds.keySet(), hasSize(1));
        assertThat(creativesByBannerIds.get(bannerId), is(videoAdditionInfo.getCreative()));
    }

    @Test
    public void getCreatives_readThemeId() {
        long expectedThemeId = 19L;
        Creative creative = defaultPerformanceCreative(null, null)
                .withThemeId(expectedThemeId);
        Long creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();

        List<Creative> creatives = repoUnderTest.getCreatives(shard, singletonList(creativeId));
        Assertions.assertThat(creatives.get(0).getThemeId())
                .as("creative.themeId")
                .isEqualTo(expectedThemeId);
    }

    @Test
    public void getCreativesByIdsAndClientId() {
        ClientInfo newClient = steps.clientSteps().createDefaultClient();
        Creative videoCreative = canvasWithNoDefaultValues(nextCreativeId, newClient.getClientId())
                .withType(CreativeType.VIDEO_ADDITION_CREATIVE);
        repoUnderTest.add(shard, singletonList(videoCreative));

        List<Creative> creatives = repoUnderTest.getCreatives(shard, clientId, asList(canvasInfo.getCreativeId(),
                videoCreative.getId()));
        List<Long> creativesByIdsAndType = mapList(creatives, Creative::getId);
        assertThat("Найдены 2 креатива", creativesByIdsAndType, contains(canvasInfo.getCreativeId()));
    }

    @Test
    public void add_Creative_SourceMediaTypeDefaultValue() {
        assertThat("Значение source_media_type по умолчанию null",
                canvasInfo.getCreative().getSourceMediaType(), equalTo(null));
    }

    @Test
    public void add_Creative_SourceMediaType() {
        Creative creative = canvasWithNoDefaultValues(nextCreativeId, clientInfo.getClientId()).withSourceMediaType(
                SourceMediaType.GIF);
        repoUnderTest.add(shard, singletonList(creative));
        Collection<Creative> creatives = repoUnderTest.getCreatives(shard, singletonList(nextCreativeId));
        List<Creative> creativeList = new ArrayList<>(creatives);
        assumeThat("креатив добавился", creativeList, hasSize(1));
        assertThat("source_media_type canvas установлен", creativeList.get(0).getSourceMediaType(),
                equalTo(SourceMediaType.GIF));
    }

    @Test
    public void update_CantUpdateSourceMediaType() {
        final Long creativeId = nextCreativeId;
        Creative creative = canvasWithNoDefaultValues(creativeId, clientInfo.getClientId()).withSourceMediaType(
                SourceMediaType.GIF);
        repoUnderTest.add(shard, singletonList(creative));
        Creative addedCreative = repoUnderTest.getCreatives(shard, singletonList(creativeId)).get(0);
        assumeThat("source_media_type равен gif", addedCreative.getSourceMediaType(), equalTo(SourceMediaType.GIF));

        ModelChanges<Creative> modelChanges = new ModelChanges<>(creative.getId(), Creative.class);
        modelChanges.process(SourceMediaType.JPG, Creative.SOURCE_MEDIA_TYPE);
        AppliedChanges<Creative> canvasAppliedChanges = modelChanges.applyTo(creative);

        repoUnderTest.update(shard, singletonList(canvasAppliedChanges));

        Creative changedCreative =
                repoUnderTest.getCreatives(shard, singletonList(creativeId)).get(0);

        assertThat("source_media_type остался равен gif",
                changedCreative.getSourceMediaType(), equalTo(SourceMediaType.GIF));
    }

    @Test
    public void add_Creative_IsGeneratedDefaultValue() {
        assertThat("Значение is_generated по умолчанию false",
                canvasInfo.getCreative().getIsGenerated(), equalTo(false));
    }

    @Test
    public void add_Creative_isGenerated() {
        Creative creative = canvasWithNoDefaultValues(nextCreativeId, clientInfo.getClientId()).withIsGenerated(true);
        repoUnderTest.add(shard, singletonList(creative));
        Collection<Creative> creatives = repoUnderTest.getCreatives(shard, singletonList(nextCreativeId));
        List<Creative> creativeList = new ArrayList<>(creatives);
        assumeThat("креатив добавился", creativeList, hasSize(1));
        assertThat("is_generated canvas установлен", creativeList.get(0).getIsGenerated(),
                equalTo(true));
    }

    @Test
    public void update_CantUpdateIsGenerated() {
        final Creative creative = canvasInfo.getCreative();

        assumeThat("is_generated равен false", creative.getIsGenerated(), equalTo(false));

        ModelChanges<Creative> modelChanges = new ModelChanges<>(creative.getId(), Creative.class);
        modelChanges.process(true, Creative.IS_GENERATED);
        AppliedChanges<Creative> canvasAppliedChanges = modelChanges.applyTo(creative);

        repoUnderTest.update(shard, singletonList(canvasAppliedChanges));

        Creative changedCreative =
                repoUnderTest.getCreatives(shard, singletonList(canvasInfo.getCreativeId())).get(0);

        assertThat("is_generated остался равен false",
                changedCreative.getIsGenerated(), equalTo(false));
    }

    @Test
    public void add_Creative_LayoutIdNull() {
        Creative creative = canvasWithNoDefaultValues(nextCreativeId, clientInfo.getClientId()).withLayoutId(null);
        repoUnderTest.add(shard, singletonList(creative));
        Collection<Creative> creatives = repoUnderTest.getCreatives(shard, singletonList(nextCreativeId));
        List<Creative> creativeList = new ArrayList<>(creatives);
        assumeThat("креатив добавился", creativeList, hasSize(1));
        assertThat("layout_id должен быть равен null", creativeList.get(0).getLayoutId(), nullValue());
    }

    @Test
    public void update_CreativeYaBsData() {
        Creative creative = html5CreativeInfo.getCreative();

        YabsData changedYabsData = new YabsData()
                .withHtml5(true)
                .withBasePath(creative.getYabsData().getBasePath() + "?some_random=stuff");

        ModelChanges<Creative> modelChanges = new ModelChanges<>(creative.getId(), Creative.class);
        modelChanges.process(changedYabsData, Creative.YABS_DATA);

        repoUnderTest.update(shard, singletonList(modelChanges.applyTo(creative)));

        Creative changedCreative =
                repoUnderTest.getCreatives(shard, singletonList(html5CreativeInfo.getCreativeId())).get(0);

        assertThat("yabs_data обновился update-ом", changedCreative.getYabsData(),
                beanDiffer(changedYabsData));
    }

    @Test
    public void update_ModerationInfo() {
        Creative creative = canvasInfo.getCreative();

        ModerationInfo changedModerationInfo = new ModerationInfo()
                .withHtml(new ModerationInfoHtml().withUrl(
                        creative.getModerationInfo().getHtml().getUrl() + "?some_random=stuff"
                ));

        ModelChanges<Creative> modelChanges = new ModelChanges<>(creative.getId(), Creative.class);
        modelChanges.process(changedModerationInfo, Creative.MODERATION_INFO);

        repoUnderTest.update(shard, singletonList(modelChanges.applyTo(creative)));

        Creative changedCreative =
                repoUnderTest.getCreatives(shard, singletonList(canvasInfo.getCreativeId())).get(0);

        assertThat("moderation_info обновился update-ом", changedCreative.getModerationInfo(),
                beanDiffer(changedModerationInfo));
    }

    @Test
    public void add_OutdoorCreative() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultCpmOutdoorVideoAddition(clientInfo.getClientId(), creativeId);
        repoUnderTest.add(clientInfo.getShard(), Collections.singletonList(creative));

        Creative actualCreative =
                repoUnderTest.getCreatives(clientInfo.getShard(), Collections.singletonList(creativeId)).get(0);
        assertThat(actualCreative,
                beanDiffer(creative).useCompareStrategy(allFields().forFields(newPath(Creative.ADDITIONAL_DATA.name(),
                        AdditionalData.DURATION.name())).useDiffer(new BigDecimalDiffer())));
    }

    @Test
    public void update_OutdoorCreative() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative =
                steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(clientInfo, creativeId).getCreative();

        repoUnderTest.update(shard, singletonList(new ModelChanges<>(creative.getId(), Creative.class)
                .process(new AdditionalData()
                                .withDuration(BigDecimal.valueOf(7.0))
                                .withFormats(singletonList(new VideoFormat()
                                        .withWidth(1000)
                                        .withHeight(1000)
                                        .withType("some type")
                                        .withUrl("http://new-url"))),
                        Creative.ADDITIONAL_DATA)
                .applyTo(creative)));

        Creative actualCreative =
                repoUnderTest.getCreatives(clientInfo.getShard(), Collections.singletonList(creativeId)).get(0);
        assertThat(actualCreative,
                beanDiffer(creative).useCompareStrategy(allFields().forFields(newPath(Creative.ADDITIONAL_DATA.name(),
                        AdditionalData.DURATION.name())).useDiffer(new BigDecimalDiffer())));
    }


    private AppliedChanges<Creative> changeCreative(@Nonnull Creative creative, String name, String previewUrl,
                                                    String livePreviewUrl) {
        return creativeModelChanges(creative, name, previewUrl, livePreviewUrl).applyTo(creative);
    }

    private AppliedChanges<Creative> changeCreative(@Nonnull Creative creative, String name, String previewUrl,
                                                    String livePreviewUrl, String archiveUrl) {
        return creativeModelChanges(creative, name, previewUrl, livePreviewUrl, archiveUrl).applyTo(creative);
    }

    private AppliedChanges<Creative> changeCreative(@Nonnull Creative creative, String name, String previewUrl,
                                                    String livePreviewUrl, String archiveUrl, YabsData yabsData, ModerationInfo moderationInfo) {
        return creativeModelChanges(creative, name, previewUrl, livePreviewUrl, archiveUrl,
                yabsData, moderationInfo).applyTo(creative);
    }

    private ModelChanges<Creative> creativeModelChanges(@Nonnull Creative creative, String name, String previewUrl,
                                                        String livePreviewUrl, String archiveUrl) {
        ModelChanges<Creative> modelChanges = creativeModelChanges(creative, name, previewUrl, livePreviewUrl);
        modelChanges.process(archiveUrl, Creative.ARCHIVE_URL);
        return modelChanges;
    }

    private ModelChanges<Creative> creativeModelChanges(@Nonnull Creative creative, String name, String previewUrl,
                                                        String livePreviewUrl) {
        ModelChanges<Creative> modelChanges = new ModelChanges<>(creative.getId(), Creative.class);
        modelChanges.process(name, Creative.NAME);
        modelChanges.process(previewUrl, Creative.PREVIEW_URL);
        modelChanges.process(livePreviewUrl, Creative.LIVE_PREVIEW_URL);
        return modelChanges;
    }

    private ModelChanges<Creative> creativeModelChanges(@Nonnull Creative creative, String name, String previewUrl,
                                                        String livePreviewUrl, String archiveUrl, @Nullable YabsData yabsData,
                                                        @Nullable ModerationInfo moderationInfo) {
        ModelChanges<Creative> modelChanges =
                creativeModelChanges(creative, name, previewUrl, livePreviewUrl, archiveUrl);
        modelChanges.process(yabsData, Creative.YABS_DATA);
        modelChanges.process(moderationInfo, Creative.MODERATION_INFO);
        return modelChanges;
    }
}
