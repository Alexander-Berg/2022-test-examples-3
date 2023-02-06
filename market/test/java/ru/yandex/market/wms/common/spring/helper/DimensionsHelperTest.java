package ru.yandex.market.wms.common.spring.helper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.pojo.Dimensions;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDetailDTO;
import ru.yandex.market.wms.common.spring.service.nonsort.DimensionsHelper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DimensionsHelperTest {

    @Test
    public void fitTest() {
        Dimensions dim1 = new Dimensions.DimensionsBuilder()
                .height(BigDecimal.valueOf(12))
                .weight(BigDecimal.valueOf(12))
                .length(BigDecimal.valueOf(12))
                .width(BigDecimal.valueOf(10))
                .build();
        Dimensions dim2 = new Dimensions.DimensionsBuilder()
                .height(BigDecimal.valueOf(120))
                .weight(BigDecimal.valueOf(120))
                .length(BigDecimal.valueOf(120))
                .width(BigDecimal.valueOf(10))
                .build();

        Dimensions max = new Dimensions.DimensionsBuilder()
                .height(BigDecimal.valueOf(15))
                .weight(BigDecimal.valueOf(15))
                .length(BigDecimal.valueOf(15))
                .width(BigDecimal.valueOf(20))
                .build();
        SkuId skuId1 = SkuId.of("STORER", "SKU1");
        SkuId skuId2 = SkuId.of("STORER", "SKU2");
        Map<SkuId, Dimensions> map = Map.of(skuId1, dim1, skuId2, dim2);
        DimensionsHelper helper = new DimensionsHelper(map, max);

        assertThat(helper.skuFitDimensionsAndWeight(skuId1)).isTrue();
        assertThat(helper.skuFitDimensionsAndWeight(skuId2)).isFalse();
        assertThat(helper.skuFitDimensionsAndWeight(SkuId.of("A", "B"))).isFalse();
    }

    @Test
    public void nullTest1() {

        DimensionsHelper helper = new DimensionsHelper(null, null);
        assertThat(helper.skuFitDimensionsAndWeight(SkuId.of("A", "B"))).isFalse();
    }

    @Test
    public void nullTest2() {
        Dimensions dim = new Dimensions.DimensionsBuilder()
                .build();
        Dimensions max = new Dimensions.DimensionsBuilder()
                .build();
        SkuId skuId = SkuId.of("STORER", "SKU1");
        Map<SkuId, Dimensions> map = Map.of(skuId, dim);
        DimensionsHelper helper = new DimensionsHelper(map, max);

        assertThat(helper.skuFitDimensionsAndWeight(skuId)).isFalse();
        assertThat(helper.skuFitDimensionsAndWeight(SkuId.of("A", "B"))).isFalse();
    }

    @Test
    public void testOrderFitVolume() {
        Dimensions dim1 = new Dimensions.DimensionsBuilder()
                .cube(BigDecimal.valueOf(10))
                .build();
        Dimensions dim2 = new Dimensions.DimensionsBuilder()
                .cube(BigDecimal.valueOf(10.5))
                .build();

        Dimensions maxLesser = new Dimensions.DimensionsBuilder()
                .cube(BigDecimal.valueOf(30.9))
                .build();
        Dimensions maxEqual = new Dimensions.DimensionsBuilder()
                .cube(BigDecimal.valueOf(31))
                .build();
        Dimensions maxHigher = new Dimensions.DimensionsBuilder()
                .cube(BigDecimal.valueOf(31.1))
                .build();
        SkuId skuId1 = SkuId.of("STORER", "SKU1");
        SkuId skuId2 = SkuId.of("STORER", "SKU2");
        Map<SkuId, Dimensions> map = Map.of(skuId1, dim1, skuId2, dim2);

        DimensionsHelper helperLesser = new DimensionsHelper(map, maxLesser);
        DimensionsHelper helperEqual = new DimensionsHelper(map, maxEqual);
        DimensionsHelper helperHigher = new DimensionsHelper(map, maxHigher);
        OrderDTO orderDTO = OrderDTO.builder()
                .orderdetails(List.of(
                        OrderDetailDTO.builder().storerkey("STORER").sku("SKU1").openqty(BigDecimal.valueOf(1)).build(),
                        OrderDetailDTO.builder().storerkey("STORER").sku("SKU2").openqty(BigDecimal.valueOf(2)).build()
                        )
                )
                .build();
        assertThat(helperLesser.orderFitVolume(orderDTO)).isFalse();
        assertThat(helperEqual.orderFitVolume(orderDTO)).isTrue();
        assertThat(helperHigher.orderFitVolume(orderDTO)).isTrue();
    }
}
