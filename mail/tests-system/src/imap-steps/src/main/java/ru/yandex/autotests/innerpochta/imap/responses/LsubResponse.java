package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.yandex.autotests.innerpochta.imap.structures.ListItem;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public final class LsubResponse extends ImapResponse<LsubResponse> {
    private final List<ListItem> items = new ArrayList<>();


    @Override
    protected void parse(String line) {
        parseLsub(line);
    }

    @Override
    protected void validate() {
    }

    private void parseLsub(String line) {
        Matcher matcher = Pattern.compile("(?i)^\\* LSUB \\((.*)\\) (\\S*) (\\S*)$").matcher(line);
        if (matcher.matches()) {
            items.add(new ListItem(matcher.group(2), matcher.group(3), matcher.group(1).split(" ")));
        }
    }

    public List<ListItem> getItems() {
        return items;
    }

    @Step("В выводе LSUB должны найти {0}")
    public LsubResponse withItem(org.hamcrest.Matcher<ListItem> matcher) {
        assertThat(items, hasItem(matcher));
        return this;
    }

    @Step("Вывод LSUB должен содержать только {0}")
    /**
     * http://stackoverflow.com/questions/21132692/java-unchecked-unchecked-generic-array-creation-for-varargs-parameter
     */
    @SafeVarargs
    public final LsubResponse withItems(org.hamcrest.Matcher<ListItem>... matchers) {
        assertThat(items, containsInAnyOrder(matchers));
        return this;
    }

    @Step("Вывод LSUB должен быть пустым")
    public LsubResponse withoutItems() {
        assertThat(items, is(empty()));
        return this;
    }

    public LsubResponse withAnyItems() {
        assertThat(items, not(empty()));
        return this;
    }
}
