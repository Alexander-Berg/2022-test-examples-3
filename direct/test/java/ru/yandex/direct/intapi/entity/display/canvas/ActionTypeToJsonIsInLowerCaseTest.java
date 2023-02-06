package ru.yandex.direct.intapi.entity.display.canvas;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.intapi.entity.display.canvas.model.ActionType;
import ru.yandex.direct.utils.JsonUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class ActionTypeToJsonIsInLowerCaseTest {

    @Parameterized.Parameter(value = 0)
    public ActionType actionType;

    @Parameterized.Parameters(name = "ActionType {0}")
    public static Collection<ActionType> params() {
        return Arrays.asList(ActionType.values());
    }

    @Test
    public void jsonValueIsInLowerCase() {
        String actual = JsonUtils.toJson(actionType);
        String expected = actual.toLowerCase();
        assertThat(actual, is(expected));
    }
}
