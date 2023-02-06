package ru.yandex.market.logistics.iris.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.yandex.market.logistics.iris.entity.Dummy;

@Repository
public interface DummyRepository extends JpaRepository<Dummy, Long> {
}
