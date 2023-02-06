package ru.yandex.travel.workflow.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.yandex.travel.workflow.entities.TestEntity;

public interface TestEntityRepository extends JpaRepository<TestEntity, UUID> {
}
