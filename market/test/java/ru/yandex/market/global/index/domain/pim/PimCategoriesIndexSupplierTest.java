package ru.yandex.market.global.index.domain.pim;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import util.RandomDataGenerator;

import ru.yandex.market.global.common.elastic.IndexedEntity;
import ru.yandex.market.global.index.BaseFunctionalTest;
import ru.yandex.market.global.index.domain.dixtionary.CategoryEnrichmentsDictionary;
import ru.yandex.mj.generated.client.pim.api.PimApiClient;
import ru.yandex.mj.generated.client.pim.model.AttributesResponse;
import ru.yandex.mj.generated.client.pim.model.AttributesResponseList;
import ru.yandex.mj.generated.client.pim.model.AttributesResponseOptions;
import ru.yandex.mj.generated.client.pim.model.CategoriesResponse;
import ru.yandex.mj.generated.client.pim.model.Category;
import ru.yandex.mj.generated.client.pim.model.GetRequest;
import ru.yandex.mj.generated.client.pim.model.InfoModelsResponse;
import ru.yandex.mj.generated.client.pim.model.InfoModelsResponseAttributes;
import ru.yandex.mj.generated.client.pim.model.InfoModelsResponseList;
import ru.yandex.mj.generated.client.pim.model.MasterCategoryInfomodel;
import ru.yandex.mj.generated.server.model.CategoryEnrichmentCacheDto;
import ru.yandex.mj.generated.server.model.MarketCategoryDto;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PimCategoriesIndexSupplierTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM =
            RandomDataGenerator.dataRandom(PimCategoriesIndexSupplierTest.class).build();
    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true)
                    .withIgnoreCollectionOrder(true)
                    .withIgnoreAllOverriddenEquals(true)
                    .build();

    private final PimApiClient pimApiClient;
    private final PimCategoriesIndexSupplier indexSupplier;
    private final CategoryEnrichmentsDictionary pimCategoryEnrichmentCacheDictionary;

    @BeforeEach
    void setup() {
        mockAttributes();
        mockInfomodels();
        mockCategories();
        mockEnrichments();
    }

    @Test
    void testGetCategories() {
        //noinspection ConstantConditions
        List<IndexedEntity<String, MarketCategoryDto>> categories = indexSupplier.streamAll(null)
                .collect(Collectors.toList());

        Assertions.assertThat(categories).hasSize(5);

        Assertions.assertThat(categories.stream().filter(e -> "1".equals(e.getId())).findAny().orElseThrow())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new IndexedEntity<String, MarketCategoryDto>().setId("1")
                        .setDto(new MarketCategoryDto()
                                .id(1L)
                                .code("root")
                                .visibleForCustomer(false) //No front category
                                .enabledForMerchant(false) //Disabled as not leaf
                                .sortOrder(1) //Master
                                .attributes(null)
                                .title(null)
                        ));


        Assertions.assertThat(categories.stream().filter(e -> "2".equals(e.getId())).findAny().orElseThrow())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new IndexedEntity<String, MarketCategoryDto>().setId("2")
                        .setDto(new MarketCategoryDto()
                                .id(2L)
                                .code("dept1")
                                .parentId(1L)
                                .parentCode("root")
                                .visibleForCustomer(true)
                                .enabledForMerchant(false) //Disabled as not leaf
                                .sortOrder(2) //Master
                                .sortOrderForCustomer(20) //Enrichment
                                .attributes(null)
                                .title(null)
                        )
                );

        Assertions.assertThat(categories.stream().filter(e -> "3".equals(e.getId())).findAny().orElseThrow())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new IndexedEntity<String, MarketCategoryDto>().setId("3")
                        .setDto(new MarketCategoryDto()
                                .id(3L)
                                .code("cat1_1")
                                .parentId(2L)
                                .parentCode("dept1")
                                .visibleForCustomer(false) //Disabled in PIM
                                .enabledForMerchant(true)
                                .sortOrder(3) //Master
                                .sortOrderForCustomer(20) //Enrichment
                                .attributes(null)
                                .title(null)
                        )
                );

        Assertions.assertThat(categories.stream().filter(e -> "4".equals(e.getId())).findAny().orElseThrow())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new IndexedEntity<String, MarketCategoryDto>().setId("4")
                        .setDto(new MarketCategoryDto()
                                .id(4L)
                                .code("cat1_2")
                                .parentId(2L)
                                .parentCode("dept1")
                                .visibleForCustomer(false) //Disabled in enrichment
                                .enabledForMerchant(false) //Disabled in PIM
                                .sortOrder(4) //Master
                                .sortOrderForCustomer(100000) //Override from PIM
                                .picture("https://market-static.s3.yandex.net/global-market-b2c/assets/market-categories/pets.png") //Override from PIM
                                .attributes(null)
                                .title(null)
                        )
                );

        Assertions.assertThat(categories.stream().filter(e -> "5".equals(e.getId())).findAny().orElseThrow())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new IndexedEntity<String, MarketCategoryDto>().setId("5")
                        .setDto(new MarketCategoryDto()
                                .id(5L)
                                .code("cat1_3")
                                .parentId(2L)
                                .parentCode("dept1")
                                .visibleForCustomer(false) //No front category in PIM
                                .enabledForMerchant(true)
                                .sortOrder(5) //Master
                                .sortOrderForCustomer(20) //Enrichment
                                .attributes(null)
                                .title(null)
                        )
                );

    }

    private void mockEnrichments() {
        Mockito.when(pimCategoryEnrichmentCacheDictionary.getAll())
                .thenReturn(List.of(
                        RANDOM.nextObject(CategoryEnrichmentCacheDto.class)
                                .categoryCode("dept1")
                                .sortOrderForCustomer(20)
                                .visibleForCustomer(true),
                        RANDOM.nextObject(CategoryEnrichmentCacheDto.class)
                                .categoryCode("cat1_1")
                                .sortOrderForCustomer(20)
                                .visibleForCustomer(true),
                        RANDOM.nextObject(CategoryEnrichmentCacheDto.class)
                                .categoryCode("cat1_2")
                                .sortOrderForCustomer(20)
                                .visibleForCustomer(false),
                        RANDOM.nextObject(CategoryEnrichmentCacheDto.class)
                                .categoryCode("cat1_3")
                                .sortOrderForCustomer(20)
                                .visibleForCustomer(true)
                ));
    }

    private void mockCategories() {
        Mockito.when(pimApiClient.v1ActualCategoriesPost(Mockito.any(GetRequest.class)).schedule().join())
                .thenReturn(RANDOM.nextObject(CategoriesResponse.class)
                        .items(List.of(
                                RANDOM.nextObject(Category.class)
                                        .id(1)
                                        .parentId(null)
                                        .type(Category.TypeEnum.MASTER)
                                        .status(Category.StatusEnum.ACTIVE)
                                        .code("master:IL:root")
                                        .parentCode(null)
                                        .order(1)
                                        .infomodel(RANDOM.nextObject(MasterCategoryInfomodel.class)
                                                .code("root")
                                        ),
                                RANDOM.nextObject(Category.class)
                                        .id(2)
                                        .parentId(1)
                                        .order(2)
                                        .type(Category.TypeEnum.MASTER)
                                        .status(Category.StatusEnum.ACTIVE)
                                        .code("master:IL:dept1")
                                        .parentCode("master:IL:root")
                                        .infomodel(RANDOM.nextObject(MasterCategoryInfomodel.class)
                                                .code("i1")
                                        ),
                                RANDOM.nextObject(Category.class)
                                        .id(3)
                                        .parentId(2)
                                        .order(3)
                                        .type(Category.TypeEnum.MASTER)
                                        .status(Category.StatusEnum.ACTIVE)
                                        .code("master:IL:cat1_1")
                                        .parentCode("master:IL:dept1")
                                        .infomodel(RANDOM.nextObject(MasterCategoryInfomodel.class)
                                                .code("i1")
                                        ),
                                RANDOM.nextObject(Category.class)
                                        .id(4)
                                        .parentId(2)
                                        .order(4)
                                        .type(Category.TypeEnum.MASTER)
                                        .status(Category.StatusEnum.DISABLED)
                                        .code("master:IL:cat1_2")
                                        .parentCode("master:IL:dept1")
                                        .description(Map.of("en", "{\"enabledForMerchant\": false}"))
                                        .infomodel(RANDOM.nextObject(MasterCategoryInfomodel.class)
                                                .code("i2")
                                        ),
                                RANDOM.nextObject(Category.class)
                                        .id(5)
                                        .parentId(2)
                                        .order(5)
                                        .type(Category.TypeEnum.MASTER)
                                        .status(Category.StatusEnum.ACTIVE)
                                        .code("master:IL:cat1_3")
                                        .parentCode("master:IL:dept1")
                                        .infomodel(RANDOM.nextObject(MasterCategoryInfomodel.class)
                                                .code("i2")
                                        ),


                                RANDOM.nextObject(Category.class)
                                        .id(3)
                                        .type(Category.TypeEnum.FRONT)
                                        .order(1)
                                        .code("front:IL:dept1")
                                        .status(Category.StatusEnum.ACTIVE),
                                RANDOM.nextObject(Category.class)
                                        .id(2)
                                        .order(2)
                                        .type(Category.TypeEnum.FRONT)
                                        .code("front:IL:cat1_1")
                                        .status(Category.StatusEnum.DISABLED),
                                RANDOM.nextObject(Category.class)
                                        .id(3)
                                        .order(3)
                                        .type(Category.TypeEnum.FRONT)
                                        .code("front:IL:cat1_2")
                                        .putDescriptionItem("en", "{\"sortOrderForCustomer\": 100000, \"picture\": \"https://market-static.s3.yandex.net/global-market-b2c/assets/market-categories/pets.png\"}")
                                        .status(Category.StatusEnum.ACTIVE)
                        ))
                );
    }

    private void mockInfomodels() {
        Mockito.when(pimApiClient.v1InfoModelsGet(Mockito.anyInt(), Mockito.anyInt()).schedule().join()).thenReturn(
                RANDOM.nextObject(InfoModelsResponse.class)
                        .list(List.of(
                                RANDOM.nextObject(InfoModelsResponseList.class)
                                        .code("root")
                                        .region("IL")
                                        .attributes(List.of(
                                                RANDOM.nextObject(InfoModelsResponseAttributes.class)
                                                        .code("r1")
                                        )),
                                RANDOM.nextObject(InfoModelsResponseList.class)
                                        .code("i1")
                                        .region("IL")
                                        .attributes(List.of(
                                                RANDOM.nextObject(InfoModelsResponseAttributes.class)
                                                        .code("r1"),
                                                RANDOM.nextObject(InfoModelsResponseAttributes.class)
                                                        .code("a1")
                                        )),
                                RANDOM.nextObject(InfoModelsResponseList.class)
                                        .code("i2")
                                        .region("IL")
                                        .attributes(List.of(
                                                RANDOM.nextObject(InfoModelsResponseAttributes.class)
                                                        .code("r1"),
                                                RANDOM.nextObject(InfoModelsResponseAttributes.class)
                                                        .code("a2")
                                        )),
                                RANDOM.nextObject(InfoModelsResponseList.class)
                                        .code("i2")
                                        .region("SA")
                                        .attributes(List.of(
                                                RANDOM.nextObject(InfoModelsResponseAttributes.class)
                                                        .code("r1"),
                                                RANDOM.nextObject(InfoModelsResponseAttributes.class)
                                                        .code("a1")
                                        ))
                        ))
        ).thenReturn(
                RANDOM.nextObject(InfoModelsResponse.class).list(List.of())
        );
    }

    private void mockAttributes() {
        Mockito.when(pimApiClient.v1AttributesGet(Mockito.anyInt(), Mockito.anyInt()).schedule().join()).thenReturn(
                RANDOM.nextObject(AttributesResponse.class).list(List.of(
                        RANDOM.nextObject(AttributesResponseList.class)
                                .code("r1")
                                .type(AttributesResponseList.TypeEnum.TEXT)
                                .name(Map.of("en", "R1"))
                                .isValueRequired(true),
                        RANDOM.nextObject(AttributesResponseList.class)
                                .code("a1")
                                .type(AttributesResponseList.TypeEnum.NUMBER)
                                .name(Map.of("en", "A1"))
                                .isValueRequired(true),
                        RANDOM.nextObject(AttributesResponseList.class)
                                .code("a2")
                                .type(AttributesResponseList.TypeEnum.MULTISELECT)
                                .name(Map.of("en", "A2"))
                                .isValueRequired(false)
                                .options(List.of(
                                        RANDOM.nextObject(AttributesResponseOptions.class)
                                                .code("o1")
                                                .name(Map.of("en", "O1")),
                                        RANDOM.nextObject(AttributesResponseOptions.class)
                                                .code("o1")
                                                .name(Map.of("en", "O1"))
                                ))
                )))
                .thenReturn(
                        RANDOM.nextObject(AttributesResponse.class).list(List.of())
                );
    }
}
