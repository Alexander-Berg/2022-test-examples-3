package ru.yandex.strictweb.scriptjava.test;

import java.util.*;


class Param {
    String get() {
        return "Hello";
    }
}

class ParamChild extends Param {
    String get() {
        return "World";
    }

    String get2() {
        return get();
    }
}

class ParamType<K extends Param> {
    K ret(K o) {
        return o;
    }
}

@SkipTest
public class TestParameters {
    static <T extends Param, K extends ParamType<T>> T ret(T s, K o) {
        return o.ret(s);
//        return null;
    }

    public boolean test() {
        return testParamType() && testParamMethod();
    }

    private boolean testParamMethod() {
        return Objects.equals(ret(new ParamChild(), new ParamType<>()).get2().substring(1), "ello");
    }

    private boolean testParamType() {
        List<ParamType<Param>> list = new Vector<>();

        list.add(new ParamType<>());

        String s = list.get(0).ret(new ParamChild() {
            String get() {
                return "Hello " + super.get();
            }
        }).get().substring(2);

        UnitTest.println(s);

        return Objects.equals(s, "llo World");
    }
}
