package ru.yandex.autotests.direct.intapi.java.tests.creatives;

import java.util.Collections;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.data.Logins;
import ru.yandex.autotests.direct.intapi.java.factories.creative.CreativesFactory;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.models.CreativeUploadData;
import ru.yandex.autotests.direct.intapi.models.CreativeUploadResponse;
import ru.yandex.autotests.direct.intapi.models.CreativeUploadResult;
import ru.yandex.autotests.direct.intapi.models.ModerationInfo;
import ru.yandex.autotests.direct.intapi.models.YabsData;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Обновление скринов и названия существующих DisplayCanvas креативов")
@Stories(TestFeatures.DisplayCanvas.UPLOAD_CREATIVES)
@Features(TestFeatures.DISPLAY_CANVAS)
@Tag(Tags.DISPLAY_CANVAS)
@Tag("DIRECT-62478")
@Issue("DIRECT-62478")
public class DisplayCanvasUploadUpdateCreativesTest {

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private static CreativeUploadData creativeUploadData = CreativesFactory.defaultCreative();

    private String operatorLogin = Logins.DEFAULT_CREATIVE_CONSTUCTOR_CLIENT;

    private String clientLogin = Logins.DEFAULT_CREATIVE_CONSTUCTOR_CLIENT;

    private Long clientId;
    private Long operatorUid;
    private static PerfCreativesRecord actualRecord;

    private CreativeUploadResponse actualResponse;

    @Before
    public void before() {
        clientId = directClassRule.dbSteps().shardingSteps().getClientIdByLogin(clientLogin);
        operatorUid = directClassRule.dbSteps().shardingSteps().getUidByLogin(operatorLogin);
        directClassRule.dbSteps().useShardForLogin(clientLogin);

        creativeUploadData.setCreativeId(new CreativesHelper(directClassRule).getNotExistentCreativeId());
        actualResponse = directClassRule.intapiSteps().displayCanvasSteps().uploadCreatives(
                operatorUid,
                clientId,
                Collections.singletonList(creativeUploadData)
        );

        CreativeUploadResult actualResult = directClassRule.intapiSteps()
                .displayCanvasSteps().resultById(actualResponse, creativeUploadData.getCreativeId());
        assumeThat("креатив сохранился", actualResult.getStatus(),
                equalTo(CreativeUploadResult.StatusEnum.OK));

    }

    @Test
    @Description("можно заменить название у существующего креатива")
    public void updateCanvasCreativeNames() {
        String newName = creativeUploadData.getCreativeName() + RandomStringUtils.randomAlphabetic(3);
        creativeUploadData.setCreativeName(newName);
        actualResponse = directClassRule.intapiSteps().displayCanvasSteps().uploadCreatives(
                operatorUid,
                clientId,
                Collections.singletonList(creativeUploadData)
        );

        CreativeUploadResult actualResult = directClassRule.intapiSteps()
                .displayCanvasSteps().resultById(actualResponse, creativeUploadData.getCreativeId());
        assumeThat("креатив сохранился", actualResult.getStatus(),
                equalTo(CreativeUploadResult.StatusEnum.OK));

        actualRecord =
                directClassRule.dbSteps().perfCreativesSteps().getPerfCreatives(creativeUploadData.getCreativeId());
        assertThat("новое название креатива сохранилось в базе", actualRecord.getName(),
                equalTo(newName));
    }

    @Test
    @Description("можно заменить обновляемые поля у существующего креатива")
    public void updateCanvasCreativeUpdatableFields() {
        String newPreviewUrl = "http://rimatravel.info/wp-content/uploads/2014/03/china-3-300x300.jpg";

        ModerationInfo updatedModerationInfo = creativeUploadData.getModerationInfo();
        updatedModerationInfo.getHtml().withUrl(
                updatedModerationInfo.getHtml().getUrl() + "?updated=1"
        );

        YabsData newYabsData = new YabsData().withBasePath("https://test-base-path");
        creativeUploadData.withYabsData(newYabsData).withModerationInfo(updatedModerationInfo)
                .withPreviewUrl(newPreviewUrl);

        actualResponse = directClassRule.intapiSteps().displayCanvasSteps().uploadCreatives(
                operatorUid,
                clientId,
                Collections.singletonList(creativeUploadData)
        );

        CreativeUploadResult actualResult = directClassRule.intapiSteps()
                .displayCanvasSteps().resultById(actualResponse, creativeUploadData.getCreativeId());
        assumeThat("креатив сохранился", actualResult.getStatus(),
                equalTo(CreativeUploadResult.StatusEnum.OK));

        actualRecord =
                directClassRule.dbSteps().perfCreativesSteps().getPerfCreatives(creativeUploadData.getCreativeId());
        assumeThat("новый скрин креатива сохранился в базе", actualRecord.getPreviewUrl(),
                equalTo(newPreviewUrl));
        assumeThat("yabs_data обновилось в базе", actualRecord.getYabsData(),
                containsString(newYabsData.getBasePath()));
        assertThat("moderate_info обновилось в базе", actualRecord.getModerateInfo(),
                containsString(updatedModerationInfo.getHtml().getUrl()));
    }

    @Test
    @Description("нельзя изменить размер у существующего креатива")
    public void updateCanvasCreativeWeightHeight() {
        short oldWight = creativeUploadData.getWidth().shortValue();
        short oldHeight = creativeUploadData.getHeight().shortValue();
        creativeUploadData.setWidth(400);
        creativeUploadData.setHeight(400);
        actualResponse = directClassRule.intapiSteps().displayCanvasSteps().uploadCreatives(
                operatorUid,
                clientId,
                Collections.singletonList(creativeUploadData)
        );

        CreativeUploadResult actualResult = directClassRule.intapiSteps()
                .displayCanvasSteps().resultById(actualResponse, creativeUploadData.getCreativeId());
        assumeThat("креатив сохранился", actualResult.getStatus(),
                equalTo(CreativeUploadResult.StatusEnum.OK));

        actualRecord =
                directClassRule.dbSteps().perfCreativesSteps().getPerfCreatives(creativeUploadData.getCreativeId());
        assertThat("ширина креатива не изменилась", actualRecord.getWidth(),
                equalTo(oldWight));
        assertThat("высота креатива не изменилась", actualRecord.getHeight(),
                equalTo(oldHeight));
    }
}
