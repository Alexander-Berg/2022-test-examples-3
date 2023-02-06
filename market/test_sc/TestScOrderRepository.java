package ru.yandex.market.tpl.core.domain.test_sc;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author kukabara
 */
public interface TestScOrderRepository extends JpaRepository<TestScOrder, String> {

    Optional<TestScOrder> findByOrderPartnerId(String orderPartnerId);

    List<TestScOrder> findByOrderPartnerIdIn(Collection<String> orderPartnerIds);

    Optional<TestScOrder> findByYandexId(String yandexId);

    List<TestScOrder> findByYandexIdIn(Collection<String> yandexIds);

    List<TestScOrder> findByDeliveryDateBetween(Instant from, Instant to);

}
