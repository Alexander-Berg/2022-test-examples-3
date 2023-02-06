package ru.yandex.market.tpl.core.domain.test_sc;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author kukabara
 */
public interface TestScOrderHistoryRepository extends JpaRepository<TestScOrderHistory, Long> {

    List<TestScOrderHistory> findByOrderPartnerIdOrderById(String orderPartnerId);

    List<TestScOrderHistory> findByYandexIdOrderById(String orderYandexId);

}
