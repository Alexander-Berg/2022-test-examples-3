package ru.yandex.travel.spring.tx.entities;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "test_entities")
@Getter
@Setter
public class TestEntity {

    @Id
    private UUID id;

    private Instant lastTransitionAt;

    @Version
    private Integer version;

}
