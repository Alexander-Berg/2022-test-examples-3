package ru.yandex.market.logistics.iris.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Dummy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public Dummy setId(Long id) {
        this.id = id;
        return this;
    }

    @Override
    public String toString() {
        return "Dummy{" +
            "id=" + id +
            '}';
    }
}
