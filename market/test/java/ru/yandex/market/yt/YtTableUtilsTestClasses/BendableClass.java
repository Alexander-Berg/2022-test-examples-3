package ru.yandex.market.yt.YtTableUtilsTestClasses;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class BendableClass {
    public static final String MY_INT_NAME = "my_int_name";
    public static final String MY_STRING_NAME = "my_string_name";

    @BenderPart(name = MY_INT_NAME, strictName = true)
    private int myInt;
    @BenderPart(name = MY_STRING_NAME, strictName = true)
    private String myString;

    public BendableClass(int myInt, String myString) {
        this.myInt = myInt;
        this.myString = myString;
    }
}
