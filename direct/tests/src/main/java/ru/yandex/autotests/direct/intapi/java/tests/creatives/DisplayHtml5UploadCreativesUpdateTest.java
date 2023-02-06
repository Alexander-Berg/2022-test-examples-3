package ru.yandex.autotests.direct.intapi.java.tests.creatives;

import java.util.Collections;

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
import ru.yandex.autotests.direct.intapi.models.YabsData;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
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
@Description("Проверка обновления полей DisplayCanvas html5-креатива при перезаливке в upload_creatives")
@Stories(TestFeatures.DisplayCanvas.UPLOAD_CREATIVES)
@Features(TestFeatures.DISPLAY_CANVAS)
@Tag(Tags.DISPLAY_CANVAS)
@Tag(TagDictionary.TRUNK)
@Tag("DIRECT-86917")
@Issue("DIRECT-86917")
public class DisplayHtml5UploadCreativesUpdateTest {
    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private static CreativeUploadData creativeUploadData = CreativesFactory.defaultHtml5Creative();

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
    @Description("можно заменить обновляемые поля у существующего креатива")
    public void updateCanvasCreativeUpdatableFields() {
        String newPreviewUrl = "http://rimatravel.info/wp-content/uploads/2014/03/china-3-300x300.jpg";
        String newArchiveUrl = "http://some-mds.yandex.ru/some-updated-archive.zip";

        YabsData newYabsData = new YabsData().withBasePath("https://test-base-path");
        creativeUploadData.withYabsData(newYabsData)
                .withPreviewUrl(newPreviewUrl).withArchiveUrl(newArchiveUrl);

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
        assertThat("новый url архива сохранился в базе", actualRecord.getArchiveUrl(),
                equalTo(newArchiveUrl));
        assumeThat("новый скрин креатива сохранился в базе", actualRecord.getPreviewUrl(),
                equalTo(newPreviewUrl));
        assertThat("yabs_data обновилось в базе", actualRecord.getYabsData(),
                containsString(newYabsData.getBasePath()));
    }


}
