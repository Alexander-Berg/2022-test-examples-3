package ru.yandex.direct.logviewercore.domain.web;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LogViewerFilterFormTest {
    @Test
    public void getConditions_RemovesTrailingSpaces() {
        Map<String, String> conditions = new HashMap<>();
        conditions.put("Key", "Value ");

        LogViewerFilterForm form = new LogViewerFilterForm();
        form.setConditions(conditions);

        Map<String, String> fixed = form.getConditions();

        assertThat(fixed.get("Key"), is(equalTo("Value")));
    }

    @Test
    public void getConditions_RemovesSpacesAtTheStartOfTheValue() {
        Map<String, String> conditions = new HashMap<>();
        conditions.put("Key", " Value");

        LogViewerFilterForm form = new LogViewerFilterForm();
        form.setConditions(conditions);

        Map<String, String> fixed = form.getConditions();

        assertThat(fixed.get("Key"), is(equalTo("Value")));
    }
}
