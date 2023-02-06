package ru.yandex.mail.tests.sendbernar.models;

import java.io.IOException;

import static ru.yandex.mail.common.utils.ClassPath.fromClasspath;

public class MessagesWithInlines {
    public static String getInlineHtml(String id, String src) throws IOException {
        return String.format(fromClasspath("inline/inline.html"), id, src, id);
    }

    public static String getSmile() throws IOException {
        int num = 1;
        return String.format(fromClasspath("inline/smile.html"), num, num, num);
    }

    public static String getSmileWithHtml() throws IOException {
        int num = 1;
        return String.format(fromClasspath("inline/smile_with.html"), num, num, num);
    }
}

