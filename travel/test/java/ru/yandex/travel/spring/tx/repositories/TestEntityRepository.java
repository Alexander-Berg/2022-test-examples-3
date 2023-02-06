package ru.yandex.travel.spring.tx.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.yandex.travel.spring.tx.entities.TestEntity;

public interface TestEntityRepository extends JpaRepository<TestEntity, UUID> {
}
