package ru.yandex.market.api.partner.controllers.stats.goodsreport;

import java.util.Collections;
import java.util.List;

import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.DataCampValidationResult;
import com.google.common.collect.Multimap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.controllers.stats.goodsreport.matchers.HiddenOfferDetailsMatchers;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.hiddenoffers.model.HiddenOfferDetails;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Тесты для {@link GoodsHidingsInfoService}.
 */
@DbUnitDataSet(before = "GoodsHidingsInfoServiceTest.before.csv")
class GoodsHidingsInfoServiceTest extends FunctionalTest {
    private static final PartnerId PARTNER = PartnerId.supplierId(1);

    @Autowired
    private GoodsHidingsInfoService goodsHidingsInfoService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Test
    void testAbo() {
        given(dataCampShopClient.searchBusinessOffers(any())).willReturn(
                SearchBusinessOffersResult.builder()
                        .setOffers(List.of(
                                DataCampUnitedOffer.UnitedOffer.newBuilder()
                                        .setBasic(DataCampOffer.Offer.newBuilder()
                                                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                        .setShopId(1)
                                                        .setBusinessId(100)
                                                        .setOfferId("sku1")
                                                        .build())
                                                .build())
                                        .putService(1, DataCampOffer.Offer.newBuilder()
                                                .setResolution(DataCampResolution.Resolution.newBuilder()
                                                        .addBySource(DataCampResolution.Verdicts.newBuilder()
                                                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                                        .setSource(DataCampOfferMeta.DataSource.MARKET_ABO)
                                                                        .build())
                                                                .addVerdict(DataCampResolution.Verdict.newBuilder()
                                                                        .addResults(DataCampValidationResult.ValidationResult.newBuilder()
                                                                                .addMessages(DataCampExplanation.Explanation.newBuilder()
                                                                                        .setCode("OTHER")
                                                                                        .setNamespace("shared" +
                                                                                                ".hidden-offers" +
                                                                                                ".reasons.codes")
                                                                                        .setDetails("OTHER")
                                                                                        .build())
                                                                                .build())
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build()
                        )).build());
        Multimap<String, HiddenOfferDetails> abo = goodsHidingsInfoService.getHidingDetails(
                PARTNER,
                Collections.singleton("sku1")
        );
        Assertions.assertEquals(1, abo.size());
        MatcherAssert.assertThat(
                abo.get("sku1"),
                Matchers.contains(
                        MbiMatchers.<HiddenOfferDetails>newAllOfBuilder()
                                .add(Matchers.allOf(
                                        HiddenOfferDetailsMatchers.hasDatasourceId(1),
                                        HiddenOfferDetailsMatchers.hasShopSku("sku1"),
                                        HiddenOfferDetailsMatchers.hasReason("Скрыты сотрудниками Беру"),
                                        HiddenOfferDetailsMatchers.hasReasonCode("MANUAL_HIDDEN_BY_BERU"),
                                        HiddenOfferDetailsMatchers.hasSubreason("Товар скрыт сотрудником Беру"),
                                        HiddenOfferDetailsMatchers.hasSubreasonCode("MANUAL_HIDDEN_BY_BERU")
                                )).build()
                )
        );
    }
}
