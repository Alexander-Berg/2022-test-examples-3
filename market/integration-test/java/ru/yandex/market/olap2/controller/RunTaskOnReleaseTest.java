package ru.yandex.market.olap2.controller;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.olap2.BaseIntegrationTest;
import ru.yandex.market.olap2.dao.ClickhouseDao;
import ru.yandex.market.olap2.util.ManifestUtil;
import ru.yandex.market.olap2.yt.YtClusterLiveliness;
import ru.yandex.market.olap2.yt.YtTableService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;

@Slf4j
@WebAppConfiguration
public class RunTaskOnReleaseTest extends BaseIntegrationTest {


    @Autowired
    private YtClusterLiveliness ytClusterLiveliness;
    @Autowired
    private YtTableService ytTableService;
    @Autowired
    private CloseableHttpClient httpClient;
    @Autowired
    private ClickhouseDao clickhouseDao;
    @MockBean
    private ManifestUtil manifestUtil;

    @SpyBean(name = "chHighPriorityExecutor")
    private ThreadPoolExecutor highPriorityExecutor;

    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext wac;

    @Before
    public void init() {
        mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    /**
     * Проверяет, что при запросе в ручку и отсутствии уже запущенной таски на этой ревизии сгенерится новый ивент,
     * таска запустится и успешно выполнится.
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/leaders.xml")
    @DatabaseSetup("classpath:fixtures/controller/release_job/1/before_events.xml")
    @ExpectedDatabase(value = "classpath:fixtures/controller/release_job/1/after_events.xml",
            assertionMode = NON_STRICT_UNORDERED, table = "step_events")
    public void shouldStartReleaseTaskSpring() throws Exception {
        // before
        // собираем ивенты только по живым кластерам
        given(manifestUtil.getRevisionFromManifest()).willReturn(1337L);
        runHappyPath();
    }

    /**
     * Тест, который генерит настоящий манифест на основе файла vcs.json. Требует дополнительных параметров запуска:
     * {@code ya make -ttt -F '*shouldStartReleaseTaskWithHackedManifest*' -DFORCE_VCS_INFO_UPDATE
     * --vcs-file=src/integration-test/resources/fixtures/controller/release_job/vcs.json}. Соответственно в идее,
     * возможно, тоже можно проставить аналогичные, но достоверно неизвестно. @Test - закомменчено,
     * чтобы он не триггерился на обычных сборках. Для запуска надо раскомментить и размокать manifestUtil.
     * <p>
     * Проверяет, что при запросе в ручку и отсутствии уже запущенной таски на этой ревизии сгенерится новый ивент,
     * таска запустится и успешно выполнится.
     */
    @Test
    @Ignore("Нужны особые параметры, читай javadoc")
    @DatabaseSetup("classpath:fixtures/leaders.xml")
    @DatabaseSetup("classpath:fixtures/controller/release_job/1/before_events.xml")
    @ExpectedDatabase(value = "classpath:fixtures/controller/release_job/1/after_events.xml",
            assertionMode = NON_STRICT_UNORDERED, table = "step_events")
    public void shouldStartReleaseTaskWithHackedManifest() throws Exception {
        runHappyPath();
    }

    private void runHappyPath() throws Exception {
        // before
        // собираем ивенты только по живым кластерам
        given(ytClusterLiveliness.liveYtClusters()).willReturn(LIVE_CLUSTERS);
        given(ytTableService.exists(
                HAHN,
                "//home/market/production/mstat/analyst/regular/cubes_vertica/cube_order_item_dict/2020-07")
        ).willReturn(true); // при подготовке проверяем наличие таблиц
        given(ytTableService.getTableRowCount(
                HAHN,
                "//home/market/production/mstat/analyst/regular/cubes_vertica/cube_order_item_dict/2020-07")
        ).willReturn(100L); // пустые не копируем

        given(httpClient.execute(any(HttpPost.class)))
                .willReturn(buildResponse(200, "this_is_task_id")) // Отправим в ТМ
                .willReturn(buildResponse(200, "{\"ids\":[\"1\"]}")); // отправим успех в STEP

        // ТМ ответит успехом сходу
        given(httpClient.execute(any(HttpGet.class))).willReturn(buildResponse(200, "{\"state\":\"completed\"}"));
        // количество строк в итоговых таблицах сошлось
        given(ytTableService.getTableRowCount(
                HAHN,
                "//home/market/production/mstat/analyst/regular/cubes_vertica/cube_order_item_dict/2020-07")
        ).willReturn(100L);
        given(clickhouseDao.getTableRows(any(String.class))).willReturn(100L);

        given(clickhouseDao.getTableRowsInDateRange(anyString(), anyString(), any(), any())).willReturn(100L);


        ResultCaptor<Future<?>> res = new ResultCaptor<>();
        doAnswer(res).when(highPriorityExecutor).submit(any(Runnable.class));

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/checkReleaseJob")).andReturn();
        String response = mvcResult.getResponse().getContentAsString();

        //wait
        waitForFuture(1_000, res);

        assertSoftly(softly -> {
            softly.assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        });
        assertJson(response, loadResourceAsString("fixtures/controller/release_job/1/handle_response.json"), LENIENT);
    }
}
