package ru.yandex.market.hrms.core.config;

import java.time.Clock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeRepo;
import ru.yandex.market.hrms.core.domain.ispring.repo.IspringYqlRepo;
import ru.yandex.market.hrms.core.domain.outstaff.repo.OutstaffYqlRepo;
import ru.yandex.market.hrms.core.domain.timex.repo.TimexHistoryRepo;
import ru.yandex.market.hrms.core.domain.timex.repo.TimexOperationAreaRepo;
import ru.yandex.market.hrms.core.service.environment.EnvironmentService;
import ru.yandex.market.hrms.core.service.isrping.stubs.IspringYqlRepoStub;
import ru.yandex.market.hrms.core.service.outstaff.client.OutstaffStartrekClient;
import ru.yandex.market.hrms.core.service.outstaff.client.YaDiskClient;
import ru.yandex.market.hrms.core.service.outstaff.stubs.BlackBoxClientStub;
import ru.yandex.market.hrms.core.service.outstaff.stubs.OutstaffStartrekClientStub;
import ru.yandex.market.hrms.core.service.outstaff.stubs.OutstaffYqlRepoStub;
import ru.yandex.market.hrms.core.service.outstaff.stubs.S3ServiceStub;
import ru.yandex.market.hrms.core.service.outstaff.stubs.YaDiskClientStub;
import ru.yandex.market.hrms.core.service.s3.S3Service;
import ru.yandex.market.hrms.core.service.timex.FakeTimexApiFacade;
import ru.yandex.market.hrms.core.service.timex.TimexMapper;
import ru.yandex.market.hrms.core.service.timex.TimexServiceMock;
import ru.yandex.market.hrms.core.service.timex.TimexServiceTest;
import ru.yandex.market.ispring.ISpringClient;
import ru.yandex.market.ispring.ISpringClientMock;

@Configuration
public class TestMockConfig {
    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer staffWireMockServer() {
        return new WireMockServer(new WireMockConfiguration()
                .dynamicPort()
                .notifier(new Slf4jNotifier(false))
        );
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer warehouseApiWireMockServer() {
        return new WireMockServer(new WireMockConfiguration()
                .dynamicPort()
                .notifier(new Slf4jNotifier(false))
        );
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer yaSmsApiWireMockServer() {
        return new WireMockServer(new WireMockConfiguration()
                .dynamicPort()
                .notifier(new Slf4jNotifier(false))
        );
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer lenelApiWireMockServer() {
        return new WireMockServer(new WireMockConfiguration()
                .dynamicPort()
                .notifier(new Slf4jNotifier(false))
        );
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer wmsApiWireMockServer() {
        return new WireMockServer(new WireMockConfiguration()
                .dynamicPort()
                .notifier(new Slf4jNotifier(false))
        );
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer scApiWireMockServer() {
        return new WireMockServer(new WireMockConfiguration()
                .dynamicPort()
                .notifier(new Slf4jNotifier(false))
        );
    }

    @Bean
    public OutstaffYqlRepo outstaffYqlRepo() {
        return new OutstaffYqlRepoStub(null);
    }

    @Bean
    public BlackBoxClientStub blackBoxClient() {
        var stub = new BlackBoxClientStub(null);
        stub.withValue("test-login");
        return stub;
    }

    @Bean
    public FakeTimexApiFacade timexApiFacade() {
        return new FakeTimexApiFacade(TimexServiceTest.TEST_EVENTS);
    }

    @Bean
    public TimexServiceMock timexService(TimexMapper timexMapper,
                                         TimexHistoryRepo timexHistoryRepo,
                                         TimexOperationAreaRepo timexOperationAreaRepo,
                                         EmployeeRepo employeeRepo,
                                         EnvironmentService environmentService,
                                         Clock clock) {
        return new TimexServiceMock(timexMapper, timexApiFacade(),
                timexHistoryRepo, timexOperationAreaRepo, employeeRepo, environmentService, clock);
    }

    @Bean
    public OutstaffStartrekClient outstaffStartrekClient() {
        return new OutstaffStartrekClientStub();
    }

    @Bean
    public YaDiskClient yaDiskClient() {
        return new YaDiskClientStub();
    }

    @Bean
    public S3Service s3Service() {
        return new S3ServiceStub(null);
    }

    @Bean
    public ISpringClient iSpringClient() {
        return new ISpringClientMock();
    }

    @Bean
    public IspringYqlRepo ispringYqlRepo() {
        return new IspringYqlRepoStub(null);
    }
}
