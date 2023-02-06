package ru.yandex.market.delivery.mdbapp.scheduled;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnRequestState;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.ReturnRequestRepository;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_SKU_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_SUPPLIER_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ORDER_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.PRICE_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID_STR;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.item;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.lomOrder;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.pickupPoint;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequest;
import static ru.yandex.market.delivery.mdbapp.testutils.MockUtils.prepareMockServer;

public class ReturnRequestSchedulerTest extends AllMockContextualTest {

    private MockRestServiceServer checkouterMockServer;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private LomClient lomClient;

    @Autowired
    ReturnRequestScheduler scheduler;
    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @BeforeEach
    void setUp() {
        checkouterMockServer = MockRestServiceServer.createServer(checkouterRestTemplate);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @Transactional
    @Sql("/data/repository/returnRequest/return-request-creating-request.sql")
    @Sql(value = "/data/repository/returnRequestItem/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    @Sql(value = "/data/repository/returnRequest/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    @Sql(value = "/data/repository/pickupPoint/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    void createRequest() throws Exception {
        // given:
        prepareMockServer(
            checkouterMockServer,
            "/orders/167802870",
            "/data/checkouter/return_order.json"
        );
        OrderSearchFilter filter = OrderSearchFilter.builder()
            .externalIds(Set.of(String.valueOf(ORDER_ID)))
            .platformClientIds(Set.of(PlatformClient.BERU.getId()))
            .build();
        when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(false)))
            .thenReturn(PageResult.of(List.of(lomOrder(PartnerType.DROPSHIP)), 1, 1, 1));

        // and:
        ReturnRequest expected = returnRequest(2L, ReturnRequestState.FINAL, null, true)
            .setStatus(ReturnStatus.NEW);
        expected.addReturnRequestItem(item(1L, PRICE_1, ITEM_SKU_1, ITEM_SUPPLIER_1));
        pickupPoint().addReturnRequest(expected);

        // when:
        scheduler.makeReturnRequests();

        // then:
        returnRequestRepository.flush();
        final Optional<ReturnRequest> actual = returnRequestRepository.findByReturnId(RETURN_ID_STR);
        softly.assertThat(actual).isPresent();
        softly.assertThat(actual.get()).usingRecursiveComparison().isEqualTo(expected);

        verify(lomClient).searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(false));
    }
}
