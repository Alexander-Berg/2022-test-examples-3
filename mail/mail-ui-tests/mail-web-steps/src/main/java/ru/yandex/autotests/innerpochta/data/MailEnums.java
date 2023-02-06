package ru.yandex.autotests.innerpochta.data;

import org.hamcrest.Matcher;

import java.util.HashMap;

import static org.hamcrest.Matchers.is;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 26.02.13
 * Time: 16:45
 */
public class MailEnums {
    public static enum PageAfterSend {
        DONE(0), CURRENT(1), SENT(2);
        private int value;

        private PageAfterSend(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static enum ElementAttributes {
        STYLE("style"), DATA_ACTION("data-action");
        private String value;

        private ElementAttributes(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static enum MessagesOrder {

        SENDER_DESCEND(new HashMap<Integer, String>() {{
            put(0, "!subject");
            put(1, "subject2");
            put(2, "Авыфвыфв");
            put(3, "Lfdsgfdg");
            put(4, "subject4");
            put(5, "subject3");
            put(6, "l423423");
            put(7, "ghgfhfghg");
        }}),
        SENDER_ASCEND(new HashMap<Integer, String>() {{
            put(7, "!subject");
            put(6, "subject2");
            put(5, "Авыфвыфв");
            put(4, "Lfdsgfdg");
            put(3, "subject4");
            put(2, "subject3");
            put(1, "l423423");
            put(0, "ghgfhfghg");
        }}),
        SUBJECT_DESCEND(new HashMap<Integer, String>() {{
            put(3, "!subject");
            put(4, "subject2");
            put(7, "Авыфвыфв");
            put(2, "Lfdsgfdg");
            put(6, "subject4");
            put(5, "subject3");
            put(1, "l423423");
            put(0, "ghgfhfghg");
        }}),
        SUBJECT_ASCEND(new HashMap<Integer, String>() {{
            put(4, "!subject");
            put(3, "subject2");
            put(0, "Авыфвыфв");
            put(5, "Lfdsgfdg");
            put(1, "subject4");
            put(2, "subject3");
            put(6, "l423423");
            put(7, "ghgfhfghg");
        }}),
        SIZE_DESCEND(new HashMap<Integer, String>() {{
            put(5, "!subject");
            put(0, "subject2");
            put(1, "Авыфвыфв");
            put(6, "Lfdsgfdg");
            put(2, "subject4");
            put(3, "subject3");
            put(7, "l423423");
            put(4, "ghgfhfghg");
        }}),
        SIZE_ASCEND(new HashMap<Integer, String>() {{
            put(2, "!subject");
            put(7, "subject2");
            put(6, "Авыфвыфв");
            put(1, "Lfdsgfdg");
            put(5, "subject4");
            put(4, "subject3");
            put(0, "l423423");
            put(3, "ghgfhfghg");
        }}),
        DATE_DESCEND(new HashMap<Integer, String>() {{
            put(4, "!subject");
            put(7, "subject2");
            put(1, "Авыфвыфв");
            put(3, "Lfdsgfdg");
            put(5, "subject4");
            put(6, "subject3");
            put(2, "l423423");
            put(0, "ghgfhfghg");
        }}),
        DATE_ASCEND(new HashMap<Integer, String>() {{
            put(3, "!subject");
            put(0, "subject2");
            put(6, "Авыфвыфв");
            put(4, "Lfdsgfdg");
            put(2, "subject4");
            put(1, "subject3");
            put(5, "l423423");
            put(7, "ghgfhfghg");
        }});

        private HashMap<Integer, String> order;

        private MessagesOrder(HashMap<Integer, String> order) {
            this.order = order;
        }

        public String getValue(int key) {
            return order.get(key);
        }

        public int messagesCount() {
            return order.size();
        }
    }


    public enum AvaType {
        TOTAL,
        AVATARS,
        COMPANY,
        NOAVATAR,
        EMPTY,
        TYPES,
        SOCIAL;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

        public static Matcher<String> type(AvaType type) {
            return is(type.toString());
        }
    }

}
