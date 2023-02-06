package ru.yandex.market.global.index.domain.api;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.index.BaseLocalTest;
import ru.yandex.mj.generated.client.pim.api.PimApiClient;
import ru.yandex.mj.generated.client.pim.model.AttributesResponse;
import ru.yandex.mj.generated.client.pim.model.CategoriesResponse;
import ru.yandex.mj.generated.client.pim.model.GetRequest;
import ru.yandex.mj.generated.client.pim.model.InfoModelsResponse;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PimLocalTest extends BaseLocalTest {
    private final PimApiClient pimApiClient;

    @Test
    public void testCategories() {
        CategoriesResponse categoriesResponse = pimApiClient.v1ActualCategoriesPost(new GetRequest()).schedule().join();

        System.out.println(categoriesResponse.getItems());
    }

    @Test
    public void testAttributes() {
        AttributesResponse attributesResponse = pimApiClient.v1AttributesGet(
                0, 10
        ).schedule().join();

        System.out.println(attributesResponse.getList());
    }

    @Test
    public void testInfomodels() {
        InfoModelsResponse attributesResponse = pimApiClient.v1InfoModelsGet(
                0, 500
        ).schedule().join();

        System.out.println(attributesResponse.getList());
    }

}
