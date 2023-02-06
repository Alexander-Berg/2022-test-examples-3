package ru.yandex.market.yt.YtTableUtilsTestClasses;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class BendableSubClass extends BendableClass{

    public static final String MY_DOUBLE_NAME = "my_double_name";

    @BenderPart(name = MY_DOUBLE_NAME, strictName = true)
    private double myDouble;


    public BendableSubClass(int myInt, String myString, double myDouble) {
        super(myInt, myString);
        this.myDouble = myDouble;
    }
}
