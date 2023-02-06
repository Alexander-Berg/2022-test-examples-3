package ru.yandex.market.ff.tms;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.service.LgwRequestService;
import ru.yandex.market.ff.service.RequestConfigsService;
import ru.yandex.market.ff.service.RequestDetailsService;
import ru.yandex.market.ff.tms.exception.TmsJobException;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundUnitDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnBoxDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnInboundDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnUnitDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoadReturnInboundRequestDetailsExecutorTest extends IntegrationTest {

    private static final Partner PARTNER = new Partner(121L);
    private static final long SUPPLIER_ID = 1;

    @Autowired
    private RequestDetailsService requestDetailsService;

    @Autowired
    private LgwRequestService lgwRequestService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private RequestConfigsService requestConfigsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private LoadReturnInboundRequestDetailsExecutor executor;

    @Autowired
    private HistoryAgent historyAgent;

    @BeforeEach
    void init() {
        reset(fulfillmentClient);
        executor = new LoadReturnInboundRequestDetailsExecutor(lgwRequestService, requestDetailsService,
                transactionTemplate, Executors.newSingleThreadExecutor(), requestConfigsService, historyAgent);

        ResourceId resourceIdInbound1 = ResourceId.builder()
                .setYandexId("2")
                .build();
        InboundUnitDetails details1 = inboundUnitDetails("art1", 4, 4, 0, SUPPLIER_ID, 1);
        InboundUnitDetails details2 = inboundUnitDetails("art2", 3, 3, 0, SUPPLIER_ID, 1);
        InboundUnitDetails details3 = inboundUnitDetails("art3", 4, 3, 1, SUPPLIER_ID, 1);
        InboundUnitDetails details4 = inboundUnitDetails("art4", 0, 3, 1, SUPPLIER_ID, 1);

        ReturnBoxDetails returnBoxDetails1 = returnBoxDetails("box1", null);
        ReturnBoxDetails returnBoxDetails2 = returnBoxDetails("box2", "order2");
        ReturnBoxDetails returnBoxDetails3 = returnBoxDetails("box3", "order2");
        ReturnBoxDetails returnBoxDetails4 = returnBoxDetails("box4", "order4");

        ReturnUnitDetails returnUnitDetails1 = returnUnitDetails("order2", List.of("box2", "box3"), details1);
        ReturnUnitDetails returnUnitDetails2 = returnUnitDetails("order2", List.of("box2", "box3"), details2);
        ReturnUnitDetails returnUnitDetails3 = returnUnitDetails("order4", List.of("box4"), details3);
        ReturnUnitDetails returnUnitDetails4 = returnUnitDetails("order4", List.of("box4"), details4);

        ReturnInboundDetails returnInboundDetails = new ReturnInboundDetails(resourceIdInbound1,
                List.of(returnBoxDetails1, returnBoxDetails2, returnBoxDetails3, returnBoxDetails4),
                List.of(returnUnitDetails1, returnUnitDetails2, returnUnitDetails3, returnUnitDetails4));

        when(fulfillmentClient.getReturnInboundDetails(resourceIdInbound1, PARTNER))
                .thenReturn(returnInboundDetails);
    }

    @Test
    @DatabaseSetup(value = "classpath:tms/load-return-request-details/" +
            "before-lgw-answer-with-exactly-all-items-with-order-id-and-order-id-for-items-not-in-db.xml")
    @ExpectedDatabase(value = "classpath:tms/load-return-request-details/" +
            "after-lgw-answer-with-exactly-all-items-with-order-id-and-order-id-for-items-not-in-db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void lgwAnswerWithExactlyAllItemsWithOrderIdAndOrderIdForItemsNotInDb() {
        executor.doJob(null);
    }

    @Test
    @DatabaseSetup(value = "classpath:tms/load-return-request-details/" +
            "before-unredeemed-as-subtype-of-return.xml")
    @ExpectedDatabase(value = "classpath:tms/load-return-request-details/" +
            "after-unredeemed-as-subtype-of-return.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void unredeemedAsSubtypeOfReturn() {
        executor.doJob(null);
    }

    @Test
    @DatabaseSetup(value = "classpath:tms/load-return-request-details/" +
            "before-default-customer-return.xml")
    @ExpectedDatabase(value = "classpath:tms/load-return-request-details/" +
            "before-default-customer-return.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void defaultCustomerReturnNotCallLgw() {
        executor.doJob(null);
        verify(fulfillmentClient, never()).getReturnInboundDetails(any(), any());
    }

    @Test
    @DatabaseSetup(value = "classpath:tms/load-return-request-details/" +
            "before-lgw-answer-with-exactly-all-items-with-order-id-and-order-id-for-items-is-in-db.xml")
    @ExpectedDatabase(value = "classpath:tms/load-return-request-details/" +
            "after-lgw-answer-with-exactly-all-items-with-order-id-and-order-id-for-items-is-in-db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void lgwAnswerWithExactlyAllItemsWithOrderIdAndOrderIdForItemsIsInDb() {
        ResourceId resourceIdInbound1 = ResourceId.builder()
                .setYandexId("2")
                .build();
        InboundUnitDetails details1 = inboundUnitDetails("art1", 4, 4, 0, SUPPLIER_ID, 1);
        InboundUnitDetails details2 = inboundUnitDetails("art2", 3, 3, 0, SUPPLIER_ID, 1);

        ReturnBoxDetails returnBoxDetails1 = returnBoxDetails("box1", "order1");
        ReturnBoxDetails returnBoxDetails2 = returnBoxDetails("box3", "order2");

        ReturnUnitDetails returnUnitDetails1 = returnUnitDetails("order1", Collections.emptyList(), details1);
        ReturnUnitDetails returnUnitDetails2 = returnUnitDetails("order2", Collections.emptyList(), details2);

        ReturnInboundDetails returnInboundDetails = new ReturnInboundDetails(resourceIdInbound1,
                List.of(returnBoxDetails1, returnBoxDetails2),
                List.of(returnUnitDetails1, returnUnitDetails2));

        when(fulfillmentClient.getReturnInboundDetails(resourceIdInbound1, PARTNER))
                .thenReturn(returnInboundDetails);

        executor.doJob(null);
    }

    @Test
    @DatabaseSetup(value = "classpath:tms/load-return-request-details/" +
            "before-lgw-answer-without-item-instances.xml")
    @ExpectedDatabase(value = "classpath:tms/load-return-request-details/" +
            "after-lgw-answer-without-item-instances.xml", assertionMode = NON_STRICT_UNORDERED)
    public void lgwAnswerWithoutItemInstances() {
        ResourceId resourceIdInbound1 = ResourceId.builder()
                .setYandexId("2")
                .build();
        InboundUnitDetails details1 = inboundUnitDetails("art1", 4, 4, 0, SUPPLIER_ID, 1);
        details1.setInstances(null);
        details1.setUnfitInstances(null);
        InboundUnitDetails details2 = inboundUnitDetails("art2", 3, 3, 0, SUPPLIER_ID, 1);
        details2.setInstances(null);
        details2.setUnfitInstances(null);

        ReturnBoxDetails returnBoxDetails1 = returnBoxDetails("box1", "order1");
        ReturnBoxDetails returnBoxDetails2 = returnBoxDetails("box3", "order2");

        ReturnUnitDetails returnUnitDetails1 = returnUnitDetails("order1", Collections.emptyList(), details1);
        ReturnUnitDetails returnUnitDetails2 = returnUnitDetails("order2", Collections.emptyList(), details2);

        ReturnInboundDetails returnInboundDetails = new ReturnInboundDetails(resourceIdInbound1,
                List.of(returnBoxDetails1, returnBoxDetails2),
                List.of(returnUnitDetails1, returnUnitDetails2));

        when(fulfillmentClient.getReturnInboundDetails(resourceIdInbound1, PARTNER))
                .thenReturn(returnInboundDetails);

        executor.doJob(null);
    }

    @Test
    @DatabaseSetup(value = "classpath:tms/load-return-request-details/" +
            "before-no-items-with-order-id-in-lgw-answer.xml")
    @ExpectedDatabase(value = "classpath:tms/load-return-request-details/" +
            "before-no-items-with-order-id-in-lgw-answer.xml", assertionMode = NON_STRICT)
    public void noItemWithOrderIdInLgwAnswer() {
        TmsJobException exception = assertThrows(TmsJobException.class, () -> executor.doJob(null));
        assertions.assertThat(exception.getMessage())
                .contains("Details not found for request [2], item with article [art5]");
    }

    @Test
    @DatabaseSetup(value = "classpath:tms/load-return-request-details/" +
            "before-extra-item-in-lgw-answer-when-should-save.xml")
    @ExpectedDatabase(value = "classpath:tms/load-return-request-details/" +
            "after-extra-item-in-lgw-answer-when-should-save.xml", assertionMode = NON_STRICT_UNORDERED)
    public void extraItemInLgwAnswerWhenShouldSave() {
        executor.doJob(null);
    }

//    @Test
    //TODO FIXME
    @DatabaseSetup(value = "classpath:tms/load-return-request-details/" +
            "before-extra-item-in-lgw-answer-when-should-not-save.xml")
    @ExpectedDatabase(value = "classpath:tms/load-return-request-details/" +
            "after-extra-item-in-lgw-answer-when-should-not-save.xml", assertionMode = NON_STRICT_UNORDERED)
    public void extraItemInLgwAnswerWhenShouldNotSave() {
        jdbcTemplate.execute("ALTER SEQUENCE request_item_id_seq RESTART WITH 5");
        executor.doJob(null);
    }

    private static InboundUnitDetails inboundUnitDetails(
            String article, int declared, int actual, int defect, long vendorId, int surplus) {

        UnitId unitId = new UnitId(null, vendorId, article);
        InboundUnitDetails inboundUnitDetails = new InboundUnitDetails(unitId, declared, actual, defect, surplus);
        inboundUnitDetails.setInstances(
                List.of(CompositeId.builder(List.of(new PartialId(PartialIdType.CIS, "CISCIS"))).build())
        );
        inboundUnitDetails.setUnfitInstances(
                List.of(CompositeId.builder(List.of(new PartialId(PartialIdType.CIS, "CIS-UNFIT"))).build())
        );
        return inboundUnitDetails;
    }

    private static ReturnBoxDetails returnBoxDetails(String boxId, String orderId) {
        return new ReturnBoxDetails(boxId, orderId);
    }

    private static ReturnUnitDetails returnUnitDetails(String orderId, List<String> boxIds,
                                                       InboundUnitDetails details) {
        return new ReturnUnitDetails(orderId, boxIds, details, null);
    }
}
