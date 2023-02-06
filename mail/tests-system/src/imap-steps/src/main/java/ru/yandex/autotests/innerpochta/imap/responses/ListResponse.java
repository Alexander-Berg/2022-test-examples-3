package ru.yandex.autotests.innerpochta.imap.responses;

import java.util.ArrayList;
import java.util.Collection;
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

public final class ListResponse extends ImapResponse<ListResponse> {
    private final Collection<ListItem> items = new ArrayList<>();
    private ListItem literalListItem = null;
    private String literalName = "";
    private Integer literalSize = 0;


    @Override
    protected void parse(String line) {
        parseList(line);
    }

    @Override
    protected void validate() {
    }

    private void parseList(String line) {
        /*
        LIST can have responses of 2 kinds
        Single-line:
        * LIST (\Unselect) "|" FolderName

        Multi-line:
        * LIST (\Unselect) "|" {7}
        qwe"asd

        In multi-line responses(called literal) we have number of bytes instead of folder name, and real name on next lines
        This is made to avoid symbols escaping
         */
        Matcher listItem = Pattern.compile("(?i)^\\* LIST \\((.*)\\) (\\S*) (\\S*)$").matcher(line);
        Matcher listLiteralItem = Pattern.compile("(?i)^\\* LIST \\((.*)\\) (\\S*) \\{(\\d*)\\}$").matcher(line);

        // First of all we check if we had literal response in progress
        if (literalSize > 0) {

            // And if we have - append current line to name
            literalName += line;
            literalSize -= line.length();
            if (literalSize == 0) { // If we got full name and no remaining size

                // Add name to the result
                literalListItem.setName(literalName);
                items.add(literalListItem);

                // And reset intermediate data
                literalListItem = null;
                literalName = "";
            }
        } else if (listLiteralItem.matches()) {
            // Parse liternal response and fill literalSize to mark that next lines are current folder name
            literalListItem = new ListItem(listLiteralItem.group(2), null, listLiteralItem.group(1).split(" "));
            literalSize = Integer.valueOf(listLiteralItem.group(3));
        } else if (listItem.matches()) {
            // Parse normal response
            String folderName = listItem.group(3);
            if (folderName.charAt(0) == '"') {
                folderName = folderName.substring(1, folderName.length() - 1);
            }
            items.add(new ListItem(listItem.group(2), folderName, listItem.group(1).split(" ")));
        }
    }


    public Collection<ListItem> getItems() {
        return items;
    }

    @Step("Вывод LIST должен содержать только элементы: {0}")
    @SafeVarargs
    public final ListResponse withItems(org.hamcrest.Matcher<ListItem>... matchers) {
        assertThat("Вывод LIST содержит не тот набор элементов", items, containsInAnyOrder(matchers));
        return this;
    }

    @Step("В выводе LIST должны найти элемент: {0}")
    public ListResponse withItem(org.hamcrest.Matcher<ListItem> matcher) {
        assertThat(items, hasItem(matcher));
        return this;
    }

    @Step("Ожидаем, что выдача LIST пустая")
    public ListResponse withoutItems() {
        assertThat(items, is(empty()));
        return this;
    }

    @Step("Ожидаем, что выдача LIST НЕ пустая")
    public ListResponse withAnyItems() {
        assertThat(items, not(empty()));
        return this;
    }
}
