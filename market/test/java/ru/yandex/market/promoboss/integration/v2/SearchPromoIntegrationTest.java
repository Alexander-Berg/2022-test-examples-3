package ru.yandex.market.promoboss.integration.v2;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.integration.IntegrationPromoUtils;
import ru.yandex.mj.generated.client.self_client.model.Error;
import ru.yandex.mj.generated.client.self_client.model.MechanicsType;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchRequestDtoV2;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchRequestDtoV2Sort;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchRequestDtoV2SrcCiface;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchResult;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchResultItem;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchResultItemSrcCiface;
import ru.yandex.mj.generated.client.self_client.model.PromoStatus;
import ru.yandex.mj.generated.client.self_client.model.SourceType;

@DbUnitDataSet(before = "SearchPromoIntegrationTest.csv")
public class SearchPromoIntegrationTest extends AbstractSearchPromoIntegrationTest {
    public static final String url = "/api/v2/promos/search";

    private PromoSearchResult buildPromoSearchResult() {
        return new PromoSearchResult()
                .totalCount(1)
                .addPromosItem(
                        new PromoSearchResultItem()
                                .promoId(IntegrationPromoUtils.PROMO_ID)
                                .name("2022-06-06-15-16-1")
                                .startAt(1657487800L)
                                .endAt(1658438140L)
                                .updatedAt(1654617851L)
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .parentPromoId("SP#201")
                                .status(2)
                                .srcCiface(
                                        new PromoSearchResultItemSrcCiface()
                                                .promoKind("NATIONAL-1")
                                                .departments(List.of("FMCG-1", "CEHAC-1"))
                                                .tradeManager("tradeManager-1")
                                                .supplierType("1P-1")
                                                .compensationSource("PARTNER-1")
                                                .author("author-1")
                                                .promotionBudgetFact(140001L)
                                                .finalBudget(true)
                                                .assortmentLoadMethod("TRACKER-1")
                                )
                                .productsCount1p(2)
                                .productsCount3p(0)
                                .active(false)
                );
    }

    private PromoSearchRequestDtoV2 buildPromoSearchRequestDtoV2() {
        return new PromoSearchRequestDtoV2()
                .pageSize(10)
                .pageNumber(1)
                .sort(List.of(new PromoSearchRequestDtoV2Sort()
                        .field(PromoSearchRequestDtoV2Sort.FieldEnum.PROMOID)
                        .direction(PromoSearchRequestDtoV2Sort.DirectionEnum.ASC)
                ))
                .startAtFrom(Long.MIN_VALUE)
                .startAtTo(Long.MAX_VALUE)
                .endAtFrom(Long.MIN_VALUE)
                .endAtTo(Long.MAX_VALUE)
                .updatedAtFrom(Long.MIN_VALUE)
                .updatedAtTo(Long.MAX_VALUE)
                .promoId(List.of(IntegrationPromoUtils.PROMO_ID, "cf_999999"))
                .name("2022-06-06")
                .mechanicsType(List.of(
                        MechanicsType.CHEAPEST_AS_GIFT,
                        MechanicsType.BLUE_FLASH
                ))
                .parentPromoId(List.of("SP#201", "SP#202"))
                .status(List.of(
                        PromoStatus.NEW,
                        PromoStatus.READY
                ))
                .sourceType(List.of(
                        SourceType.CATEGORYIFACE,
                        SourceType.ANAPLAN
                ))
                .srcCiface(new PromoSearchRequestDtoV2SrcCiface()
                        .promoKind(List.of("NATIONAL-1", "NATIONAL-2"))
                        .department(List.of("FMCG-1", "FMCG-2"))
                        .tradeManager(List.of("tradeManager-1", "tradeManager-2"))
                        .supplierType(List.of("1P-1", "1P-2"))
                        .compensationSource(List.of("PARTNER-1", "PARTNER-2"))
                        .author(List.of("author-1", "author-2"))
                        .finalBudget(true)
                        .assortmentLoadMethod(List.of("TRACKER-1", "TRACKER-2"))
                );
    }

    @Test
    void searchPromoOkNotEmpty() throws Exception {
        PromoSearchRequestDtoV2 promoRequest = buildPromoSearchRequestDtoV2();
        PromoSearchResult promoSearchResult = buildPromoSearchResult();

        mockMvc.perform(
                        MockMvcRequestBuilders.post(url)
                                .contentType("application/json")
                                .content(getTestObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(promoRequest))
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(
                        MockMvcResultMatchers.content()
                                .json(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(promoSearchResult))
                );
    }

    @Test
    void searchPromoOkEmpty() throws Exception {
        PromoSearchRequestDtoV2 promoRequest = buildPromoSearchRequestDtoV2()
                .parentPromoId(List.of("anotherParentPromoId"));
        PromoSearchResult promoSearchResult = new PromoSearchResult()
                .totalCount(0)
                .promos(List.of());

        mockMvc.perform(
                        MockMvcRequestBuilders.post(url)
                                .contentType("application/json")
                                .content(getTestObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(promoRequest))
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(
                        MockMvcResultMatchers.content()
                                .json(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(promoSearchResult))
                );
    }

    @Test
    void searchPromoBadRequest() throws Exception {
        PromoSearchRequestDtoV2 promoRequest = buildPromoSearchRequestDtoV2()
                .pageSize(null);

        Error error = new Error()
                .message("PageSize and PageNumber must be set both or none of them.");

        mockMvc.perform(
                        MockMvcRequestBuilders.post(url)
                                .contentType("application/json")
                                .content(getTestObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(promoRequest))
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(
                        MockMvcResultMatchers.content()
                                .json(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(error))
                );
    }
}
