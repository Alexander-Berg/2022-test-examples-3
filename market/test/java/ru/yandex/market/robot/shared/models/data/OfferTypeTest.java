package ru.yandex.market.robot.shared.models.data;

import org.junit.Test;
import ru.yandex.market.ir.http.Offer;
import ru.yandex.misc.test.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OfferTypeTest {

    @Test
    public void testOfferTypeIsSame() {
        List<String> locals = Arrays.stream(OfferType.values()).map(Enum::name).collect(Collectors.toList());
        List<String> protos = Arrays.stream(Offer.OfferType.values()).map(Enum::name).collect(Collectors.toList());
        Assert.equals(locals, protos);
    }
}