package ru.yandex.common.framework.http;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.common.framework.core.Servantlet;

import static ru.yandex.common.util.collections.CollectionFactory.newHashMap;
import static ru.yandex.common.util.collections.CollectionFactory.newList;

/**
 * Date: May 10, 2011
 * Time: 7:33:57 PM
 *
 * @author Dima Schitinin, dimas@yandex-team.ru
 */
public class DifferentDataServantlet implements Servantlet {

    @Override
    public void process(final ServRequest req, final ServResponse res) {
        res.addData(5);
        res.addData(Format.JSON);
        res.addData(new A(100, new B("stroka")));
        res.addData(getSimpleMap());
        res.addData(getCollection());
        getCollection();
    }

    private Collection<String> getCollection() {
        final Collection<String> result = newList();
        for (int i = 10; i < 20; i++) {
            result.add(String.valueOf(i));
        }
        return result;
    }

    private Map<String, String> getSimpleMap() {
        final HashMap<String, String> result = newHashMap();
        result.put("fir<>//{}st", "Адын");
        result.put("second", "Два");
        return result;
    }


    public enum Format {
        JSON(0),
        XML(1);

        private final int code;

        private Format(final int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    public static class A {
        private final int i;
        private final B b;

        public A(final int i, final B b) {
            this.i = i;
            this.b = b;
        }

        public int getI() {
            return i;
        }

        public B getB() {
            return b;
        }
    }

    public static class B {
        private final String s;

        public B(final String s) {
            this.s = s;
        }

        public String getS() {
            return s;
        }
    }

}
