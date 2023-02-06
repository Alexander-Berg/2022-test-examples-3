package ru.yandex.market.logistics.cs.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.yandex.market.logistics.cs.domain.entity.ServiceCapacityMapping;

@Repository
public interface ServiceCapacityMappingTestRepository extends JpaRepository<ServiceCapacityMapping, Long> {

    @Query(
        nativeQuery = true,
        value =
            "       SELECT scm.updated"
                + " FROM service_capacity_mapping AS scm"
                + " WHERE scm.id = :mapping_id"
    )
    LocalDateTime findUpdated(@Param("mapping_id") Long mappingId);

}
