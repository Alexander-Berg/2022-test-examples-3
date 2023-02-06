package ru.yandex.strictweb.scriptjava.test;

import ru.yandex.strictweb.scriptjava.base.NativeCode;

class SuperClass {

    int number = 0;

    public SuperClass() {
        number += 1;
    }

    void inc() {
        number += 10;
    }
}

class SuperClass1 extends SuperClass {

    public SuperClass1() {

    }

    public SuperClass1(int i) {
        number += i;
    }

    void inc() {
        super.inc();
        number += 1000;
    }
}

@SkipTest
public class TestInheritance extends SuperClass1 {

//    int n = 10;

    public TestInheritance() {
        super(100);
        number += 10000;
    }

    @NativeCode("{}")
    public static void main(String[] args) {
        boolean test = new TestInheritance().test();
        UnitTest.println(String.valueOf(test));
    }

    void inc() {
        super.inc();
        number += 100000;
    }

    public boolean test() {
        inc();

        UnitTest.println(number + " :: ");

        return number == 1111;
    }
}
