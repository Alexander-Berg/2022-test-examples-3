package ru.yandex.market.mboc.common.masterdata.model;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;

/**
 * @author dmserebr
 * @date 15/10/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class BoxDimensionsConversionTest {
    private static MdmIrisPayload.ShippingUnit createShippingUnit(
        Long length, Long width, Long height, Long gross, Long net) {

        MdmIrisPayload.ShippingUnit.Builder builder = MdmIrisPayload.ShippingUnit.newBuilder();
        if (length != null) {
            builder.setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(length).build());
        }
        if (width != null) {
            builder.setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(width).build());
        }
        if (height != null) {
            builder.setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(height).build());
        }
        if (gross != null) {
            builder.setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(gross).build());
        }
        if (net != null) {
            builder.setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(net).build());
        }
        return builder.build();
    }

    @Test
    public void testFillEmptyMasterDataWithFullSizes() {
        MasterData md = new MasterData();

        BoxDimensionsInUm dimensions = new BoxDimensionsInUm(100L, 200L, 300L);
        md.setBoxDimensions(dimensions);

        Assertions.assertThat(md.getItemShippingUnit()).isEqualTo(createShippingUnit(100L, 200L, 300L, null, null));
    }

    @Test
    public void testFillEmptyMasterDataWithPartialSizes() {
        MasterData md = new MasterData();

        BoxDimensionsInUm dimensions = new BoxDimensionsInUm(100L, 200L, null);
        md.setBoxDimensions(dimensions);

        Assertions.assertThat(md.getItemShippingUnit()).isEqualTo(createShippingUnit(100L, 200L, null, null, null));
    }

    @Test
    public void testFillEmptyMasterDataWithWeight() {
        MasterData md = new MasterData();

        md.setWeightGross(500L);
        md.setWeightNet(1500L);

        Assertions.assertThat(md.getItemShippingUnit()).isEqualTo(createShippingUnit(null, null, null, 500L, 1500L));
    }

    @Test
    public void testFillNotEmptyMasterDataWithFullSizes() {
        MasterData md = new MasterData();
        md.setItemShippingUnit(createShippingUnit(50L, 60L, 70L, 80L, null));

        BoxDimensionsInUm dimensions = new BoxDimensionsInUm(100L, 200L, 300L);
        md.setBoxDimensions(dimensions);

        Assertions.assertThat(md.getItemShippingUnit()).isEqualTo(createShippingUnit(100L, 200L, 300L, 80L, null));
    }

    @Test
    public void testFillNotEmptyMasterDataWithWeight() {
        MasterData md = new MasterData();
        md.setItemShippingUnit(createShippingUnit(50L, 60L, 70L, 80L, null));

        md.setWeightGross(500L);
        md.setWeightNet(600L);

        Assertions.assertThat(md.getItemShippingUnit()).isEqualTo(createShippingUnit(50L, 60L, 70L, 500L, 600L));
    }

    @Test
    public void testFillPartiallyEmptyMasterDataWithWeightAndPartialSizes() {
        MasterData md = new MasterData();
        md.setItemShippingUnit(createShippingUnit(null, 60L, 70L, 80L, null));

        BoxDimensionsInUm dimensions = new BoxDimensionsInUm(100L, 200L, null);
        md.setBoxDimensions(dimensions);
        md.setWeightNet(600L);

        Assertions.assertThat(md.getItemShippingUnit()).isEqualTo(createShippingUnit(100L, 200L, null, 80L, 600L));
    }

    @Test
    public void testFillNotEmptyMasterDataWithNullDimensions() {
        MasterData md = new MasterData();
        md.setItemShippingUnit(createShippingUnit(null, 60L, 70L, 80L, null));

        md.setBoxDimensions(null);

        Assertions.assertThat(md.getItemShippingUnit()).isEqualTo(createShippingUnit(null, null, null, 80L, null));
    }

    @Test
    public void testFillNotEmptyMasterDataWithNullWeight() {
        MasterData md = new MasterData();
        md.setItemShippingUnit(createShippingUnit(50L, 60L, 70L, 80L, 90L));

        md.setWeightNet(null);

        Assertions.assertThat(md.getItemShippingUnit()).isEqualTo(createShippingUnit(50L, 60L, 70L, 80L, null));
    }

    @Test
    public void testClearWeight() {
        MasterData md = new MasterData();
        md.setItemShippingUnit(createShippingUnit(null, null, null, 80L, null));

        md.setWeightGross(null);

        Assertions.assertThat(md.getItemShippingUnit()).isNull();
    }

    @Test
    public void testClearSizes() {
        MasterData md = new MasterData();
        md.setItemShippingUnit(createShippingUnit(90L, 70L, 60L, null, null));

        md.setBoxDimensions(null);

        Assertions.assertThat(md.getItemShippingUnit()).isNull();
    }
}
