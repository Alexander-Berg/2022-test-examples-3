package ru.yandex.autotests.direct.intapi.java.tests.creatives;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PerfCreativesCreativeType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.data.Logins;
import ru.yandex.autotests.direct.intapi.java.factories.creative.CreativesFactory;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.java.tests.creatives.CreativesHelper;
import ru.yandex.autotests.direct.intapi.models.CreativeUploadData;
import ru.yandex.autotests.direct.intapi.models.CreativeUploadResponse;
import ru.yandex.autotests.direct.intapi.models.CreativeUploadResult;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.intapi.java.factories.creative.CreativesFactory.toCreativeUploadData;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка работы DisplayCanvas upload_creatives для видео-дополнения")
@Stories(TestFeatures.DisplayCanvas.UPLOAD_CREATIVES)
@Features(TestFeatures.DISPLAY_CANVAS)
@Tag(Tags.DISPLAY_CANVAS)
@Tag(TagDictionary.TRUNK)
@Tag("DIRECT-63973")
@Issue("DIRECT-63973")
public class DisplayVideoAdditionUploadCreativesTest {
    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();


    private static CreativeUploadData creativeUploadData = CreativesFactory.defaultVideoCreative();

    private static String operatorLogin = Logins.DEFAULT_CREATIVE_CONSTUCTOR_CLIENT;

    private static String clientLogin = Logins.DEFAULT_CREATIVE_CONSTUCTOR_CLIENT;

    private static Long clientId;
    private static Long operatorUid;

    private static CreativeUploadResponse actualResponse;

    private static CreativeUploadResult actualResult;
    private static CreativeUploadResult expectedResult;

    private static PerfCreativesRecord actualRecord;

    @BeforeClass
    public static void before() {
        clientId = directClassRule.dbSteps().shardingSteps().getClientIdByLogin(clientLogin);
        operatorUid = directClassRule.dbSteps().shardingSteps().getUidByLogin(operatorLogin);

        directClassRule.dbSteps().useShardForLogin(clientLogin);
        creativeUploadData.setCreativeId(new CreativesHelper(directClassRule).getNotExistentCreativeId());
        creativeUploadData.withStockCreativeId(creativeUploadData.getCreativeId());
        actualResponse = directClassRule.intapiSteps().displayCanvasSteps().uploadCreatives(
                operatorUid,
                clientId,
                Collections.singletonList(creativeUploadData)
        );

        actualResult = directClassRule.intapiSteps().displayCanvasSteps()
                .resultById(actualResponse, creativeUploadData.getCreativeId());

        expectedResult = new CreativeUploadResult()
                .withCreativeId(creativeUploadData.getCreativeId())
                .withMessage("success")
                .withStatus(CreativeUploadResult.StatusEnum.OK);

        assumeThat("в ответе результат по одному креативу", actualResponse.getUploadResults(), hasSize(1));

        assumeThat("корректны результат сохранения", actualResult.getStatus(),
                equalTo(expectedResult.getStatus()));

        actualRecord =
                directClassRule.dbSteps().perfCreativesSteps().getPerfCreatives(creativeUploadData.getCreativeId());
    }

    @Test
    @Description("креатив сохраняется ручкой upload_creatives")
    public void creativeSavedAndResponseOk() {
        assertThat("ошибка соответсвует ожиданиям", actualResult, beanDiffer(expectedResult));
    }

    @Test
    @Description("креатив корректно сохранился в базе после вызова ручки upload_creatives")
    public void creativeSavedInDataBase() {
        assertThat("креатив корректно сохранился в базу", toCreativeUploadData(actualRecord),
                beanDiffer(creativeUploadData));
    }

    @Test
    @Description("тип креатива canvas в базе после вызова ручки upload_creatives")
    public void creativeTypeIsCanvasInDataBase() {
        assertThat("тип креатива canvas", actualRecord.getCreativeType(),
                equalTo(PerfCreativesCreativeType.video_addition));
    }

    @Test
    @Description("креатив привязан к запрошенному клиенту в базе после вызова ручки upload_creatives")
    public void creativeIsLinkedWithCorrectClientInDataBase() {
        assertThat("привязан к запрошенному клиенту", actualRecord.getClientid(), equalTo(clientId));
    }

    @AfterClass
    public static void after() {
        directClassRule.dbSteps().perfCreativesSteps().deletePerfCreatives(creativeUploadData.getCreativeId());
    }

}
