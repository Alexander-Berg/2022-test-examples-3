package ru.yandex.market.ff.integration;

import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.dto.RequestAcceptDTO;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.service.ItemNameService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.registry.LgwRegistryService;
import ru.yandex.market.ff.service.registry.RegistryService;
import ru.yandex.market.ff.service.registry.converter.ff.FFInboundRegistryEntityToRegistryConverter;
import ru.yandex.market.ff.service.registry.converter.ff.FFOutboundRegistryEntityToRegistryConverter;
import ru.yandex.market.ff.tms.SendRequestRegisterToServiceExecutor;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SendWithRegistrySubtypeOnAcceptTest extends MvcIntegrationTest {

    private SendRequestRegisterToServiceExecutor executor;

    @Autowired
    private LgwRegistryService lgwRegistryService;

    @Autowired
    private FFInboundRegistryEntityToRegistryConverter ffInboundConverter;

    @Autowired
    private FFOutboundRegistryEntityToRegistryConverter ffOutboundConverter;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RequestSubTypeService requestSubTypeService;

    @Autowired
    private ItemNameService itemNameService;

    @Autowired
    private HistoryAgent historyAgent;

    @BeforeEach
    void init() {
        executor = new SendRequestRegisterToServiceExecutor(shopRequestFetchingService, lgwRegistryService,
                Executors.newSingleThreadExecutor(), ffInboundConverter, ffOutboundConverter,
                transactionTemplate, registryService, requestSubTypeService, itemNameService, historyAgent);
    }

    @Test
    @DatabaseSetup("classpath:integration/send-with-registry-on-accept/before.xml")
    @ExpectedDatabase(value = "classpath:integration/send-with-registry-on-accept/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptByServiceOutboundWithSendToServiceStatus() throws Exception {
        mockMvc.perform(
                put("/requests/" + 1 + "/accept-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andExpect(status().isOk());
        executor.doJob(null);
    }
}
