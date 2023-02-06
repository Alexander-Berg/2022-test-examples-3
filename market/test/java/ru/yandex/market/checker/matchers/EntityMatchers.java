package ru.yandex.market.checker.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.checker.api.model.Entity;
import ru.yandex.market.mbi.util.MbiMatchers;

public class EntityMatchers {

    public static Matcher<Entity> hasId(Long expectedValue) {
        return MbiMatchers.<Entity>newAllOfBuilder()
                .add(Entity::getId, expectedValue, "id")
                .build();
    }

    public static Matcher<Entity> hasName(String expectedValue) {
        return MbiMatchers.<Entity>newAllOfBuilder()
                .add(Entity::getName, expectedValue, "name")
                .build();
    }

    public static Matcher<Entity> hasDescription(String expectedValue) {
        return MbiMatchers.<Entity>newAllOfBuilder()
                .add(Entity::getDescription, expectedValue, "description")
                .build();
    }

}
