package ru.yandex.market.pharmatestshop.domain.pharmacy;

import lombok.NonNull;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.yandex.market.pharmatestshop.domain.cart.delivery.DeliveryType;


public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {


    Pharmacy findByShopId(Long shopId);


    @SQLInsert(sql = "INSERT INTO pharma_test_shop_settings" +
            " (shop_id,delivery_types," +
            "payment_method_express,payment_method_delivery,payment_method_pickup," +
            "sales_model,oauth_token,oauth_client_id,campaign_id) " +
            " VALUES (?, ?,?,?,?,?,?,?,?) ON CONFLICT (shop_id) UPDATE SET")
    @Override
    Pharmacy save(@NonNull Pharmacy entity);


//    @SQLUpdate(sql = "UPDATE pharma_test_shop_settings" +
//            " delivery_types=?," +
//            "payment_method_express=?,payment_method_delivery=?,payment_method_pickup=?," +
//            "sales_model=?,oauth_token=?,oauth_client_id,campaign_id=? " +
//            "WHERE shop_id=?" )
//    void update(@NonNull Pharmacy entity);
}
