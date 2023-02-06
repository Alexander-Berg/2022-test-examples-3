package ru.yandex.market.ir.tms.logs.saasoffers;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.ir.tms.utils.Log4jAwareSpringJUnit4ClassRunner;
import ru.yandex.market.ir.tms.utils.YtTestTableUtils;
import ru.yandex.market.mbo.core.saas.SaasActiveServiceRouter;
import ru.yandex.market.mbo.core.saas.SaasZooKeeperInfo;
import ru.yandex.market.mbo.utils.test.MockContextInitializer;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Большой и долгий тест (10+ минут).
 * Можно использовать, как платформу для разработки. Прогоняет:
 * - все YQL запросы (на небольших тестовых данных)
 * - YT маппер-джобу для конвертации в SaaS
 * - работу с ZooKeeper-ом
 * - SaaS в части разбора ответов/реакции - сам SaaS не используется, идёт мок
 * <p>
 * MockServer - поднимает прямо сервер. Т.е. SaaS компоненты в духе интеграционных тестов общаются прямо по сети,
 * отправляют запросы, принимают json-ки, парсят и т.п.. Даёт некоторую уверенность, что всё норм, как минимум,
 * с разбором ответов.
 * <p>
 * Долгость теста - в первую очередь из-за YQL/YT - несмотря на смешной объём данных в 5-10 записей, каждая операция
 * на кластере - это боль. В перспективе можно пробовать заменить на локальный докер с YT-ём.
 */
@SuppressWarnings({"SameParameterValue", "checkstyle:magicnumber"})
@RunWith(Log4jAwareSpringJUnit4ClassRunner.class)
@ContextConfiguration(
    locations = "classpath:ir-tms/test_config/saas/saas-test.xml",
    initializers = SaasCopySessionProcessorTest.Initializer.class
)
public class SaasCopySessionProcessorTest {
    static final FileAttribute<Set<PosixFilePermission>> ARWX =
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx"));
    @Autowired
    private SaasCopySessionProcessor job;

    @Resource(name = "ytIndexerRelatedHttpApi")
    private Yt yt;

    @Autowired
    private SaasZooKeeperInfo zooKeeperInfo;

    @Value("${mbo.yt.sc.offers.path}")
    private String scSessionsPath;

    @Value("${mbo.yt.saas.offers.path}")
    private String mboOffersSaasPath;

    @Autowired
    private SaasActiveServiceRouter saas;


    private static ClientAndServer mockServerClient;

    public static class Initializer extends MockContextInitializer {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            super.initialize(applicationContext);
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            environment.setActiveProfiles("yt");

            // SaaS работает а) долго, б) там нельзя сделать тестовый кусочек
            // а протестировать хочется на круг. Так что работаем по сохранённым ответам - проверит
            // немного логику, немного разбор и отработку JSON-ов.
            initMockServer(applicationContext);
        }

        @Override
        public void registerMocks(MockRegistryPostProcessor postProcessor) {
            postProcessor.addMockBean("standaloneIndexerController", mockStandaloneIndexController());
        }

        private StandaloneIndexerController mockStandaloneIndexController() {
            try {
                Path emptyScript = Files.createTempFile("standalone-indexer-mock", ".sh", ARWX);
                emptyScript.toFile().deleteOnExit();

                Path controller = Files.createTempFile("standalone-indexer-controller", ".sh", ARWX);
                Files.write(controller,
                    loadUtf8Resource("/standalone-indexer-controller.sh").getBytes(StandardCharsets.UTF_8));
                controller.toFile().deleteOnExit();

                return new StandaloneIndexerController(
                    controller.toString(), emptyScript::toString, "", "", "", "", "", "");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void initMockServer(ConfigurableApplicationContext applicationContext) {
            mockServerClient = ClientAndServer.startClientAndServer(0); // use random port

            applicationContext.addApplicationListener(event -> {
                if (event instanceof ContextStoppedEvent) { // Иначе нужен анонимный класс
                    mockServerClient.stop(true);
                }
            });

            Integer mockPort = mockServerClient.getPort();
            applicationContext.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("dynamicPorts", ImmutableMap.of(
                    "mbo.saas.search.port", mockPort,
                    "mbo.saas.indexer.port", mockPort,
                    "mbo.saas.dm.port", mockPort
                )));
        }
    }

    @Before
    public void resetServer() {
        mockServerClient.reset();
    }

    @Before
    public void cleanZookeeper() {
        Preconditions.checkState(zooKeeperInfo.getParentNode().contains("integration-test"));
        zooKeeperInfo.deleteData();
    }

    @Before
    public void cleanYt() {
        safeDelete(scSessionsPath);
        safeDelete(mboOffersSaasPath);
    }

    @Test
    public void wiringTest() {
        // Check for temp settings
        assertThat(job.getMboOffersSaasPath(), CoreMatchers.containsString("tmp"));
    }

    @Test
    public void testRunning() throws IOException {
        YtTestTableUtils.loadMockYtData(yt, "ir-tms/saas/session1.json");
        setupMockServer();

        assertEquals("market-backoffice", saas.getActiveClient().getServiceId());

        job.setCheckInterval(10);
        String statTable = "//home/market/testing/mbo/tmp/integration-tests/sc/20180202_1616/params_mr";
        assertTrue(job.copySession(statTable, "20180202_1616"));

        // Довольно мало внешних проявлений, но можно проверить, что доработало до сюда.
        assertEquals("market-backoffice-2", saas.getActiveClient().getServiceId());
    }

    private void setupMockServer() throws IOException {
        mockServerClient
            .when(HttpRequest.request("/docfetcher"), Times.once())
            .respond(HttpResponse.response()
                .withBody(loadUtf8Resource("/ir-tms/saas/docfetcher-response1.json"), MediaType.JSON_UTF_8));

        mockServerClient
            .when(HttpRequest.request("/docfetcher"))
            .respond(HttpResponse.response()
                .withBody(loadUtf8Resource("/ir-tms/saas/docfetcher-response2.json"), MediaType.JSON_UTF_8));
    }

    private static String loadUtf8Resource(String name) throws IOException {
        return IOUtils.toString(SaasCopySessionProcessorTest.class.getResource(name), StandardCharsets.UTF_8);
    }

    private void safeDelete(String path) {
        YPath ytPath = YPath.simple(path);
        // Check twice not to kill something important
        if (ytPath.toString().contains("/tmp/integration-tests")) {
            if (yt.cypress().exists(ytPath)) {
                yt.cypress().remove(ytPath);
            }
            yt.cypress().remove(ytPath);
        }
    }
}
