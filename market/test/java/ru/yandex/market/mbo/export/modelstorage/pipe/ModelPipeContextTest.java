package ru.yandex.market.mbo.export.modelstorage.pipe;

import java.util.Arrays;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;

@SuppressWarnings("checkstyle:MagicNumber")
public class ModelPipeContextTest {

    private static final ModelStorage.Model MODEL = ModelStorage.Model.newBuilder().setId(1).build();

    @Test
    public void testFailWithDuplicatesInModifications() {
        ModelStorage.Model modif1 = ModelStorage.Model.newBuilder().setId(2).build();
        ModelStorage.Model modif2 = ModelStorage.Model.newBuilder().setId(2).build();

        Assertions.assertThatThrownBy(() -> {
            new ModelPipeContext(MODEL, Arrays.asList(modif1, modif2), Collections.emptyList());
        }).hasMessageContaining("Found duplicates at parent model id: 1 in modifications: 2");
    }

    @Test
    public void testFailWithDuplicatesInSkus() {
        ModelStorage.Model ssku1 = ModelStorage.Model.newBuilder().setId(2).build();
        ModelStorage.Model ssku2 = ModelStorage.Model.newBuilder().setId(2).build();
        ModelStorage.Model ssku3 = ModelStorage.Model.newBuilder().setId(3).build();
        ModelStorage.Model ssku4 = ModelStorage.Model.newBuilder().setId(3).build();
        ModelStorage.Model ssku5 = ModelStorage.Model.newBuilder().setId(4).build();

        Assertions.assertThatThrownBy(() -> {
            new ModelPipeContext(MODEL, Collections.emptyList(), Arrays.asList(ssku1, ssku2, ssku3, ssku4, ssku5));
        }).hasMessageContaining("Found duplicates at parent model id: 1 in skus: 2");
    }

    @Test
    public void testAcceptNewSkusOrModifications() {
        ModelStorage.Model ssku1 = ModelStorage.Model.newBuilder().setId(0).build();
        ModelStorage.Model ssku2 = ModelStorage.Model.newBuilder().setId(0).build();

        Assertions.assertThatCode(() ->
            new ModelPipeContext(MODEL, Collections.emptyList(), Arrays.asList(ssku1, ssku2))
        ).doesNotThrowAnyException();

        ModelStorage.Model modif1 = ModelStorage.Model.newBuilder().setId(0).build();
        ModelStorage.Model modif2 = ModelStorage.Model.newBuilder().setId(0).build();

        Assertions.assertThatCode(() ->
            new ModelPipeContext(MODEL, Arrays.asList(modif1, modif2), Collections.emptyList())
        ).doesNotThrowAnyException();
    }

}
