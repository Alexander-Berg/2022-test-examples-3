package ru.yandex.market.logistics.cs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.yandex.market.logistics.cs.domain.entity.QueueTask;

@Repository
public interface QueueTaskTestRepository extends JpaRepository<QueueTask, Long> {

    @Query(
        value = "SELECT count(qt.id) = 0"
            + "  FROM queue_task AS qt",
        nativeQuery = true
    )
    boolean thereIsNoTask();

    @Query(
        value = "SELECT sum(qt.attempt) - count(qt.id) > 0"
            + "  FROM queue_task AS qt",
        nativeQuery = true
    )
    boolean thereWasAtLeastOneRetry();

}
