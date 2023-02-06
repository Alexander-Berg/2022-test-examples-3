package ru.yandex.travel.tx.utils.entities;


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
public class TestObj {
    @Id
    private UUID id;

    private String state;

    @Version
    private Integer version;
}
