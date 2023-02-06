package ru.yandex.market.pharmatestshop.domain.order;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.pharmatestshop.domain.cart.buyer.Buyer;
import ru.yandex.market.pharmatestshop.domain.cart.item.Item;
import ru.yandex.market.pharmatestshop.domain.order.currency.OrderCurrency;
import ru.yandex.market.pharmatestshop.domain.order.paymentmethod.PaymentMethod;
import ru.yandex.market.pharmatestshop.domain.order.paymenttype.OrderPaymentType;
import ru.yandex.market.pharmatestshop.domain.order.status.OrderStatus;
import ru.yandex.market.pharmatestshop.domain.order.status.OrderSubstatus;
import ru.yandex.market.pharmatestshop.domain.order.taxsystem.TaxSystem;


@Builder
@Data
@JsonDeserialize
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {

    private LocalDate date;//Дата и время оформления заказа.
    //Формат даты и времени: ДД-ММ-ГГГГ ЧЧ:ММ:СС.
    private OrderCurrency currency;
    private boolean fake;//Тип заказа. (true -- тестовый магазин)
    private long id;
    private double itemsTotal;//Общая сумма заказа в валюте заказа
    // без учета стоимости доставки и вознаграждения партнеру
    // за скидки по промокодам, купонам и акциям (параметр subsidyTotal).

    private OrderPaymentType paymentType;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private OrderSubstatus substatus;
    private TaxSystem taxSystem;
    private double total;//Общая сумма заказа в валюте заказа с учетом стоимости доставки, но без учета
    // вознаграждения партнеру
    // за скидки по промокодам, купонам и акциям
    private double subsidyTotal;// Общее вознаграждение партнеру за скидки:
    private Buyer buyer;
    private long businessId;
    private List<Item> items;

}
