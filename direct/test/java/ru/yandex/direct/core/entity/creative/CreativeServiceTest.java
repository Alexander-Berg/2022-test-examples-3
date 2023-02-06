package ru.yandex.direct.core.entity.creative;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.canvas.client.model.video.ModerationInfoAspect;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoSound;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoText;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoVideo;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeBusinessType;
import ru.yandex.direct.core.entity.creative.model.ModerationInfo;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoHtml;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.creative.model.YabsData;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.creative.service.CreativeService;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCreatives;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestCreativeRepository;
import ru.yandex.direct.core.testing.steps.CreativeSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.core.testing.stub.CanvasClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.creative.model.CreativeConverter.fromCanvasCreative;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmIndoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmOutdoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultVideoAddition;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreativeServiceTest {
    @Autowired
    private CreativeService creativeService;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private CanvasClientStub canvasClientStub;

    @Autowired
    private TestCreativeRepository testCreativeRepository;

    @Autowired
    private CreativeSteps creativeSteps;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private Steps steps;

    private int shard;
    private ClientId clientId;
    private UserInfo defaultUser;

    @Before
    public void before() {
        defaultUser = userSteps.createDefaultUser();
        shard = defaultUser.getShard();
        clientId = defaultUser.getClientInfo().getClientId();
    }

    @Test
    public void synhronizeCreatives_CreativeNotSaved_Sync() {
        long creativeId = testCreativeRepository.getNextCreativeId();
        List<Long> creativeIds = singletonList(creativeId);
        canvasClientStub.addCreatives(creativeIds);
        creativeService.synchronizeVideoAdditionCreatives(shard, clientId, new HashSet<>(creativeIds));

        List<Creative> actualCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        assumeThat("получен один креатив", actualCreatives, hasSize(1));
        assumeThat("креативы совпдают по id", actualCreatives.get(0).getId(), equalTo(creativeId));
    }

    @Test
    public void synhronizeCreatives_CreativeNotSaved_CreativeNotInCanvas_NotSync() {
        long creativeId = testCreativeRepository.getNextCreativeId();
        List<Long> creativeIds = singletonList(creativeId);
        creativeService.synchronizeVideoAdditionCreatives(shard, clientId, new HashSet<>(creativeIds));

        List<Creative> actualCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        assertThat("креативы отсутствуют", actualCreatives, hasSize(0));
    }

    @Test
    public void synhronizeCreatives_CreativeSaved_NotSync() {
        String name = "testName";
        CreativeInfo creative = creativeSteps
                .createCreative(defaultVideoAddition(clientId, null).withName(name), defaultUser.getClientInfo());
        List<Long> creativeIds = singletonList(creative.getCreativeId());
        canvasClientStub.addCreatives(creativeIds);
        creativeService.synchronizeVideoAdditionCreatives(shard, clientId, new HashSet<>(creativeIds));
        List<Creative> actualCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        assumeThat("получен один креатив", actualCreatives, hasSize(1));
        assertThat("имя совпадает", actualCreatives.get(0).getName(), equalTo(creative.getCreative().getName()));
    }

    @Test
    public void createOrUpdate_UpdateYabsData() {
        CreativeInfo creativeInfo =
                creativeSteps.createCreative(defaultHtml5(clientId, null), defaultUser.getClientInfo());
        List<Long> creativeIds = singletonList(creativeInfo.getCreativeId());
        canvasClientStub.addCreatives(creativeIds);
        List<Creative> expectedCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        expectedCreatives.get(0).withYabsData(new YabsData().withBasePath("http://updated"));
        creativeService.createOrUpdate(expectedCreatives, clientId);
        List<Creative> afterUpdateCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        assertThat("Креатив после обновления yabs_data совпадает с ожидаемым", expectedCreatives,
                beanDiffer(afterUpdateCreatives));
    }

    @Test
    public void createOrUpdate_UpdateModerationInfo() {
        CreativeInfo creativeInfo =
                creativeSteps.createCreative(defaultHtml5(clientId, null), defaultUser.getClientInfo());
        List<Long> creativeIds = singletonList(creativeInfo.getCreativeId());
        canvasClientStub.addCreatives(creativeIds);
        List<Creative> expectedCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        expectedCreatives.get(0)
                .withModerationInfo(new ModerationInfo().withHtml(new ModerationInfoHtml().withUrl("http://test")));
        creativeService.createOrUpdate(expectedCreatives, clientId);
        List<Creative> afterUpdateCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        assertThat("Креатив после обновления moderation_info совпадает с ожидаемым", expectedCreatives,
                beanDiffer(afterUpdateCreatives));
    }

    @Test
    public void createOrUpdate_UpdateModerationInfo_dropAdminRejectReason() {
        String reason = "someReason";
        Creative creative = defaultHtml5(clientId, null);
        creative.setModerationInfo(new ModerationInfo().withAdminRejectReason(reason));
        CreativeInfo creativeInfo = creativeSteps.createCreative(creative, defaultUser.getClientInfo());
        List<Long> creativeIds = singletonList(creativeInfo.getCreativeId());
        canvasClientStub.addCreatives(creativeIds);
        List<Creative> expectedCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        expectedCreatives.get(0)
                .withModerationInfo(new ModerationInfo().withHtml(new ModerationInfoHtml().withUrl("http://test")));
        creativeService.createOrUpdate(expectedCreatives, clientId);
        List<Creative> afterUpdateCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        assertThat("Должна быть возможность сбросить поле admin_reject_reason",
                afterUpdateCreatives.get(0).getModerationInfo().getAdminRejectReason(), nullValue());
    }

    @Test
    public void createOrUpdate_UpdateArchiveUrl() {
        CreativeInfo creativeInfo =
                creativeSteps.createCreative(defaultHtml5(clientId, null), defaultUser.getClientInfo());
        List<Long> creativeIds = singletonList(creativeInfo.getCreativeId());
        canvasClientStub.addCreatives(creativeIds);
        List<Creative> expectedCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        expectedCreatives.get(0).withArchiveUrl("https://storage.yandex.ru/some-domain/some-creative-archive.zip");
        creativeService.createOrUpdate(expectedCreatives, clientId);
        List<Creative> afterUpdateCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        assertThat("Креатив после обновления archive_url совпадает с ожидаемым", expectedCreatives,
                beanDiffer(afterUpdateCreatives));
    }

    @Test
    public void createOrUpdate_UpdateVideoSize() {
        var creativeId = creativeSteps.getNextCreativeId();
        var creative = creativeSteps.addDefaultCpmVideoAdditionCreative(defaultUser.getClientInfo(), creativeId)
                        .getCreative();
        creative.withWidth(1920L)
                .withHeight(1080L);
        MassResult<Long> result = creativeService.createOrUpdate(singletonList(creative), clientId);
        assertThat(result, isFullySuccessful());

        Creative actualCreative = creativeRepository.getCreatives(shard, singletonList(creativeId)).get(0);
        assertThat("Поля размера актуальны", actualCreative.getWidth(), is(1920L));
    }

    @Test
    public void createOrUpdate_NullableFieldsIgnored() {
        CreativeInfo creativeInfo = creativeSteps.createCreative(
                defaultHtml5(clientId, null).withModerationInfo(new ModerationInfo().withHtml(
                        new ModerationInfoHtml().withUrl("http://test-moderation-info")
                )),
                defaultUser.getClientInfo());
        List<Long> creativeIds = singletonList(creativeInfo.getCreativeId());
        canvasClientStub.addCreatives(creativeIds);
        List<Creative> expectedCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);

        List<Creative> toUpdateCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        toUpdateCreatives.get(0).withYabsData(null);
        toUpdateCreatives.get(0).withModerationInfo(null);
        toUpdateCreatives.get(0).withArchiveUrl(null);

        creativeService.createOrUpdate(toUpdateCreatives, clientId);
        List<Creative> afterUpdateCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        assertThat("Поля переданные как null в update остались без изменений", afterUpdateCreatives,
                beanDiffer(expectedCreatives));
    }

    @Test
    public void createOrUpdate_CpmOutdoorCreativeWithInvalidAdditionalData_Error() {
        Long nextCreativeId = creativeSteps.getNextCreativeId();
        MassResult<Long> result = creativeService
                .createOrUpdate(singletonList(
                        defaultCpmOutdoorVideoAddition(clientId, nextCreativeId).withAdditionalData(null)), clientId);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field(Creative.ADDITIONAL_DATA)), notNull())));
    }

    @Test
    public void createOrUpdate_AddCpmOutdoor_Successful() {
        Long creativeId = creativeSteps.getNextCreativeId();
        Creative creative = defaultCpmOutdoorVideoAddition(clientId, creativeId);
        MassResult<Long> result = creativeService
                .createOrUpdate(singletonList(creative), clientId);
        assertThat(result, isFullySuccessful());

        Creative actualCreative = creativeRepository.getCreatives(shard, singletonList(creativeId)).get(0);
        assertThat(actualCreative,
                beanDiffer(creative).useCompareStrategy(allFields().forFields(newPath(Creative.ADDITIONAL_DATA.name(),
                        AdditionalData.DURATION.name())).useDiffer(new BigDecimalDiffer())));
    }

    @Test
    public void createOrUpdate_UpdateCpmOutdoor_Successful() {
        Long creativeId = creativeSteps.getNextCreativeId();
        Creative creative =
                creativeSteps.addDefaultCpmOutdoorVideoCreative(defaultUser.getClientInfo(), creativeId).getCreative();
        creative.getAdditionalData()
                .withDuration(BigDecimal.valueOf(7))
                .getFormats().get(1)
                .withWidth(125)
                .withHeight(125);
        MassResult<Long> result = creativeService.createOrUpdate(singletonList(creative), clientId);
        assertThat(result, isFullySuccessful());

        Creative actualCreative = creativeRepository.getCreatives(shard, singletonList(creativeId)).get(0);
        assertThat(actualCreative,
                beanDiffer(creative).useCompareStrategy(allFields().forFields(newPath(Creative.ADDITIONAL_DATA.name(),
                        AdditionalData.DURATION.name())).useDiffer(new BigDecimalDiffer())));
    }

    @Test
    public void createOrUpdate_CpmIndoorCreativeWithInvalidAdditionalData_Error() {
        Long nextCreativeId = creativeSteps.getNextCreativeId();
        MassResult<Long> result = creativeService
                .createOrUpdate(singletonList(
                        defaultCpmIndoorVideoAddition(clientId, nextCreativeId).withAdditionalData(null)), clientId);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field(Creative.ADDITIONAL_DATA)), notNull())));
    }

    @Test
    public void createOrUpdate_AddCpmIndoor_Successful() {
        Long creativeId = creativeSteps.getNextCreativeId();
        Creative creative = defaultCpmIndoorVideoAddition(clientId, creativeId);
        MassResult<Long> result = creativeService
                .createOrUpdate(singletonList(creative), clientId);
        assertThat(result, isFullySuccessful());

        Creative actualCreative = creativeRepository.getCreatives(shard, singletonList(creativeId)).get(0);
        assertThat(actualCreative,
                beanDiffer(creative).useCompareStrategy(allFields().forFields(newPath(Creative.ADDITIONAL_DATA.name(),
                        AdditionalData.DURATION.name())).useDiffer(new BigDecimalDiffer())));
    }

    @Test
    public void createOrUpdate_UpdateCpmIndoor_Successful() {
        Long creativeId = creativeSteps.getNextCreativeId();
        Creative creative =
                creativeSteps.addDefaultCpmIndoorVideoCreative(defaultUser.getClientInfo(), creativeId).getCreative();
        creative.getAdditionalData()
                .withDuration(BigDecimal.valueOf(7))
                .getFormats().get(1)
                .withWidth(125)
                .withHeight(125);
        MassResult<Long> result = creativeService.createOrUpdate(singletonList(creative), clientId);
        assertThat(result, isFullySuccessful());

        Creative actualCreative = creativeRepository.getCreatives(shard, singletonList(creativeId)).get(0);
        assertThat(actualCreative,
                beanDiffer(creative).useCompareStrategy(allFields().forFields(newPath(Creative.ADDITIONAL_DATA.name(),
                        AdditionalData.DURATION.name())).useDiffer(new BigDecimalDiffer())));
    }

    @Test
    public void createOrUpdate_AddNonCpmOutdoor_AdditionalDataCleared() {
        Long creativeId = creativeSteps.getNextCreativeId();
        Creative creative = defaultCpcVideoForCpcVideoBanner(clientId, creativeId)
                .withAdditionalData(new AdditionalData()
                        .withDuration(BigDecimal.valueOf(10.2))
                        .withFormats(singletonList(new VideoFormat()
                                .withHeight(1000)
                                .withWidth(500)
                                .withType("some type")
                                .withUrl("https://ya.ru/creative/123"))));
        MassResult<Long> result = creativeService
                .createOrUpdate(singletonList(creative), clientId);
        assertThat(result, isFullySuccessful());

        Creative actualCreative = creativeRepository.getCreatives(shard, singletonList(creativeId)).get(0);
        assertThat(actualCreative.getAdditionalData(), nullValue());
    }

    @Test
    public void createOrUpdate_Html5Creative_AdditionalDataNotCleared() {
        Long creativeId = creativeSteps.getNextCreativeId();
        var originalHeight = 602;
        var originalWidth = 500;
        var additionalData = new AdditionalData()
                .withOriginalHeight(originalHeight)
                .withOriginalWidth(originalWidth);

        Creative creative = defaultHtml5(clientId, creativeId).withAdditionalData(additionalData);

        MassResult<Long> result = creativeService.createOrUpdate(singletonList(creative), clientId);

        assertThat(result, isFullySuccessful());

        Creative actualCreative = creativeRepository.getCreatives(shard, singletonList(creativeId)).get(0);
        assertThat(actualCreative.getAdditionalData(), equalTo(additionalData));
    }

    @Test
    public void getPerfCreativesWithBusinessType_success() {
        Creative defaultCreative = TestCreatives.defaultPerformanceCreative(clientId, null)
                .withBusinessType(CreativeBusinessType.RETAIL);
        creativeSteps.createCreative(defaultCreative, defaultUser.getClientInfo());

        List<Creative> creativesWithBusinessType =
                creativeService.getCreativesWithBusinessType(clientId, BusinessType.RETAIL, null);

        creativesWithBusinessType.forEach(creative -> {
            assertThat(creative.getBusinessType(), is(CreativeBusinessType.RETAIL));
        });
        assertTrue(creativesWithBusinessType.contains(defaultCreative));
    }

    @Test
    public void getPerfCreativesWithBusinessType_incorrectType() {
        Creative creative = TestCreatives.defaultPerformanceCreative(clientId, null)
                .withBusinessType(CreativeBusinessType.RETAIL);
        creativeSteps.createCreative(creative, defaultUser.getClientInfo());

        List<Creative> creativesWithBusinessType =
                creativeService.getCreativesWithBusinessType(clientId, BusinessType.AUTO, null);

        assertTrue(!creativesWithBusinessType.contains(creative));
    }

    @Test
    public void getCreativesByPerformanceAdGroups_success() {
        //Создаём группу, и связанные с нею креативы
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();
        ClientId clientId = adGroupInfo.getClientId();
        Creative[] expectedCreatives = new Creative[3];
        for (int i = 0; i < expectedCreatives.length; i++) {
            Creative creative = defaultPerformanceCreative(clientId, null);
            CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());
            Long creativeId = creativeInfo.getCreativeId();
            OldPerformanceBanner banner = activePerformanceBanner(campaignId, adGroupId, creativeId);
            steps.bannerSteps().createBanner(banner, adGroupInfo);
            expectedCreatives[i] = creative;
        }

        //Выполняем запрос
        Map<Long, List<Creative>> creativesByPerformanceAdGroups =
                creativeService.getCreativesByPerformanceAdGroups(clientId, singletonList(adGroupId));
        List<Creative> creatives = creativesByPerformanceAdGroups.get(adGroupId);
        Creative[] actualSortedByIdCreatives = StreamEx.of(creatives)
                .sortedByLong(Creative::getId)
                .toArray(Creative.class);

        //Сверяем ожидания и реальность
        Assertions.assertThat(actualSortedByIdCreatives)
                .is(matchedBy(beanDiffer(expectedCreatives)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void isAdaptive_fromCanvasCreative_Successful() {
        ru.yandex.direct.canvas.client.model.video.Creative canvasCreative = new ru.yandex.direct.canvas.client.model.video.Creative()
                .withCreativeId(33L)
                .withCreativeName("")
                .withCreativeType(ru.yandex.direct.canvas.client.model.video.Creative.CreativeType.HTML5_CREATIVE)
                .withPreviewUrl("https://www.artstation.com/artwork/RYKqoA")
                .withModerationInfo(
                        new ru.yandex.direct.canvas.client.model.video.ModerateInfo().withHtml(
                                new ru.yandex.direct.canvas.client.model.video.ModerationInfoHtml().withUrl("")
                        ).withAspects(
                                Arrays.asList(new ModerationInfoAspect().withHeight(5).withWidth(10))
                        ).withSounds(
                                Arrays.asList(new ModerationInfoSound().withStockId("124").withUrl(""))
                        ).withTexts(
                                Arrays.asList(new ModerationInfoText().withColor("").withText("").withType(""))
                        ).withVideos(
                                Arrays.asList(new ModerationInfoVideo().withStockId("124").withUrl(""))
                        ).withImages(
                                Arrays.asList(new ru.yandex.direct.canvas.client.model.video.ModerationInfoImage().withAlt("").withUrl(""))
                        )
                )
                .withStockCreativeId(124L)
                .withWidth(10)
                .withHeight(5)
                .withDuration(73)
                .withPresetId(28)
                .withIsAdaptive(null);

        Creative creative = fromCanvasCreative(canvasCreative);

        assertThat(creative.getIsAdaptive(), equalTo(false));
    }

    @Test
    public void synhronizeCreatives_isAdaptive_Successful() {
        long creativeId = testCreativeRepository.getNextCreativeId();
        ru.yandex.direct.canvas.client.model.video.Creative creative =
                canvasClientStub.defaultCanvasCreative(creativeId).withIsAdaptive(null);
        canvasClientStub.addCustomCreatives(Arrays.asList(creative));
        List<Long> creativeIds = singletonList(creativeId);
        creativeService.synchronizeVideoAdditionCreatives(shard, clientId, new HashSet<>(creativeIds));

        List<Creative> actualCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        assumeThat("получен один креатив", actualCreatives, hasSize(1));
        assumeThat("правельный IsAdaptive по умолчанию", actualCreatives.get(0).getIsAdaptive(), equalTo(false));
    }

    @Test
    public void synhronizeCreatives_hasPackshot_Successful() {
        long creativeId = testCreativeRepository.getNextCreativeId();
        ru.yandex.direct.canvas.client.model.video.Creative creative =
                canvasClientStub.defaultCanvasCreative(creativeId).withIsAdaptive(null);
        canvasClientStub.addCustomCreatives(singletonList(creative));
        List<Long> creativeIds = singletonList(creativeId);
        creativeService.synchronizeVideoAdditionCreatives(shard, clientId, new HashSet<>(creativeIds));

        List<Creative> actualCreatives = creativeRepository.getCreatives(shard, clientId, creativeIds);
        assumeThat("получен один креатив", actualCreatives, hasSize(1));
        assumeThat("правильный hasPackshot", actualCreatives.get(0).getHasPackshot(), equalTo(true));
    }

}
