package ru.yandex.autotests.innerpochta.imap.consts.flags;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 05.05.14
 * Time: 17:52
 */
//http://tools.ietf.org/html/rfc3501#page-72
public enum MessageFlags {
    SEEN("\\Seen"),
    ANSWERED("\\Answered"),
    FLAGGED("\\Flagged"),
    DELETED("\\Deleted"),
    DRAFT("\\Draft"),
    RECENT("\\Recent");

    //пользовательские флаги:
    public static final String JUNK = "$Junk";
    public static final String NOT_JUNK = "$NotJunk";
    public static final String NEW_JUNK = "Junk";
    public static final String NEW_NOT_JUNK = "NotJunk";
    public static final String CYRILLIC = "флажЁчек";
    public static final String LONG_FLAG =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                    "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" +
                    "ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc" +
                    "ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd" +
                    "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
                    "ggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg";
    private String value;

    private MessageFlags(String value) {
        this.value = value;
    }

    public static String randomCyrillic() {
        List<String> cyrrylicFlags = Arrays.asList("йцукенгшщзх", "фывапролджэ", "ячсмитьбю");
        int randomIndex = new Random().nextInt(cyrrylicFlags.size());
        return cyrrylicFlags.get(randomIndex);
    }

    public static String random() {
        List<String> cyrrylicFlags = Arrays.asList("qwertyuiop", "asdfghjkl", "zxcvbnm");
        int randomIndex = new Random().nextInt(cyrrylicFlags.size());
        return cyrrylicFlags.get(randomIndex);
    }

    public String value() {
        return this.value;
    }
}
