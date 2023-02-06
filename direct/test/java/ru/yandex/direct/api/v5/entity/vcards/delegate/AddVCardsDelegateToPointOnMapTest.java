package ru.yandex.direct.api.v5.entity.vcards.delegate;

import java.math.BigDecimal;

import com.yandex.direct.api.v5.vcards.MapPoint;
import org.junit.Test;

import ru.yandex.direct.core.entity.vcard.model.PointOnMap;

import static org.assertj.core.api.Assertions.assertThat;

public class AddVCardsDelegateToPointOnMapTest {
    private static final BigDecimal X = BigDecimal.valueOf(1);
    private static final BigDecimal Y = BigDecimal.valueOf(2);
    private static final BigDecimal X1 = BigDecimal.valueOf(3);
    private static final BigDecimal Y1 = BigDecimal.valueOf(4);
    private static final BigDecimal X2 = BigDecimal.valueOf(5);
    private static final BigDecimal Y2 = BigDecimal.valueOf(6);

    private static final MapPoint REQUEST_POINT = new MapPoint()
            .withX(X)
            .withY(Y)
            .withX1(X1)
            .withY1(Y1)
            .withX2(X2)
            .withY2(Y2);
    private static final PointOnMap EXPECTED_POINT = new PointOnMap()
            .withX(X)
            .withY(Y)
            .withX1(X1)
            .withY1(Y1)
            .withX2(X2)
            .withY2(Y2);

    @Test
    public void test() {
        PointOnMap actualPoint = AddVCardsDelegate.toVcardPointOnMap(REQUEST_POINT);

        assertThat(actualPoint).isEqualTo(EXPECTED_POINT);
    }
}
