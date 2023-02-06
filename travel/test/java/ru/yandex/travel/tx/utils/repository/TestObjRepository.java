package ru.yandex.travel.tx.utils.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.yandex.travel.tx.utils.entities.TestObj;

public interface TestObjRepository extends JpaRepository<TestObj, UUID> {
}
