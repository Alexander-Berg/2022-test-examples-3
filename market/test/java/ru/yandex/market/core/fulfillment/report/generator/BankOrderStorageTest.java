package ru.yandex.market.core.fulfillment.report.generator;

import java.util.Collections;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.core.order.payment.OrderTransaction;
import ru.yandex.market.core.order.payment.TransactionBankOrder;
import ru.yandex.market.core.partner.contract.PartnerContractDao;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link BankOrderStorage}.
 *
 * @author vbudnev
 */
@DbUnitDataSet(before = "shops.common.csv")
class BankOrderStorageTest extends FunctionalTest {

    private static final Long ITEM_41 = 41L;
    private static final Long ITEM_42 = 42L;
    private static final Long ITEM_51 = 51L;
    private static final Long ITEM_IRRELEVANT = 10005000L;
    private static final Long SID_501 = 501L;
    private static final Long SID_502 = 502L;
    private static final Long SID_CLONE_601 = 601L;
    private static final Long SID_CLONE_602 = 602L;
    private static final String TRUST_P1 = "trust_p1";
    private static final String TRUST_P2 = "trust_p2";
    private static final String TRUST_P3 = "trust_p3";
    private static final String TRUST_P4 = "trust_p4";
    private static final String TRUST_P5 = "trust_p5";
    private static final String TRUST_P6 = "trust_p6";
    private static final String TRUST_R1 = "trust_ref1";
    private static final String TRUST_R2 = "trust_ref2";
    private static final OrderTransaction P3_SID_501 = pay(TRUST_P3, SID_501);
    private static final OrderTransaction P3_SID_502 = pay(TRUST_P3, SID_502);
    private static final OrderTransaction P4_SID_501 = pay(TRUST_P4, SID_501);
    private static final OrderTransaction P4_SID_502 = pay(TRUST_P4, SID_502);
    private static final OrderTransaction P5_SID_501 = pay(TRUST_P5, SID_501);
    private static final OrderTransaction P5_SID_502 = pay(TRUST_P5, SID_502);
    private static final OrderTransaction P6_SID_502 = pay(TRUST_P6, SID_502);
    private static final OrderTransaction P1_SID_CLONE_601 = pay(TRUST_P1, SID_CLONE_601);
    private static final OrderTransaction P1_SID_CLONE_602 = pay(TRUST_P1, SID_CLONE_602);
    private static final OrderTransaction P2_SID_CLONE_601 = pay(TRUST_P2, SID_CLONE_601);
    private static final OrderTransaction P2_SID_CLONE_602 = pay(TRUST_P2, SID_CLONE_602);
    private static final OrderTransaction R1_SID_501 = ref(TRUST_R1, SID_501);
    private static final OrderTransaction R1_SID_502 = ref(TRUST_R1, SID_502);
    private static final OrderTransaction R2_SID_502 = ref(TRUST_R2, SID_502);

    private static final boolean SEARCH_WITH_ITEMID = true;
    private static final boolean SEARCH_WITH_SUPPLIER_ID = false;

    @Autowired
    private PartnerContractDao supplierContractDao;
    @Autowired
    private OrderService orderService;
    private BankOrderStorage storage;

    /**
     * Чтобы в матерах не проверять все поля полученной {@link TransactionBankOrder}, идентифицируем записи по
     * трназкции и item_id.
     */
    private static Stream<Arguments> supplierMatchArgs() {
        return Stream.of(
                Arguments.of(
                        "один заказ",
                        P5_SID_501, SID_501, ITEM_IRRELEVANT, SEARCH_WITH_SUPPLIER_ID,
                        hasTrustPidAndSidAndItem(TRUST_P5, SID_501, ITEM_41)
                ),
                Arguments.of(
                        "один заказ. другой поставщик",
                        P5_SID_502, SID_502, ITEM_IRRELEVANT, SEARCH_WITH_SUPPLIER_ID,
                        hasTrustPidAndSidAndItem(TRUST_P5, SID_502, ITEM_42)
                ),
                /**
                 * из-за перетирания данных в мапе при коллизии
                 */
                Arguments.of(
                        "разные заказы в одной транзакции приводят к last stand данным по ПП. bug ?",
                        P4_SID_501, SID_501, ITEM_IRRELEVANT, SEARCH_WITH_SUPPLIER_ID,
                        hasTrustPidAndSidAndItem(TRUST_P4, SID_501, ITEM_51)
                ),
                /**
                 * из - за перетирания данных в мапе при коллизии, но данные
                 /* по ид транзакции + заказ должны быть одним ПП, потому не важно.
                 */
                Arguments.of(
                        "разные item заказа в одной транзакции приводят к last stand данным по ПП но это ОК",
                        P3_SID_501, SID_501, ITEM_IRRELEVANT, SEARCH_WITH_SUPPLIER_ID,
                        hasTrustPidAndSidAndItem(TRUST_P3, SID_501, ITEM_42)
                ),
                Arguments.of(
                        "клоны в одной транзакции и заказе. матчим как обычно",
                        P2_SID_CLONE_601, SID_CLONE_601, ITEM_IRRELEVANT, SEARCH_WITH_SUPPLIER_ID,
                        hasTrustPidAndSidAndItem(TRUST_P2, SID_CLONE_601, ITEM_41)
                ),
                Arguments.of(
                        "клоны в одной транзакции и заказе. матчим как обычно. другой поставщик",
                        P2_SID_CLONE_601, SID_CLONE_602, ITEM_IRRELEVANT, SEARCH_WITH_SUPPLIER_ID,
                        hasTrustPidAndSidAndItem(TRUST_P2, SID_CLONE_602, ITEM_42)
                ),
                Arguments.of(
                        "клоны в разных заказах. матчим как обычно",
                        P1_SID_CLONE_601, SID_CLONE_601, ITEM_IRRELEVANT, SEARCH_WITH_SUPPLIER_ID,
                        hasTrustPidAndSidAndItem(TRUST_P1, SID_CLONE_601, ITEM_41)
                ),
                Arguments.of(
                        "клоны в разных заказах. матчим как обычно. другой поставщик",
                        P1_SID_CLONE_602, SID_CLONE_602, ITEM_IRRELEVANT, SEARCH_WITH_SUPPLIER_ID,
                        hasTrustPidAndSidAndItem(TRUST_P1, SID_CLONE_602, ITEM_51)
                ),
                Arguments.of(
                        "матчится при отсвутствии поля item_id",
                        P6_SID_502, SID_502, ITEM_IRRELEVANT, SEARCH_WITH_SUPPLIER_ID,
                        hasTrustPidAndSid(TRUST_P6, SID_502)
                ),
                Arguments.of(
                        "рефанд.один заказ",
                        R1_SID_501, SID_501, ITEM_IRRELEVANT, SEARCH_WITH_SUPPLIER_ID,
                        hasTrustRidAndSidAndItem(TRUST_R1, SID_501, ITEM_41)
                ),
                Arguments.of(
                        "рефанд.один заказ, другой поставщик",
                        R1_SID_502, SID_502, ITEM_IRRELEVANT, SEARCH_WITH_SUPPLIER_ID,
                        hasTrustRidAndSidAndItem(TRUST_R1, SID_502, ITEM_42)
                )
        );
    }

    private static Stream<Arguments> itemIdMatchArgs() {
        return Stream.of(
                Arguments.of(
                        "неверный item_id не сматчится, несмотря на supplier_id",
                        P5_SID_501, SID_501, ITEM_IRRELEVANT, SEARCH_WITH_ITEMID,
                        nullValue()
                ),
                Arguments.of(
                        "матч ПП на основе item_id",
                        P3_SID_501, SID_501, ITEM_41, SEARCH_WITH_ITEMID,
                        hasTrustPidAndSidAndItem(TRUST_P3, SID_501, ITEM_41)
                ),
                Arguments.of(
                        "матч ПП на основе item_id. другой item",
                        P3_SID_501, SID_501, ITEM_42, SEARCH_WITH_ITEMID,
                        hasTrustPidAndSidAndItem(TRUST_P3, SID_501, ITEM_42)
                ),
                Arguments.of(
                        "рефанд. один заказ",
                        R1_SID_501, SID_501, ITEM_41, SEARCH_WITH_ITEMID,
                        hasTrustRidAndSidAndItem(TRUST_R1, SID_501, ITEM_41)
                ),
                Arguments.of(
                        "рефанд. один заказ, другой item_id",
                        R1_SID_502, SID_502, ITEM_42, SEARCH_WITH_ITEMID,
                        hasTrustRidAndSidAndItem(TRUST_R1, SID_502, ITEM_42)
                )
        );
    }

    private static OrderTransaction pay(String trustPayId, Long shopId) {
        return new OrderTransaction(null, null, null, null, null, null, shopId, trustPayId,
                null, null, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null
        );
    }

    private static OrderTransaction ref(String trustRefId, Long shopId) {
        return new OrderTransaction(null, null, null, null, null, null,
                shopId, null, trustRefId,
                null, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null
        );
    }

    private static Matcher<TransactionBankOrder> hasTrustPidAndSid(String trustPayId, long datasourceId) {
        return hasTrustPidAndSidAndItem(trustPayId, datasourceId, null);
    }

    private static Matcher<TransactionBankOrder> hasTrustPidAndSidAndItem(String trustPayId,
                                                                          Long datasourceId,
                                                                          Long orderItemId) {
        return MbiMatchers.<TransactionBankOrder>newAllOfBuilder()
                .add(TransactionBankOrder::getTrustPaymentId, trustPayId, "trustPaymentId")
                .add(TransactionBankOrder::getOrderItemId, orderItemId, "orderItemId")
                .add(TransactionBankOrder::getDatasourceId, datasourceId, "datasourceId")
                .build();
    }

    private static Matcher<TransactionBankOrder> hasTrustRidAndSidAndItem(String trustRefundId,
                                                                          Long datasourceId,
                                                                          Long orderItemId) {
        return MbiMatchers.<TransactionBankOrder>newAllOfBuilder()
                .add(TransactionBankOrder::getTrustRefundId, trustRefundId, "trustRefundId")
                .add(TransactionBankOrder::getOrderItemId, orderItemId, "orderItemId")
                .add(TransactionBankOrder::getDatasourceId, datasourceId, "datasourceId")
                .build();
    }

    @DisplayName("Маркировка клонов - клон")
    @Test
    void test_isClone() {
        storage = new BankOrderStorage(
                orderService,
                supplierContractDao,
                SID_CLONE_601,
                Collections.emptyList(),
                Collections.emptySet()
        );

        assertFalse(storage.isClone(SID_501));
        assertFalse(storage.isClone(SID_502));
        assertTrue(storage.isClone(SID_CLONE_601));
        assertTrue(storage.isClone(SID_CLONE_602));
    }

    @DisplayName("Маркировка клонов - не клон")
    @Test
    void test_isNotClone() {
        storage = new BankOrderStorage(
                orderService,
                supplierContractDao,
                SID_501,
                Collections.emptyList(),
                Collections.emptySet()
        );

        assertFalse(storage.isClone(SID_501));
        assertFalse(storage.isClone(SID_502));
        assertFalse(storage.isClone(SID_CLONE_601));
        assertFalse(storage.isClone(SID_CLONE_602));
    }

    @DisplayName("Для клона будут подтянуты данные о ПП всей группы")
    @Test
    @DbUnitDataSet(before = "BankOrderStorageTest.bankOrders.before.csv")
    void test_cloneTransactions() {
        storage = new BankOrderStorage(
                orderService,
                supplierContractDao,
                SID_CLONE_601,
                ImmutableList.of(P1_SID_CLONE_601, P1_SID_CLONE_602),
                Collections.emptySet()
        );

        //находятся по supplier_id
        assertThat(
                storage.getTransactionBankOrder(P1_SID_CLONE_601, SID_CLONE_601, ITEM_IRRELEVANT,
                        SEARCH_WITH_SUPPLIER_ID),
                notNullValue()
        );
        assertThat(
                storage.getTransactionBankOrder(P1_SID_CLONE_602, SID_CLONE_602, ITEM_IRRELEVANT,
                        SEARCH_WITH_SUPPLIER_ID),
                notNullValue()
        );

        //находятся по item_id
        assertThat(
                storage.getTransactionBankOrder(P1_SID_CLONE_601, SID_CLONE_601, ITEM_41, SEARCH_WITH_ITEMID),
                notNullValue()
        );
        assertThat(
                storage.getTransactionBankOrder(P1_SID_CLONE_602, SID_CLONE_602, ITEM_51, SEARCH_WITH_ITEMID),
                notNullValue()
        );

    }

    @DisplayName("Платеж. item_id не важен. Матч по supplier_id + tr_payment_id + tr_refund_id.")
    @MethodSource(value = "supplierMatchArgs")
    @ParameterizedTest(name = "{0}")
    @DbUnitDataSet(before = "BankOrderStorageTest.bankOrders.before.csv")
    void test_generalSuppliers(String desc,
                               OrderTransaction ot,
                               Long supplierId,
                               Long itemId,
                               boolean searchType,
                               Matcher<TransactionBankOrder> expectedMatcher
    ) {
        storage = new BankOrderStorage(
                orderService,
                supplierContractDao,
                supplierId,
                ImmutableList.of(
                        P1_SID_CLONE_601, P1_SID_CLONE_602,
                        P2_SID_CLONE_601, P2_SID_CLONE_602,
                        P3_SID_501, P3_SID_502,
                        P4_SID_501, P4_SID_502,
                        P5_SID_501, P5_SID_502,
                        P6_SID_502,
                        R1_SID_501, R1_SID_502,
                        R2_SID_502
                ),
                Collections.emptySet()
        );

        TransactionBankOrder actual = storage.getTransactionBankOrder(ot, supplierId, itemId, searchType);
        assertThat(
                actual,
                expectedMatcher
        );
    }

    @DisplayName("Платеж. supplier_id не важен. Матч по item_id + tr_payment_id + tr_refund_id")
    @MethodSource(value = "itemIdMatchArgs")
    @ParameterizedTest(name = "{0}")
    @DbUnitDataSet(before = "BankOrderStorageTest.bankOrders.before.csv")
    void test_generalItems(String desc,
                           OrderTransaction ot,
                           Long supplierId,
                           Long itemId,
                           boolean searchType,
                           Matcher<TransactionBankOrder> expectedMatcher
    ) {
        storage = new BankOrderStorage(
                orderService,
                supplierContractDao,
                supplierId,
                ImmutableList.of(
                        P1_SID_CLONE_601, P1_SID_CLONE_602,
                        P2_SID_CLONE_601, P2_SID_CLONE_602,
                        P3_SID_501, P3_SID_502,
                        P4_SID_501, P4_SID_502,
                        P5_SID_501, P5_SID_502,
                        P6_SID_502,
                        R1_SID_501, R1_SID_502,
                        R2_SID_502
                ),
                Collections.emptySet()
        );

        assertThat(
                storage.getTransactionBankOrder(ot, supplierId, itemId, searchType),
                expectedMatcher
        );
    }
}
