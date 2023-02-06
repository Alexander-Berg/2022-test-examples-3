package ru.yandex.autotests.direct.intapi.java.tests.creatives;

import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PerfCreativesSourceMediaType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.data.Logins;
import ru.yandex.autotests.direct.intapi.java.factories.creative.CreativesFactory;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.models.CreativeUploadData;
import ru.yandex.autotests.direct.intapi.models.CreativeUploadResponse;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by hmepas on 21.02.18
 */
@Aqua.Test
@Description("Проверка работы поля composedFrom в DisplayCanvas upload_creatives при заливке html5-объявления")
@Stories(TestFeatures.DisplayCanvas.UPLOAD_CREATIVES)
@Features(TestFeatures.DISPLAY_CANVAS)
@Tag(Tags.DISPLAY_CANVAS)
@Tag(TagDictionary.TRUNK)
@Tag("DIRECT-73194")
@Issue("DIRECT-73194")
@RunWith(Parameterized.class)
public class DisplayHtml5UploadCreativesComposedFromTest {
    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private static CreativeUploadData creativeUploadData = CreativesFactory.defaultHtml5Creative();

    private static String operatorLogin = Logins.DEFAULT_CREATIVE_CONSTUCTOR_CLIENT;

    private static String clientLogin = Logins.DEFAULT_CREATIVE_CONSTUCTOR_CLIENT;

    private static Long clientId;
    private static Long operatorUid;

    private PerfCreativesRecord uploadedCreativeRecord;

    @Parameterized.Parameter
    public static CreativeUploadData.ComposedFromEnum composedFrom;

    @Parameterized.Parameter(1)
    public static int isGenerated;

    @Parameterized.Parameter(2)
    public static PerfCreativesSourceMediaType sourceMediaType;

    @Parameterized.Parameters(name = "checking composedFrom = {0}")
    public static List<Object[]> parameters() {
        return asList(new Object[][]{
                {CreativeUploadData.ComposedFromEnum.GIF, 1, PerfCreativesSourceMediaType.gif},
                {CreativeUploadData.ComposedFromEnum.JPG, 1, PerfCreativesSourceMediaType.jpg},
                {CreativeUploadData.ComposedFromEnum.PNG, 1, PerfCreativesSourceMediaType.png},
                {null, 0, null},
        });
    }


    @BeforeClass
    public static void before() {
        clientId = directClassRule.dbSteps().shardingSteps().getClientIdByLogin(clientLogin);
        operatorUid = directClassRule.dbSteps().shardingSteps().getUidByLogin(operatorLogin);
        directClassRule.dbSteps().useShardForLogin(clientLogin);
    }

    @Before
    public void uploadCreative() {
        creativeUploadData.withCreativeId(new CreativesHelper(directClassRule).getNotExistentCreativeId())
                .withComposedFrom(composedFrom);
        final CreativeUploadResponse actualResponse = directClassRule.intapiSteps().displayCanvasSteps().uploadCreatives(
                operatorUid,
                clientId,
                Collections.singletonList(creativeUploadData)
        );

        assumeThat("в ответе результат по одному креативу", actualResponse.getUploadResults(), hasSize(1));

        uploadedCreativeRecord = directClassRule.dbSteps().perfCreativesSteps().getPerfCreatives(creativeUploadData.getCreativeId());
    }

    @AfterClass
    public static void after() {
        directClassRule.dbSteps().perfCreativesSteps().deletePerfCreatives(creativeUploadData.getCreativeId());
    }

    @Test
    @Description("is_generated сохранился верно")
    public void creativeIsGeneratedOk() {
        assertThat("is_generated = " + isGenerated, uploadedCreativeRecord.getIsGenerated(), equalTo(isGenerated));
    }

    @Test
    @Description("source_media_type сохранился верно")
    public void creativeSourceMediaTypeOk() {
        assertThat("source_media_type = " + sourceMediaType, uploadedCreativeRecord.getSourceMediaType(), equalTo(sourceMediaType));
    }
}
