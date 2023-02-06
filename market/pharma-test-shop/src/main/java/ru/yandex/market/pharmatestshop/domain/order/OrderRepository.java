package ru.yandex.market.pharmatestshop.domain.order;

import lombok.NonNull;
import org.hibernate.annotations.SQLInsert;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.yandex.market.pharmatestshop.domain.orderjson.OrderJson;

public interface OrderRepository extends JpaRepository<OrderJson, Long> {

    @SQLInsert(sql = "INSERT INTO pharma_test_shop_order  VALUES (?, ?,?) " +
            "ON CONFLICT (id) DO UPDATE SET")
    @Override
    @NonNull
    OrderJson save(@NonNull OrderJson entity);

}
