package ru.yandex.market.ff.tms;

import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.service.ItemNameService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.registry.LgwRegistryService;
import ru.yandex.market.ff.service.registry.RegistryService;
import ru.yandex.market.ff.service.registry.converter.ff.FFInboundRegistryEntityToRegistryConverter;
import ru.yandex.market.ff.service.registry.converter.ff.FFOutboundRegistryEntityToRegistryConverter;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class SendRequestRegisterToServiceExecutorTest extends IntegrationTest {

    private SendRequestRegisterToServiceExecutor executor;

    @Autowired
    private LgwRegistryService lgwRegistryService;

    @Autowired
    private FFInboundRegistryEntityToRegistryConverter inboundConverter;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RequestSubTypeService requestSubTypeService;

    @Autowired
    FFOutboundRegistryEntityToRegistryConverter ffOutboundConverter;

    @Autowired
    private ItemNameService itemNameService;

    @Autowired
    private HistoryAgent historyAgent;

    @BeforeEach
    void init() {
        executor = new SendRequestRegisterToServiceExecutor(shopRequestFetchingService, lgwRegistryService,
                Executors.newSingleThreadExecutor(), inboundConverter, ffOutboundConverter,
                transactionTemplate, registryService, requestSubTypeService, itemNameService, historyAgent);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:tms/send-to-service/inbound/before.xml")})
    @ExpectedDatabase(value = "classpath:tms/send-to-service/inbound/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testSuccessfullyCreatedInboundPlanRegistry() {
        executor.doJob(null);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:tms/send-to-service/outbound/before.xml")})
    @ExpectedDatabase(value = "classpath:tms/send-to-service/outbound/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testSuccessfullyCreatedOutboundPlanRegistry() {
        executor.doJob(null);
    }


    /**
     * Запросы:
     * 1 - выборка всех RequestSubTypeEntity
     * 2 - выборка ShopRequest по типам их RequestSubTypeEntity и статусу ACCEPTED_BY_SERVICE
     * 3 - выборка RegistryEntity по requestId
     * 4 - выборка всех RegistryEntityUnit для всех RegistryEntity
     * 5-10 - запись ShopRequest в бд
     * 11,12 - select nextval ('request_status_history_id_seq')
     * 13 - вставка в request_status_history
     */
    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:tms/send-to-service/many-registries/before.xml")})
    @JpaQueriesCount(14)
    void testQueriesCountForManyRegistries() {
        executor.getRows().forEach(tsk -> executor.processRow(tsk));
    }
}
