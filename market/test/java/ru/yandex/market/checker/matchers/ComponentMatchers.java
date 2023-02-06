package ru.yandex.market.checker.matchers;

import java.util.List;

import org.hamcrest.Matcher;

import ru.yandex.market.checker.api.model.Component;
import ru.yandex.market.mbi.util.MbiMatchers;

public class ComponentMatchers {

    public static Matcher<Component> hasId(Long expectedValue) {
        return MbiMatchers.<Component>newAllOfBuilder()
                .add(Component::getId, expectedValue, "id")
                .build();
    }

    public static Matcher<Component> hasName(String expectedValue) {
        return MbiMatchers.<Component>newAllOfBuilder()
                .add(Component::getName, expectedValue, "name")
                .build();
    }

    public static Matcher<Component> hasResponsibleLogin(List<String> expectedValue) {
        return MbiMatchers.<Component>newAllOfBuilder()
                .add(Component::getResponsibleList, expectedValue, "responsibleLogin")
                .build();
    }

}
