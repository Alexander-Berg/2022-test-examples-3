package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.product.model.ProductType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.bsexport.model.ProductType.AudioCreativeReach;
import static ru.yandex.direct.bsexport.model.ProductType.VideoCreativeReach;
import static ru.yandex.direct.bsexport.model.ProductType.VideoCreativeReachIndoor;
import static ru.yandex.direct.bsexport.model.ProductType.VideoCreativeReachOutdoor;
import static ru.yandex.direct.bsexport.query.order.BillingAggregatesFactory.productTypeMapper;

class BillingAggregateFactoryProductTypeTests {

    @Test
    void cpmAudioMappedValue() {
        assertThat(productTypeMapper(ProductType.CPM_AUDIO))
                .containsExactly(AudioCreativeReach);
    }

    @Test
    void cpmVideoMappedValue() {
        assertThat(productTypeMapper(ProductType.CPM_VIDEO))
                .containsExactly(VideoCreativeReach);
    }

    @Test
    void cpmIndoorMappedValue() {
        assertThat(productTypeMapper(ProductType.CPM_INDOOR))
                .containsExactly(VideoCreativeReachIndoor);
    }

    @Test
    void cpmOutdoorMappedValue() {
        assertThat(productTypeMapper(ProductType.CPM_OUTDOOR))
                .containsExactly(VideoCreativeReachOutdoor);
    }
}
