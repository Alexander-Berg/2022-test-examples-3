package ru.yandex.market.mbi.api.controller.delivery;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.client.entity.order.DaasOrderPaymentDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "DaasOrderPaymentsTest.csv")
class DaasOrderPaymentsTest extends FunctionalTest {
    private static final Set<Long> ORDER_TRUST_IDS = Set.of(121L, 122L, 123L, 2L, 3L);
    private static final Set<Long> ORDER_BANK_IDS = Set.of(485001L);

    @Test
    void gettingPaymentsBankIds() {
        assertThat(mbiApiClient.getDaasOrders(ORDER_TRUST_IDS, ORDER_BANK_IDS))
                .containsExactly(
                        new DaasOrderPaymentDTO(121L, 485001, "trans_id_payment_real_card")
                );
    }

    @Test
    void gettingPaymentsBankIdsEmptyTrustIds() {
        assertThat(mbiApiClient.getDaasOrders(Set.of(), ORDER_BANK_IDS))
                .containsExactlyInAnyOrder(
                        new DaasOrderPaymentDTO(121L, 485001, "trans_id_payment_real_card")
                );
    }

    @Test
    void gettingPaymentsTrustIdsAndEmptyBankIds() {
        assertThat(mbiApiClient.getDaasOrders(ORDER_TRUST_IDS, Set.of()))
                .containsExactlyInAnyOrder(
                        new DaasOrderPaymentDTO(121L, 485001, "trans_id_payment_real_card"),
                        new DaasOrderPaymentDTO(122L, 485008, "trans_id_payment_real_spasibo")
                );
    }

    @Test
    void gettingPaymentsTestEmptyParams() {
        assertThat(mbiApiClient.getDaasOrders(Set.of(), Set.of())).isEmpty();
    }

    @Test
    void gettingPaymentsTestInvalidParams() {
        assertThat(mbiApiClient.getDaasOrders(Set.of(1L, 2L), Set.of(22L, 21L))).isEmpty();
    }

    @Test
    void gettingPaymentsTestUnexcitingBankIds() {
        assertThat(mbiApiClient.getDaasOrders(Set.of(), Set.of(22L, 21L))).isEmpty();
    }
}
