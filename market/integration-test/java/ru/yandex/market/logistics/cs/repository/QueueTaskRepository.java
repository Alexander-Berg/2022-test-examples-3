package ru.yandex.market.logistics.cs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.yandex.market.logistics.cs.domain.entity.QueueTask;

@Repository
public interface QueueTaskRepository extends JpaRepository<QueueTask, Long> {
}
