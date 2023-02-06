package ru.yandex.market.checkout.pushapi.service.shop;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class CheckouterShopMetaDataGetterTest {

    private final CheckouterShopMetaDataGetter getter =
            spy(new CheckouterShopMetaDataGetter(10000, 2));

    @Test
    void testGetMeta() {
        var result11 = ShopMetaDataBuilder.createTestDefault().withClientId(11).build();
        var result12 = ShopMetaDataBuilder.createTestDefault().withClientId(12).build();
        var result13 = ShopMetaDataBuilder.createTestDefault().withClientId(13).build();

        doReturn(result11).when(getter).doLoadNullable(1L);
        doReturn(result12).when(getter).doLoadNullable(2L);
        doReturn(result13).when(getter).doLoadNullable(3L);

        getter.init();

        var meta1 = getter.getMeta(1L);
        assertThat(meta1).isNotNull();
        assertThat(meta1.getClientId()).isEqualTo(11L);

        getter.getMeta(1L);
        getter.getMeta(2L);
        getter.getMeta(3L);
        getter.getMeta(1L);

        verify(getter, times(2)).doLoadNullable(1L);
        verify(getter, times(1)).doLoadNullable(2L);
        verify(getter, times(1)).doLoadNullable(3L);
    }

    @Test
    void testNullInCache() {
        doReturn(null).when(getter).doLoadNullable(1L);
        var testMetaData = ShopMetaDataBuilder.createTestDefault().build();
        doReturn(testMetaData).when(getter).doLoadNullable(2L);

        getter.init();

        assertThat(getter.getMeta(1L)).isNull();
        assertThat(getter.getMeta(2L)).isEqualTo(testMetaData);
        assertThat(getter.getMeta(1L)).isNull();

        verify(getter, times(1)).doLoadNullable(1L);
        verify(getter, times(1)).doLoadNullable(2L);
    }
}