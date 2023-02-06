package ru.yandex.market.logistics.cs.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.yandex.market.logistics.cs.domain.entity.Capacity;

@Repository
public interface CapacityTestRepository extends JpaRepository<Capacity, Long> {

    @Query(
        nativeQuery = true,
        value =
            "       SELECT c.created"
                + " FROM capacity AS c"
                + " WHERE c.id = :capacity_id"
    )
    LocalDateTime findCreated(@Param("capacity_id") Long capacityId);

    @Query(
        nativeQuery = true,
        value =
            "       SELECT c.updated"
                + " FROM capacity AS c"
                + " WHERE c.id = :capacity_id"
    )
    LocalDateTime findUpdated(@Param("capacity_id") Long capacityId);

}
