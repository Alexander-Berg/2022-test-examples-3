package ru.yandex.market.ir.matcher2.matcher.alternate.load.protobuf;

import org.junit.Test;

import ru.yandex.market.ir.http.Delivery;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.Dimension;

import static org.junit.Assert.assertEquals;

public class DimensionMapperTest {

    private final DimensionMapper dimensionMapper = new DimensionMapper();

    @Test
    public void mapProtoToPlain() {
        Delivery.Dimensions dimensions = Delivery.Dimensions.newBuilder()
                .setHeight(1.0)
                .setLength(2.0)
                .setWidth(3.0)
                .setWeight(4.0)
                .build();

        Dimension actual = dimensionMapper.map(dimensions);
        assertEquals(1.0, actual.getHeight(), 0.00001);
        assertEquals(2.0, actual.getDepth(), 0.00001);
        assertEquals(3.0, actual.getWidth(), 0.00001);
        assertEquals(4.0, actual.getWeight(), 0.00001);
    }

    @Test
    public void mapPlainToProto() {
        Dimension dimension = Dimension.newBuilder()
                .setHeight(1.0)
                .setDepth(2.0)
                .setWidth(3.0)
                .setWeight(4.0)
                .build();

        Delivery.Dimensions actual = dimensionMapper.map(dimension);
        assertEquals(1.0, actual.getHeight(), 0.00001);
        assertEquals(2.0, actual.getLength(), 0.00001);
        assertEquals(3.0, actual.getWidth(), 0.00001);
        assertEquals(4.0, actual.getWeight(), 0.00001);
    }
}
