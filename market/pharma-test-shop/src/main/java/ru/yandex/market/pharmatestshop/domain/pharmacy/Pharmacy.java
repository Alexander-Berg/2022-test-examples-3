package ru.yandex.market.pharmatestshop.domain.pharmacy;


import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.pharmatestshop.domain.cart.delivery.DeliveryType;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_test_shop_settings")
public class Pharmacy {
    @Id
    private Long shopId;

    private String status;
    private String message;
    @Enumerated(EnumType.STRING)
    private DeliveryType deliveryTypes;//[]
    //Express
    private LocalDate fromDateExpress;//Regex?
    private LocalDate toDateExpress;
    private String paymentMethodExpress;
    //Delivery
    private LocalDate fromDateDelivery;
    private LocalDate toDateDelivery;
    private String paymentMethodDelivery;
    //Pickup
    private LocalDate fromDatePickup;
    private LocalDate toDatePickup;
    private String paymentMethodPickup;

    //Fbs/Dbs
    private String salesModel;
    private String oauthToken;
    private String oauthClientId;

    //
    private String campaignId;

    private String outletIds;
}
