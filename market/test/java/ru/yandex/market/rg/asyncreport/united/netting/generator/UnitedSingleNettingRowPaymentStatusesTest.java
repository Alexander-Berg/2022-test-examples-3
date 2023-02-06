package ru.yandex.market.rg.asyncreport.united.netting.generator;

import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.order.payment.BankOrderInfo;
import ru.yandex.market.core.order.payment.TransactionType;

import static org.junit.Assert.assertEquals;



/**
 *  Только УВшные статусы
 *  Вариант в БД получены SQL-лем в конце файла
 */
@ParametersAreNonnullByDefault
class UnitedSingleNettingRowPaymentStatusesTest {
    @DisplayName("Проверяем все возможные статусы платежа")
    @ParameterizedTest
    @MethodSource("params")
    public void mapBankInfoToStatuses(String expectedPaymentStatus, String transactionType, String paymentType,
                                      boolean paymentOrderExists, boolean bankOrderExists, boolean isOrderCanceled,
                                      boolean isCorrection) {
        BankOrderInfo bankOrderInfo = new BankOrderInfo.Builder()
                .setTransactionType(TransactionType.getById(transactionType))
                .setPaymentType(paymentType)
                .setPaymentOrderId(paymentOrderExists ? 123L : null)
                .setBankOrderId(bankOrderExists ? 456L : null)
                .setOrderCanceled(isOrderCanceled)
                .setCorrection(isCorrection)
                .setDeliveryServiceItem(false) // or else NPE
                .build();
        UnitedSingleNettingRow row = new UnitedSingleNettingRow(bankOrderInfo, false, null);
        // Распечатать таблицу, приложить в тикет. Также раскоментируй @AfterAll, @BeforeAll в конце файла
        /*
        System.out.println(
                "|| "
                + row.getTransactionType() + " | "
                + row.getPaymentType() + " | "
                + (paymentOrderExists ? "да" : "нет") + " | "
                + (bankOrderExists ? "да" : "нет") + " | "
                + (isOrderCanceled ? "да" : "нет") + " | "
                + (isCorrection ? "да" : "нет") + " | "
                + row.getPaymentStatus() + "||"
        ); */
        assertEquals("Ожидаемый статус платежа", expectedPaymentStatus, row.getPaymentStatus());
    }

    private static Stream<Arguments> params() {
        return Stream.of(
                Arguments.of("Будет переведён по графику выплат",
                        //tType,    paymentType,           payOrder, bankOrder, cancelled, corr
                        "payment", "acc_delivery_subsidy", false, false, false, false),
                Arguments.of("Переведён по графику выплат",
                        "payment", "acc_delivery_subsidy", false, true, false, false),
                Arguments.of("Не будет переведён из-за отмены заказа",
                        "payment", "acc_delivery_subsidy", false, true, true, false),
                Arguments.of("Будет переведён по графику выплат",
                        "payment", "acc_delivery_subsidy", true, false, false, false),
                Arguments.of("Переведён по графику выплат",
                        "payment", "acc_delivery_subsidy", true, true, false, false),
                Arguments.of("Не будет переведён из-за отмены заказа",
                        "payment", "acc_delivery_subsidy", true, true, true, false),
                Arguments.of("Будет переведён по графику выплат",
                        "payment", "acc_subsidy", false, false, false, false),
                Arguments.of("Переведён по графику выплат",
                        "payment", "acc_subsidy", false, true, false, false),
                Arguments.of("Не будет переведён из-за отмены заказа",
                        "payment", "acc_subsidy", false, true, true, false),
                Arguments.of("Будет переведён по графику выплат",
                        "payment", "acc_subsidy", true, false, false, false),
                Arguments.of("Переведён по графику выплат",
                        "payment", "acc_subsidy", true, true, false, false),
                Arguments.of("Не будет переведён из-за отмены заказа",
                        "payment", "acc_subsidy", true, true, true, false),
                Arguments.of("Будет переведён по графику выплат",
                        "payment", "acc_ya_withdraw", false, false, false, false),
                Arguments.of("Переведён по графику выплат",
                        "payment", "acc_ya_withdraw", false, true, false, false),
                Arguments.of("Не будет переведён из-за отмены заказа",
                        "payment", "acc_ya_withdraw", false, true, true, false),
                Arguments.of("Будет переведён по графику выплат",
                        "payment", "acc_ya_withdraw", true, false, false, false),
                Arguments.of("Переведён по графику выплат",
                        "payment", "acc_ya_withdraw", true, true, false, false),
                Arguments.of("Не будет переведён из-за отмены заказа",
                        "payment", "acc_ya_withdraw", true, true, true, false),
                Arguments.of("Будет переведён по графику выплат",
                        "payment", "partner_payment", false, false, false, false),
                Arguments.of("Будет переведён по графику выплат",
                        "payment", "partner_payment", false, false, false, true),
                Arguments.of("Переведён по графику выплат",
                        "payment", "partner_payment", false, true, false, false),
                Arguments.of("Переведён по графику выплат",
                        "payment", "partner_payment", false, true, false, true),
                Arguments.of("Не будет переведён из-за отмены заказа",
                        "payment", "partner_payment", false, true, true, false),
                Arguments.of("Переведён по графику выплат",
                        "payment", "partner_payment", false, true, true, true),
                Arguments.of("Будет удержан для оплаты услуг",
                        "payment", "partner_payment", true, false, false, false),
                Arguments.of("Будет удержан для оплаты услуг",
                        "payment", "partner_payment", true, false, false, true),
                Arguments.of("Переведён по графику выплат",
                        "payment", "partner_payment", true, true, false, false),
                Arguments.of("Переведён по графику выплат",
                        "payment", "partner_payment", true, true, false, true),
                Arguments.of("Не будет переведён из-за отмены заказа",
                        "payment", "partner_payment", true, true, true, false),
                Arguments.of("Переведён по графику выплат",
                        "payment", "partner_payment", true, true, true, true),

                Arguments.of("Будет удержан из платежей покупателей",
                        "refund", "acc_subsidy", false, false, false, false),
                Arguments.of("Удержан из платежей покупателей",
                        "refund", "acc_subsidy", false, true, false, false),
                Arguments.of("Не будет проведён из-за отмены заказа",
                        "refund", "acc_subsidy", false, true, true, false),
                Arguments.of("Будет удержан из платежей покупателей",
                        "refund", "acc_subsidy", true, false, false, false),
                Arguments.of("Удержан из платежей покупателей",
                        "refund", "acc_subsidy", true, true, false, false),
                Arguments.of("Не будет проведён из-за отмены заказа",
                        "refund", "acc_subsidy", true, true, true, false),
                Arguments.of("Будет удержан из платежей покупателей",
                        "refund", "acc_ya_withdraw", false, false, false, false),
                Arguments.of("Удержан из платежей покупателей",
                        "refund", "acc_ya_withdraw", false, true, false, false),
                Arguments.of("Не будет проведён из-за отмены заказа",
                        "refund", "acc_ya_withdraw", false, true, true, false),
                Arguments.of("Будет удержан из платежей покупателей",
                        "refund", "acc_ya_withdraw", true, false, false, false),
                Arguments.of("Удержан из платежей покупателей",
                        "refund", "acc_ya_withdraw", true, true, false, false),
                Arguments.of("Не будет проведён из-за отмены заказа",
                        "refund", "acc_ya_withdraw", true, true, true, false),
                Arguments.of("Будет удержан из платежей покупателей",
                        "refund", "partner_payment", false, false, false, false),
                Arguments.of("Удержан из платежей покупателей",
                        "refund", "partner_payment", false, true, false, false),
                Arguments.of("Не будет проведён из-за отмены заказа",
                        "refund", "partner_payment", false, true, true, false),
                Arguments.of("Будет удержан из платежей покупателей",
                        "refund", "partner_payment", true, false, false, false),
                Arguments.of("Удержан из платежей покупателей",
                        "refund", "partner_payment", true, true, false, false),
                Arguments.of("Не будет проведён из-за отмены заказа",
                        "refund", "partner_payment", true, true, true, false)
        );
    }

    // Распечатать таблицу, приложить в тикет
    // Пример https://st.yandex-team.ru/MARKETBILLING-3172#6241d97db150db1c918d456c
    /*
    @BeforeAll
    public static void printHeader() {
        System.out.println("#|\n|| Тип транзакции | Источник транзакции | Выплата | ПП | Отменён | Корректировка |" +
                " Статус платежа ||");

    }

    @AfterAll
    public static void printFooter() {
        System.out.println("#|");

    }*/

    /*
 Пример запроса, которым это можно вынуть, не забудь подставить даты
 select DISTINCT
    acc.transaction_type,
    case when boi.service_order_id like '%ret%' then 'compensation'
         when boi.payment_type in ('compensation') then boi.payment_type
         else acc.product
        end payment_type,
    case when co.status IN (2, 3, 10) then 1 else 0 end as   cancelled,
    nvl2(pgpo.payment_order_id, 1, 0)                   as   payment_order_exists,
    nvl2(bo.bank_order_id, 1, 0)                        as   bank_order_exists,
    0 as is_correction
from market_billing.accrual acc
         join market_billing.cpa_order co
              on co.order_id  = acc.order_id
         left join market_billing.cpa_order_item coi
                   on coi.id = acc.entity_id and acc.entity_type = 'item'
         left join market_billing.payout p
                   on p.checkouter_id = acc.checkouter_id
                       and p.transaction_type = acc.transaction_type
                       and p.entity_id = acc.entity_id
                       and p.entity_type = acc.entity_type
                       and case when p.product in ('pay_subsidy', 'subsidy')
                                    then 'acc_subsidy'
                                when p.product in ('pay_ya_withdraw','yandex_cashback', 'yandex_account_withdraw')
                                    then 'acc_ya_withdraw'
                                when p.product in ('pay_delivery_subsidy', 'ya_delivery_subsidy', 'delivery_subsidy')
                                    then 'acc_delivery_subsidy'
                                else p.product
                               end = acc.product
         left join market_billing.payout_group_payment_order pgpo
                   on pgpo.payout_group_id = p.payout_group_id
         left join market_billing.bank_order_item boi
                   on boi.payment_order_id  = pgpo.payment_order_id
         left join market_billing.bank_order bo
                   on bo.payment_batch_id  = boi.payment_batch_id
                       and boi.payment_type != 'correction_commission'
where co.CREATION_DATE >= date'2022-02-01' and co.CREATION_DATE < date'2022-03-22'
union all
-- accrual_correction_ids: accrual_corrections and payment_corrections bank_order sum
-- новые УВ-шные корректировки
select DISTINCT
       coalesce(pc.transaction_type, acc.transaction_type) as   transaction_type,
       acc.product                                         as   payment_type,
       case when co.status IN (2, 3, 10) then 1 else 0 end  as   cancelled,
       nvl2(pgpo.payment_order_id, 1, 0)                   as   payment_order_exists,
       nvl2(bo.bank_order_id, 1, 0)                        as   bank_order_exists,
       1                                                   as   is_correction
from market_billing.accrual_correction acc
         join market_billing.cpa_order co on co.order_id = acc.order_id
         left join market_billing.cpa_order_item coi
                   on coi.id = acc.entity_id and acc.entity_type = 'item'
         left join market_billing.payout_correction pc on pc.accrual_correction_id = acc.id
         left join market_billing.payout_group_payment_order pgpo
                   on pgpo.payout_group_id = pc.payout_group_id
         left join market_billing.bank_order_item boi on boi.payment_order_id = pgpo.payment_order_id
         left join market_billing.bank_order bo
                   on bo.payment_batch_id = boi.payment_batch_id
                       and boi.payment_type != 'correction_commission'
where co.CREATION_DATE >= date'2022-02-01' and co.CREATION_DATE < date'2022-03-22';

 */
}
