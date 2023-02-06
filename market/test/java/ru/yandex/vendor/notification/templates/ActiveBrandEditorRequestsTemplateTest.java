package ru.yandex.vendor.notification.templates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.junit.jupiter.api.Test;

import ru.yandex.vendor.notification.templates.ActiveBrandEditorRequestsTemplate.RequestEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static org.junit.Assert.*;

public class ActiveBrandEditorRequestsTemplateTest {

    @Test
    @SuppressWarnings("unchecked")
    public void template_renders_parameters() throws Exception {
        List<RequestEntry> requests = singletonList(new RequestEntry(42, "BORK", LocalDateTime.now()));
        LocalDateTime date = LocalDateTime.of(2017, 10, 22, 9, 12);
        String link = "www.google.ru";
        ActiveBrandEditorRequestsTemplate template = new ActiveBrandEditorRequestsTemplate(requests, link, date);
        Map<String,Object> params = (Map<String, Object>) template.getParams();
        assertEquals(link, params.get("link"));
        assertEquals("22 октября 2017г. 09:12", params.get("date"));
        assertEquals(requests, params.get("requests"));
    }

    @Test
    public void request_entry_formats_date() throws Exception {
        RequestEntry entry = new RequestEntry(42, "BORK", LocalDateTime.of(2017, 5, 12, 20, 42));
        assertEquals(42, entry.getId());
        assertEquals("BORK", entry.getBrandName());
        assertEquals("12 мая 2017г. 20:42", entry.getCreationDate());
    }

    @Test
    public void template_produces_valid_json() throws Exception {
        List<RequestEntry> requests = singletonList(new RequestEntry(42, "BORK", LocalDateTime.of(2017, 5, 12, 20, 42)));
        LocalDateTime date = LocalDateTime.of(2017, 10, 22, 9, 12);
        String link = "www.google.ru";
        ActiveBrandEditorRequestsTemplate template = new ActiveBrandEditorRequestsTemplate(requests, link, date);
        String json = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
                .writeValueAsString(template.getParams());
        String expected =
            "{\"short_date\":\"2017-10-22\",\"date\":\"22 октября 2017г. 09:12\",\"link\":\"www.google.ru\"," +
            "\"requests\":[{\"id\":42,\"brand_name\":\"BORK\",\"creation_date\":\"12 мая 2017г. 20:42\"}]}";
        assertEquals(expected, json);
    }
}
