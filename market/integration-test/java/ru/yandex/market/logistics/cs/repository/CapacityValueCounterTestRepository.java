package ru.yandex.market.logistics.cs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.yandex.market.logistics.cs.domain.entity.CapacityValueCounter;

@Repository
public interface CapacityValueCounterTestRepository extends JpaRepository<CapacityValueCounter, Long> {

    @Modifying
    @Query(
        nativeQuery = true,
        value = "UPDATE capacity_value_counter AS cvc"
            + "  SET count = :count"
            + "  WHERE cvc.id = :counter_id"
    )
    void updateCount(
        @Param("counter_id") long capacityValueCounterId,
        @Param("count") long count
    );

}
