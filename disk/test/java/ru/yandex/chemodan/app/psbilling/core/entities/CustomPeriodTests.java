package ru.yandex.chemodan.app.psbilling.core.entities;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple3;
import ru.yandex.misc.test.Assert;

public class CustomPeriodTests {
    private static final ListF<Tuple3<CustomPeriodUnit, Integer, String>> ddt = Cf.list(
            Tuple3.tuple(CustomPeriodUnit.ONE_SECOND, 1, "1S"),
            Tuple3.tuple(CustomPeriodUnit.ONE_MINUTE, 1, "60S"),
            Tuple3.tuple(CustomPeriodUnit.TEN_MINUTES, 1, "600S"),
            Tuple3.tuple(CustomPeriodUnit.ONE_HOUR, 1, "3600S"),
            Tuple3.tuple(CustomPeriodUnit.ONE_DAY, 3, "3D"),
            Tuple3.tuple(CustomPeriodUnit.ONE_WEEK, 4, "4W"),
            Tuple3.tuple(CustomPeriodUnit.ONE_MONTH, 5, "5M"),
            Tuple3.tuple(CustomPeriodUnit.ONE_YEAR, 6, "6Y")
    );

    @Test
    public void toStringTest() {
        for (Tuple3<CustomPeriodUnit, Integer, String> data : ddt) {
            CustomPeriod customPeriod = new CustomPeriod(data._1, data._2);
            Assert.equals(data._3, customPeriod.toString());
        }
    }

    @Test
    public void fromStringTest() {
        for (Tuple3<CustomPeriodUnit, Integer, String> data : ddt) {
            CustomPeriod customPeriod1 = new CustomPeriod(data._1, data._2);
            CustomPeriod customPeriod2 = CustomPeriod.fromString(customPeriod1.toString());
            Assert.equals(customPeriod1.toJodaPeriod(), customPeriod2.toJodaPeriod());
        }
    }
}
