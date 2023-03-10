package ru.yandex.market.core.order;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.BuyerType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.core.billing.OrderCheckpointDao;
import ru.yandex.market.core.billing.matchers.OrderCheckpointMatcher;
import ru.yandex.market.core.business.metrika.BusinessMetrikaService;
import ru.yandex.market.core.config.DevIntegrationTest;
import ru.yandex.market.core.order.model.CheckpointType;
import ru.yandex.market.core.order.model.DeliveryBillingType;
import ru.yandex.market.core.order.model.DeliveryRoute;
import ru.yandex.market.core.order.model.MbiOrderBuilder;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.order.model.OrderBillingStatus;
import ru.yandex.market.core.order.model.OrderCheckpoint;
import ru.yandex.market.core.order.model.OrderDelivery;
import ru.yandex.market.core.order.model.OrderDeliveryCosts;
import ru.yandex.market.core.order.model.OrderDeliveryDeclaredValue;
import ru.yandex.market.core.order.model.Parcel;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.lock.DbmsLockService;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

class OrderCheckpointDaoTest extends DevIntegrationTest {
    private static final long ORDER_ID = 7539514568L;
    private static final long CHECKPOINT_ID = 7539514500L;
    private static final long SUPPLIER_ID = 7539514567L;
    private static final long SORTING_CENTER_ID = 133L;

    private DbOrderService orderService;

    private OrderCheckpointDao orderCheckpointDao;

    private EnvironmentService environmentService;

    private ServiceFeePartitionDao serviceFeePartitionDao;

    @BeforeEach
    void before() {
        orderService = new DbOrderService(environmentService, serviceFeePartitionDao);
        orderService.setNamedParameterJdbcTemplate(namedParameterJdbcTemplate);
        orderService.setBusinessMetrikaService(mock(BusinessMetrikaService.class));
        orderCheckpointDao = new OrderCheckpointDao(namedParameterJdbcTemplate);
    }

    @Test
    void testMergeOrders() {
        var lockService = new DbmsLockService(jdbcTemplate);
        transactionTemplate.execute(status -> {
            if (lockService.tryLockWithinTx("orderCheckpointDaoTest")) {
                runTestLocked();
                return null;
            } else {
                return fail("locked for now");
            }
        });
    }

    private void runTestLocked() {
        cleanUp();
        prepareDB();

        List<OrderCheckpoint> checkpointToSave = ImmutableList.of(
                OrderCheckpoint.builder()
                        .setCheckpointType(CheckpointType.SORTING_CENTER_RETURN_PREPARING_SENDER)
                        .setDate(LocalDateTime.of(2020, 6, 22, 10, 0))
                        .setOrderId(ORDER_ID)
                        .setId(CHECKPOINT_ID + 2)
                        .setPartnerType(PartnerType.SORTING_CENTER)
                        .setPartnerId(SORTING_CENTER_ID)
                        .setPickupable(false)
                        .build(),
                OrderCheckpoint.builder()
                        .setCheckpointType(CheckpointType.SORTING_CENTER_RETURN_ARRIVED)
                        .setDate(LocalDateTime.of(2020, 6, 22, 8, 0))
                        .setOrderId(ORDER_ID)
                        .setId(CHECKPOINT_ID + 1) //?????????????? id
                        .setPartnerType(PartnerType.SORTING_CENTER)
                        .setPartnerId(SORTING_CENTER_ID)
                        .setPickupable(true)
                        .setPickupLogisticPointId(1000000001L)
                        .build(),
                OrderCheckpoint.of(
                        CheckpointType.SORTING_CENTER_RETURN_RETURNED,
                        LocalDateTime.of(2020, 6, 22, 17, 0),
                        ORDER_ID,
                        CHECKPOINT_ID - 3,//?????????????? id
                        PartnerType.SORTING_CENTER,
                        SORTING_CENTER_ID
                )
        );

        orderCheckpointDao.mergeCheckpoints(checkpointToSave);

        List<OrderCheckpoint> checkpoints = orderCheckpointDao.getOrderCheckpoints(ORDER_ID);
        Assertions.assertEquals(3, checkpoints.size());

        MatcherAssert.assertThat(
                checkpoints,
                Matchers.containsInAnyOrder(
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(ORDER_ID),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.SORTING_CENTER_RETURN_ARRIVED),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 22, 8, 0)),
                                        OrderCheckpointMatcher.hasId(CHECKPOINT_ID + 1),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(SORTING_CENTER_ID)
                                )).build(),
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(ORDER_ID),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.SORTING_CENTER_RETURN_PREPARING_SENDER),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 22, 10, 0)),
                                        OrderCheckpointMatcher.hasId(CHECKPOINT_ID + 2),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(SORTING_CENTER_ID)
                                )).build(),
                        MbiMatchers.<OrderCheckpoint>newAllOfBuilder()
                                .add(allOf(
                                        OrderCheckpointMatcher.hasOrderId(ORDER_ID),
                                        OrderCheckpointMatcher.hasCheckpointType(CheckpointType.SORTING_CENTER_RETURN_RETURNED),
                                        OrderCheckpointMatcher.hasDate(LocalDateTime.of(2020, 6, 22, 16, 0)),
                                        OrderCheckpointMatcher.hasId(CHECKPOINT_ID - 1),
                                        OrderCheckpointMatcher.hasPartnerType(PartnerType.SORTING_CENTER),
                                        OrderCheckpointMatcher.hasPartnerId(SORTING_CENTER_ID)
                                )).build()
                )
        );

        cleanUp();
    }

    private void prepareDB() {
        namedParameterJdbcTemplate.update("" +
                        "insert into shops_web.partner (id, type) values (:id, 'SUPPLIER')",
                new MapSqlParameterSource("id", SUPPLIER_ID));
        orderService.storeOrder(
                new MbiOrderBuilder()
                        .setId(ORDER_ID)
                        .setShopId(SUPPLIER_ID)
                        .setCampaignId(111L)
                        .setCreationDate(Date.from(Instant.now()))
                        .setTotal(BigDecimal.TEN)
                        .setUeTotal(BigDecimal.ONE)
                        .setDelivery(BigDecimal.ZERO)
                        .setFeeSum(BigDecimal.ZERO)
                        .setStatus(MbiOrderStatus.UNPAID)
                        .setBillingStatus(OrderBillingStatus.BILLED)
                        .setTrantime(Date.from(Instant.now()))
                        .setFree(false)
                        .setCurrency(Currency.RUR)
                        .setOrderDelivery(
                                new OrderDelivery(
                                        Date.from(Instant.now()),
                                        OrderDeliveryCosts.builder().build(),
                                        DeliveryBillingType.SPENT,
                                        new DeliveryRoute(213L, 213L),
                                        new OrderDeliveryDeclaredValue(10, BigDecimal.ONE),
                                        Collections.singletonList(Parcel.builder().build())
                                )
                        ).setColor(Color.BLUE)
                        .setBuyerType(BuyerType.PERSON)
                        .build()
        );

        namedParameterJdbcTemplate.update("" +
                        "insert into market_billing.order_checkpoint " +
                        "(order_id, type, checkpoint_date, id, partner_type, partner_id)\n" +
                        "values (:orderId, :type, :checkpointDate, :id, :partnerType, :partnerId)",
                new MapSqlParameterSource()
                        .addValue("orderId", ORDER_ID)
                        .addValue("type", CheckpointType.SORTING_CENTER_RETURN_ARRIVED.getId())
                        .addValue("checkpointDate", Timestamp.valueOf(LocalDateTime.of(2020, 6, 22, 9, 0)))
                        .addValue("id", CHECKPOINT_ID)
                        .addValue("partnerType", PartnerType.SORTING_CENTER.name())
                        .addValue("partnerId", SORTING_CENTER_ID)
        );

        namedParameterJdbcTemplate.update("" +
                        "insert into market_billing.order_checkpoint " +
                        "(order_id, type, checkpoint_date, id, partner_type, partner_id)\n" +
                        "values (:orderId, :type, :checkpointDate, :id, :partnerType, :partnerId)",
                new MapSqlParameterSource()
                        .addValue("orderId", ORDER_ID)
                        .addValue("type", CheckpointType.SORTING_CENTER_RETURN_RETURNED.getId())
                        .addValue("checkpointDate", Timestamp.valueOf(LocalDateTime.of(2020, 6, 22, 16, 0)))
                        .addValue("id", CHECKPOINT_ID - 1)
                        .addValue("partnerType", PartnerType.SORTING_CENTER.name())
                        .addValue("partnerId", SORTING_CENTER_ID)
        );
    }

    private void cleanUp() {
        namedParameterJdbcTemplate.query(
                "select * from market_billing.cpa_order fetch first 10 rows only",
                rs -> 0
        );
        namedParameterJdbcTemplate.update(
                "delete from market_billing.cpa_order where order_id = :orderId",
                new MapSqlParameterSource("orderId", ORDER_ID)
        );
        namedParameterJdbcTemplate.update(
                "delete from market_billing.order_checkpoint where order_id = :orderId",
                new MapSqlParameterSource("orderId", ORDER_ID)
        );
        namedParameterJdbcTemplate.update(
                "delete from shops_web.partner where id = :id",
                new MapSqlParameterSource("id", SUPPLIER_ID)
        );
    }
}
