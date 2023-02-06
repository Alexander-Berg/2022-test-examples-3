package ru.yandex.market.tsum.registry.v2.tasks.autodelete;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.clients.nanny.NannyClient;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.registry.v2.TestInstallationBuilder;
import ru.yandex.market.tsum.registry.v2.TestLegacyNannyServiceBuilder;
import ru.yandex.market.tsum.registry.v2.dao.InstallationsDao;
import ru.yandex.market.tsum.registry.v2.dao.NannyAutoDeleteDao;
import ru.yandex.market.tsum.registry.v2.dao.model.Installation;
import ru.yandex.market.tsum.registry.v2.dao.model.autodelete.LegacyNannyService;
import ru.yandex.misc.test.Assert;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NannyAutoDeleteWorkerTest {
    private final static String RESOURCE_DIRECTORY_PATH = "registry/v2/tasks/autodelete/";
    private final static int WAITING_DAYS_BEFORE_DELETE = 2;

    private final static String ACTIVE_SERVICE = "production_market_active_service_vla";
    private final static String ANOTHER_ACTIVE_SERVICE = "production_market_another_active_service_vla";
    private final static String ABSENT_SERVICE = "production_market_absent_service_vla";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Autowired
    private MongoTemplate mongoTemplate;

    private InstallationsDao installationsDao;
    private NannyClient nannyClient;
    private NannyAutoDeleteDao autoDeleteDao;
    private NannyAutoDeleteWorker autoDeleteWorker;

    @Before
    public void setUp() throws IOException {

        nannyClient = new NannyClient("http://localhost:" + wireMockRule.port(),
            "token", null);
        this.installationsDao = new InstallationsDao(mongoTemplate);
        this.autoDeleteDao = new NannyAutoDeleteDao(mongoTemplate);

        this.autoDeleteWorker = new NannyAutoDeleteWorker(WAITING_DAYS_BEFORE_DELETE,
            nannyClient, installationsDao, autoDeleteDao);

        addJsonStubWithStatusNotFound("/v2/services/legacy_to_mark_service_iva/");
        addJsonStubWithStatusNotFound("/v2/services/legacy_to_delete_service_iva/");
        addJsonStubWithAuthHeader("/v2/services/active_service_vla/", "nanny_response_one_service.json");
        addJsonStubWithAuthHeader("/v2/services/?category=%2Fmarket", "all_services.json");
        addJsonStubWithAuthHeader("/v2/services/?category=%2Fmarket&skip=2", "empty_result.json");

    }

    private void addJsonStubWithAuthHeader(String url, String jsonFile) throws IOException {
        wireMockRule.stubFor(get(urlEqualTo(url))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(getTestResourceAsString(RESOURCE_DIRECTORY_PATH + jsonFile))));
    }

    private void addJsonStubWithStatusNotFound(String url) {
        wireMockRule.stubFor(get(urlEqualTo(url))
            .willReturn(aResponse()
                .withStatus(404)
                .withBody("")));
    }

    @Test
    public void test_nannyServiceIsAbsent_nannyServiceMarkedToDelete() {

        Installation installationToDeleteService = new TestInstallationBuilder()
            .withRandomName()
            .withNannyServices(Arrays.asList(ACTIVE_SERVICE, ABSENT_SERVICE))
            .build();
        installationsDao.save(installationToDeleteService);

        autoDeleteWorker.removeDeletedNannyServices();
        Assert.assertEquals(1, autoDeleteDao.list().size());

        LegacyNannyService legacyNannyService = autoDeleteDao.list().get(0);
        Assert.assertEquals(ABSENT_SERVICE, legacyNannyService.getServiceName());

        List<String> afterAutoDeleteServices = installationsDao.getAll().stream()
            .flatMap(installation -> installation.getNannyServices().stream())
            .collect(Collectors.toList());

        Assert.assertEquals(2, afterAutoDeleteServices.size());
    }

    @Test
    public void test_nannyServiceIsAbsentAndAlreadyMarked_nothingChanged() {

        Installation installationToDeleteService = new TestInstallationBuilder()
            .withRandomName()
            .withNannyServices(Arrays.asList(ACTIVE_SERVICE, ABSENT_SERVICE))
            .build();
        installationsDao.save(installationToDeleteService);

        LegacyNannyService savedLegacyNannyService = TestLegacyNannyServiceBuilder.builder()
            .withServicesName(ABSENT_SERVICE)
            .withDataToDelete(Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS))
            .build();
        autoDeleteDao.save(savedLegacyNannyService);

        autoDeleteWorker.removeDeletedNannyServices();

        Assert.assertEquals(1, autoDeleteDao.list().size());
        LegacyNannyService legacyNannyService = autoDeleteDao.list().get(0);
        Assert.assertEquals(savedLegacyNannyService.getServiceName(), legacyNannyService.getServiceName());
        Assert.assertEquals(savedLegacyNannyService.getTimestamp(), legacyNannyService.getTimestamp());
        List<String> afterAutoDeleteServices = installationsDao.getAll().stream()
            .flatMap(installation -> installation.getNannyServices().stream())
            .collect(Collectors.toList());
        Assert.assertEquals(2, afterAutoDeleteServices.size());
    }

    @Test
    public void test_nannyServiceIsAbsentLongTimeAgo_nannyServiceRemoved() {
        Installation installationToDeleteService = new TestInstallationBuilder()
            .withRandomName()
            .withNannyServices(Arrays.asList(ACTIVE_SERVICE, ABSENT_SERVICE))
            .build();
        installationsDao.save(installationToDeleteService);

        LegacyNannyService savedLegacyNannyService = TestLegacyNannyServiceBuilder.builder()
            .withServicesName(ABSENT_SERVICE)
            .withDataToDelete(Instant.now()
                .minus(WAITING_DAYS_BEFORE_DELETE + 1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.MILLIS))
            .build();
        autoDeleteDao.save(savedLegacyNannyService);

        autoDeleteWorker.removeDeletedNannyServices();
        Assert.assertEquals(0, autoDeleteDao.list().size());

        List<String> afterAutoDeleteServices = installationsDao.getAll().stream()
            .flatMap(installation -> installation.getNannyServices().stream())
            .collect(Collectors.toList());

        Assert.assertEquals(1, afterAutoDeleteServices.size());
        Assert.assertEquals(ACTIVE_SERVICE, afterAutoDeleteServices.get(0));
    }

    @Test
    public void test_allServicesPresentAndOneAlreadyMarked_unmarkNannyService() {

        Installation installationToDeleteService = new TestInstallationBuilder()
            .withRandomName()
            .withNannyServices(Arrays.asList(ACTIVE_SERVICE, ANOTHER_ACTIVE_SERVICE))
            .build();
        installationsDao.save(installationToDeleteService);

        LegacyNannyService savedLegacyNannyService = TestLegacyNannyServiceBuilder.builder()
            .withServicesName(ACTIVE_SERVICE)
            .withDataToDelete(Instant.now()
                .minus( 1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.MILLIS))
            .build();
        autoDeleteDao.save(savedLegacyNannyService);

        autoDeleteWorker.removeDeletedNannyServices();
        Assert.assertEquals(0, autoDeleteDao.list().size());


        List<String> afterAutoDeleteServices = installationsDao.getAll().stream()
            .flatMap(installation -> installation.getNannyServices().stream())
            .collect(Collectors.toList());

        Assert.assertEquals(2, afterAutoDeleteServices.size());
    }
}
