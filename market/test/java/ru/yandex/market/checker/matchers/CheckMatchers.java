package ru.yandex.market.checker.matchers;

import java.util.List;

import org.hamcrest.Matcher;

import ru.yandex.market.checker.model.Check;
import ru.yandex.market.mbi.util.MbiMatchers;

public class CheckMatchers {
    public static Matcher<Check> hasCheckFields(List<String> checkFields) {
        return MbiMatchers.<Check>newAllOfBuilder()
                .add(Check::getCheckFields, checkFields, "checkFields")
                .build();
    }
}
