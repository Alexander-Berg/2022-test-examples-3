package ru.yandex.market.mbo.cardrender.app.model.saas;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;

/**
 * @author apluhin
 * @created 1/18/22
 */
public class SaasRenderModelHolderTest {

    @Test
    public void testSkuUrl() {
        SaasRenderModelHolder sku =
                new SaasRenderModelHolder(ModelStorage.Model.newBuilder().setId(1L).setCurrentType("SKU").build());
        Assertions.assertThat(sku.getUrl()).isEqualTo("1S");
        SaasRenderModelHolder model =
                new SaasRenderModelHolder(ModelStorage.Model.newBuilder().setId(2L).setCurrentType("GURU").build());
        Assertions.assertThat(model.getUrl()).isEqualTo("2M");
    }
}
