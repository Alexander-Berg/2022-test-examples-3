package ru.yandex.market.olap2.load;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.olap2.BaseIntegrationTest;
import ru.yandex.market.olap2.dao.ClickhouseDao;
import ru.yandex.market.olap2.graphite.Graphite;
import ru.yandex.market.olap2.yt.YtClusterLiveliness;
import ru.yandex.market.olap2.yt.YtTableService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;


public class LoadIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private YtClusterLiveliness ytClusterLiveliness;
    @Autowired
    private YtTableService ytTableService;
    @Autowired
    private CloseableHttpClient httpClient;
    @Autowired
    private ClickhouseDao clickhouseDao;
    @Autowired
    private TaskGenerator generator;
    @Autowired
    private Graphite graphite;

    @SpyBean(name = "chHighPriorityExecutor")
    private ThreadPoolExecutor highPriorityExecutor;

    /**
     * Хэппи-пас загрузки, сервисы отвечают с первого раза, таска ТМом сразу же completed.
     * Ожидаем, что ивент перейдет в статус SUCCESS.
     *
     * @throws IOException        - требуется сигнатурой httpClient.execute.
     * @throws URISyntaxException - требуется сигнатурой createEvent
     */
    @Test
    @DatabaseSetup("classpath:fixtures/leaders.xml") // проставим таблицу лидеров, т.к. ходим в нее во время таски
    @DatabaseSetup("classpath:fixtures/load/1/before_events.xml") // наш тестовый ивент
    @ExpectedDatabase(value = "classpath:fixtures/load/1/expected_events.xml", // проверим, что статус ивента сменился
            table = "step_events",
            assertionMode = NON_STRICT_UNORDERED)
    public void eventShouldBeCompletedWhenTaskIsLoadedAndRowsCountIsSame() throws IOException {
        // before
        // собираем ивенты только по живым кластерам
        given(ytClusterLiveliness.liveYtClusters()).willReturn(LIVE_CLUSTERS);
        given(ytTableService.exists(HAHN, "//tmp/test/qq")).willReturn(true); // при подготовке проверяем наличие таблиц
        given(ytTableService.getTableRowCount(HAHN, "//tmp/test/qq")).willReturn(100L); // пустые не копируем

        given(httpClient.execute(any(HttpPost.class)))
                .willReturn(buildResponse(200, "this_is_task_id")) // Отправим в ТМ
                .willReturn(buildResponse(200, "{\"ids\":[\"1\"]}")); // отправим успех в STEP

        // ТМ ответит успехом сходу
        given(httpClient.execute(any(HttpGet.class))).willReturn(buildResponse(200, "{\"state\":\"completed\"}"));
        // количество строк в итоговых таблицах сошлось
        given(ytTableService.getTableRowCount(HAHN, "//tmp/test/qq")).willReturn(100L);
        given(clickhouseDao.getTableRows(any(String.class))).willReturn(100L);

        ResultCaptor<Future<?>> res = new ResultCaptor<>();
        doAnswer(res).when(highPriorityExecutor).submit(any(Runnable.class));

        // then
        generator.checkNewStepEvents();

        //wait
        waitForFuture(1_000, res);

        //verify
        ArgumentCaptor<HttpPost> postCaptor = ArgumentCaptor.forClass(HttpPost.class);
        // HttpPost-ов должно быть 2, но он почему-то считает HttpGet здесь тоже, возможно считаются все вызовы мока.
        verify(httpClient, times(3)).execute(postCaptor.capture());
        assertSoftly(softly -> {
            List<HttpPost> args = postCaptor.getAllValues();
            softly.assertThat(args).hasSize(3); // todo почему он принимает HttpGet за статусом таски в ТМ тоже за пост?
            // HttpPost get = args.get(1); // - такой код его сломает
            HttpPost postToTM = args.get(0); // первый обмен должен быть пост в ТМ
            softly.assertThat(postToTM.getURI().getPath()).isEqualTo("test_tm_url/");
            softly.assertThat(extractContentAsString(postToTM)).contains("\"source_table\":\"//tmp/test/qq\"");
            // Проверим отправку в STEP
            HttpPost postToStep = args.get(2);
            assertJson(
                    loadResourceAsString("classpath:fixtures/load/1/expected_post_step.json"),
                    extractContentAsString(postToStep),
                    LENIENT
            );
        });
        // мы не знаем, каким по счету будет этот тест и сколько уже зарепорчено в графит, поэтому проверим хотя бы,
        // что просто вызвалось
        verify(graphite, atLeastOnce()).report(anyString(), anyLong());
    }
}
