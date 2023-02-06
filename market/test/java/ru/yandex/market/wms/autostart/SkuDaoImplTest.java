package ru.yandex.market.wms.autostart;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.lgw.util.CollectionUtil;
import ru.yandex.market.wms.common.pojo.Dimensions;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuDaoImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/autostart/2/pick_locations.xml", connection = "wmwhseConnection", type =
                DatabaseOperation.REFRESH),
        @DatabaseSetup(value = "/fixtures/autostart/2/skus.xml", connection = "wmwhseConnection"),
})
class SkuDaoImplTest extends AutostartIntegrationTest {

    @Autowired
    protected SkuDaoImpl dao;

    @Test
    void findInventoryByOrderKeys() {
        SkuId rov1 = SkuId.of("100", "ROV0000000000000000001");
        SkuId rov2 = SkuId.of("100", "ROV0000000000000000002");

        Map<SkuId, Dimensions> dimensions = dao.getDimensions(
                Arrays.asList(
                        rov1,
                        rov2
                )
        );
        assertThat(
                dimensions,
                is(equalTo(CollectionUtil.mapOf(
                        rov1, dim2("1.00000", "1.00000"),
                        rov2, dim2("52.00000", "52.00000")
                )))
        );
    }

    private Dimensions dim2(String weight, String cube) {
        return new Dimensions.DimensionsBuilder()
                .length(new BigDecimal("0.0"))
                .height(new BigDecimal("0.0"))
                .width(new BigDecimal("0.0"))
                .weight(new BigDecimal(weight))
                .cube(new BigDecimal(cube))
                .build();
    }
}
