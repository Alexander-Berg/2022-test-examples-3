package ru.yandex.market.yt.YtTableUtilsTestClasses;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class BendableSubClassWithSameName extends BendableClass {

    @BenderPart(name = BendableClass.MY_INT_NAME, strictName = true)
    private int mySameInt;

    public BendableSubClassWithSameName(int myInt, String myString, int mySameInt) {
        super(myInt, myString);
        this.mySameInt = mySameInt;
    }
}
