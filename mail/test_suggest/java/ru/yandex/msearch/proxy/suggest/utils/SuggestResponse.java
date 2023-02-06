package ru.yandex.msearch.proxy.suggest.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ru.yandex.function.BasicGenericConsumer;
import ru.yandex.json.dom.ValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.xpath.ValueUtils;

/**
 * User: stassiak
 * Date: 19.05.14
 */
public class SuggestResponse {
    final List<Contact> contacts;

    public SuggestResponse(String responseText) throws Exception {
        BasicGenericConsumer<Object, JsonException> consumer =
            new BasicGenericConsumer<>();

        ValueContentHandler.prepareParser(consumer).parse(responseText);
        Map<?, ?> json = ValueUtils.asMap(consumer.get());

        contacts = new ArrayList<>();
        List<?> contactsJsonList = ValueUtils.asList(json.get("contacts"));
        for (Object o: contactsJsonList) {
            Map<?, ?> contactMap = ValueUtils.asMap(o);
            int id  = ValueUtils.asInt(contactMap.get("id"));
            String name = ValueUtils.asString(contactMap.get("name"));
            String email = ValueUtils.asString(contactMap.get("email"));
            String t = ValueUtils.asString(contactMap.get("t"));
            List<?> phonesList = ValueUtils.asList(contactMap.get("phones"));
            String[] phones = new String[phonesList.size()];
            for (int i = 0; i < phonesList.size(); i++) {
                phones[i] = ValueUtils.asString(phonesList.get(i));
            }

            int u = ValueUtils.asInt(contactMap.get("u"));
            contacts.add(new Contact(id, email, name, phones, t, u));
        }
    }

    public List<Contact> contacts() {
        return contacts;
    }
}
