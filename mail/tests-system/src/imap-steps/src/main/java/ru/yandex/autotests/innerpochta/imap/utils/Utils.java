package ru.yandex.autotests.innerpochta.imap.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.randname.RandomNameGenerator;

import ru.yandex.autotests.innerpochta.wmicommon.Util;

import static com.google.common.base.Joiner.on;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

public class Utils {

    public static final String PASSPORT_ZOMBIE = "comtest.zombie.yandex-team.ru";

    public static final RandomNameGenerator RANDOM_NAME_GENERATOR = new RandomNameGenerator((int) System.currentTimeMillis());

    public static String getRandomName() {
        return RANDOM_NAME_GENERATOR.next();
    }

    public static String generateName() {
        return randomAlphanumeric(10);
    }

    public static List<String> generateRandomList(int size) {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            list.add(Util.getRandomString());
        }
        return list;
    }

    public static String quoted(String s) {
        return String.format("\"%s\"", s);
    }

    public static String literal(String s) {
        return String.format("{%d+}\r\n%s", s.length(), s);
    }

    public static String roundBraceList(Iterable<String> strings) {
        return String.format("(%s)", on(' ').join(strings));
    }

    public static String roundBraceList(String... strings) {
        return roundBraceList(Arrays.asList(strings));
    }

    public static String removeRoundBrace(String s) {
        return StringUtils.removeStart(StringUtils.removeEnd(s, ")"), "(");
    }

    public static String dateToString(Date date) {
        return null;  // FIXME
    }

    public static String cyrillic() {
        return random(8, "ЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮйцукенгшщзхъфывапролджэячсмитьбю");
    }

    public static void enableProxy() {
        System.setProperty("proxySet", "true");
        System.setProperty("http.proxyHost", PASSPORT_ZOMBIE);
        System.setProperty("http.proxyPort", "3128");
    }

    public static void disableProxy() {
        System.setProperty("proxySet", "false");
        System.setProperty("http.proxyHost", "");
        System.setProperty("http.proxyPort", "");
    }

    public static void enableProxyIf(boolean cond) {
        if (cond) {
            enableProxy();
        }
    }

    public static void disableProxyIf(boolean cond) {
        if (cond) {
            disableProxy();
        }
    }

}
