package ru.yandex.market.yt.YtTableUtilsTestClasses;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderBindAllFields;

@BenderBindAllFields
@Bendable
public class BendAllFieldsClass {

    private int myInt;
    private String myString;

    public BendAllFieldsClass(int myInt, String myString) {
        this.myInt = myInt;
        this.myString = myString;
    }
}
