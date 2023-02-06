package ru.yandex.autotests.innerpochta.rules.acclock;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * User: lanwen
 * Date: 03.09.13
 * Time: 20:04
 */
public class AccLocker {
    private String name;
    private String group;

    AccLocker(String name, String group) {
        this.name = name;
        this.group = group;
    }

    public static Query query(String name, String group) {
        return new Query(Criteria.where("o.name").is(name).and("o.group").is(group));
    }
}
