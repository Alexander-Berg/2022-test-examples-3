package ru.yandex.autotests.direct.intapi.java.tests.creatives;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.data.Logins;
import ru.yandex.autotests.direct.intapi.java.factories.creative.CreativesFactory;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.models.CreativeUploadData;
import ru.yandex.autotests.direct.intapi.models.CreativeUploadResponse;
import ru.yandex.autotests.direct.intapi.models.CreativeUploadResult;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Every.everyItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка DisplayCanvas upload_creatives для нескольких креативов")
@Stories(TestFeatures.DisplayCanvas.UPLOAD_CREATIVES)
@Features(TestFeatures.DISPLAY_CANVAS)
@Tag(Tags.DISPLAY_CANVAS)
@Tag("DIRECT-57547")
@Issue("DIRECT-57547")
public class DisplayCanvaUploadAlotOfCreativesTest {
    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    @Rule
    public DirectRule directRule = DirectRule.defaultRule();

    private List<CreativeUploadData> creativeUploadDataList = new ArrayList<>();

    private String operatorLogin = Logins.DEFAULT_CREATIVE_CONSTUCTOR_CLIENT;

    private String clientLogin = Logins.DEFAULT_CREATIVE_CONSTUCTOR_CLIENT;

    private Long clientId;
    private Long operatorUid;

    private CreativeUploadResponse actualResponse;

    @Before
    public void before() {
        clientId = directRule.dbSteps().shardingSteps().getClientIdByLogin(clientLogin);
        operatorUid = directRule.dbSteps().shardingSteps().getUidByLogin(operatorLogin);

        directRule.dbSteps().useShardForLogin(clientLogin);
    }

    @Test
    @Description("2 креатива сохраняются корректно")
    public void uploadTwoCreatives() {
        long lastCreativeId = new CreativesHelper(directRule).getNotExistentCreativeId();
        creativeUploadDataList.add(CreativesFactory.defaultCreative().withCreativeId(lastCreativeId++));
        creativeUploadDataList.add(CreativesFactory.defaultCreative().withCreativeId(lastCreativeId++));

        actualResponse = directRule.intapiSteps().displayCanvasSteps().uploadCreatives(
                operatorUid,
                clientId,
                creativeUploadDataList
        );

        assumeThat("получили результат по 2м креативам", actualResponse.getUploadResults(), hasSize(2));

        List<CreativeUploadResult.StatusEnum> results = actualResponse.getUploadResults().stream()
                .map(CreativeUploadResult::getStatus)
                .collect(toList());
        assertThat("ответ OK по обоим креативам", results,
                everyItem(equalTo(CreativeUploadResult.StatusEnum.OK)));
    }

    @Test
    @Description("при массовом запросе валидных и невалидных креативов должны сохраниться только валидные")
    public void oneCorrectAndOneIncorrectCreative() {
        long creativeId = new CreativesHelper(directRule).getNotExistentCreativeId();
        creativeUploadDataList.add(CreativesFactory.defaultCreative().withCreativeId(creativeId++)
                .withCreativeName(null));
        creativeUploadDataList.add(CreativesFactory.defaultCreative().withCreativeId(creativeId++));

        actualResponse = directRule.intapiSteps().displayCanvasSteps().uploadCreatives(
                operatorUid,
                clientId,
                creativeUploadDataList
        );

        assumeThat("получили результат по 2м креативам", actualResponse.getUploadResults(), hasSize(2));

        CreativeUploadResult invalidResult = directRule.intapiSteps().displayCanvasSteps().resultById(
                actualResponse, creativeUploadDataList.get(0).getCreativeId()
        );

        assertThat("ошибка по первому креативу", invalidResult.getStatus(),
                equalTo(CreativeUploadResult.StatusEnum.ERROR));

        CreativeUploadResult validResult = directRule.intapiSteps().displayCanvasSteps().resultById(
                actualResponse, creativeUploadDataList.get(1).getCreativeId()
        );

        assertThat("ответ ОК по второму креативу", validResult.getStatus(),
                equalTo(CreativeUploadResult.StatusEnum.OK));
    }

    @After
    public void deleteCreatives() {
        directRule.dbSteps().perfCreativesSteps().deletePerfCreatives(
                creativeUploadDataList.stream()
                        .map(CreativeUploadData::getCreativeId)
                        .collect(toList())
        );
    }
}
