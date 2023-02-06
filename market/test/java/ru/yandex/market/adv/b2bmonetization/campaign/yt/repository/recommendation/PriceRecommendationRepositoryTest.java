package ru.yandex.market.adv.b2bmonetization.campaign.yt.repository.recommendation;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.database.entity.offer.file.PartnerType;
import ru.yandex.market.adv.b2bmonetization.campaign.model.campaign.CampaignVersion;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.OfferRecommendation;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.PriceRecommendation;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.ShopOffer;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;

@ParametersAreNonnullByDefault
public class PriceRecommendationRepositoryTest extends AbstractMonetizationTest {

    @Autowired
    private PriceRecommendationsRepository priceRecommendationsRepository;

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_blueAdv_list_" +
                            "blue_shop_offer"
            ),
            before = "PriceRecommendationRepository/json/blueShopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_blueAdv_list_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Синяя ADV кампания со списком товаров с рекомендацией")
    @Test
    public void findByPartnerIdAndCampaignId_blueAdv_list() {
        test(
                "findByPartnerIdAndCampaignId_blueAdv_list_",
                11L,
                1011L,
                PartnerType.FB,
                CampaignVersion.ADV,
                List.of(
                        OfferRecommendation.builder()
                                .partnerId(11L)
                                .offerId("1100")
                                .name("Name_1100")
                                .bid(540)
                                .price(25343)
                                .origPrice(253.43)
                                .recommendedPrice(140.50)
                                .recommendedPromocode(12.0)
                                .build(),
                        OfferRecommendation.builder()
                                .partnerId(11L)
                                .offerId("1101")
                                .name("Name_1101")
                                .bid(1140)
                                .price(125520)
                                .origPrice(1255.2)
                                .recommendedPrice(1150)
                                .recommendedPromocode(50.59)
                                .build()
                )
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_blueAdv_empty_" +
                            "blue_shop_offer"
            ),
            before = "PriceRecommendationRepository/json/blueShopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_blueAdv_empty_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Синяя ADV кампания без товаров с рекомендацией")
    @Test
    public void findByPartnerIdAndCampaignId_blueAdv_empty() {
        test(
                "findByPartnerIdAndCampaignId_blueAdv_empty_",
                11L,
                1010L,
                PartnerType.FB,
                CampaignVersion.ADV,
                List.of()
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_bluePl_empty_" +
                            "blue_shop_offer"
            ),
            before = "PriceRecommendationRepository/json/blueShopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_bluePl_empty_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Синяя PL кампания без товаров с рекомендацией")
    @Test
    public void findByPartnerIdAndCampaignId_bluePl_empty() {
        test(
                "findByPartnerIdAndCampaignId_bluePl_empty_",
                11L,
                1010L,
                PartnerType.FB,
                CampaignVersion.PL,
                List.of()
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_bluePl_list_" +
                            "blue_shop_offer"
            ),
            before = "PriceRecommendationRepository/json/blueShopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_bluePl_list_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Синяя PL кампания со списком товаров с рекомендацией")
    @Test
    public void findByPartnerIdAndCampaignId_bluePl_list() {
        test(
                "findByPartnerIdAndCampaignId_bluePl_list_",
                11L,
                1011L,
                PartnerType.FB,
                CampaignVersion.PL,
                List.of(
                        OfferRecommendation.builder()
                                .partnerId(11L)
                                .offerId("-100")
                                .name("Name_-100")
                                .bid(984)
                                .price(9841400)
                                .origPrice(98414)
                                .recommendedPrice(98402)
                                .recommendedPromocode(0)
                                .build(),
                        OfferRecommendation.builder()
                                .partnerId(11L)
                                .offerId("-101")
                                .name("Name_-101")
                                .bid(530)
                                .price(874444)
                                .origPrice(874.42)
                                .recommendedPrice(484.51)
                                .recommendedPromocode(41.853)
                                .build()
                )
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_whiteAdv_list_" +
                            "shop_offer"
            ),
            before = "PriceRecommendationRepository/json/shopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_whiteAdv_list_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Белая ADV кампания со списком товаров с рекомендацией")
    @Test
    public void findByPartnerIdAndCampaignId_whiteAdv_list() {
        test(
                "findByPartnerIdAndCampaignId_whiteAdv_list_",
                12L,
                1011L,
                PartnerType.DBS,
                CampaignVersion.ADV,
                List.of(
                        OfferRecommendation.builder()
                                .partnerId(12L)
                                .offerId("1100")
                                .name("Name_1100")
                                .bid(540)
                                .price(25343)
                                .origPrice(253.43)
                                .recommendedPrice(140.50)
                                .recommendedPromocode(12.0)
                                .build(),
                        OfferRecommendation.builder()
                                .partnerId(12L)
                                .offerId("1101")
                                .name("Name_1101")
                                .bid(1140)
                                .price(125520)
                                .origPrice(1255.2)
                                .recommendedPrice(1150)
                                .recommendedPromocode(50.59)
                                .build()
                )
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_whiteAdv_empty_" +
                            "shop_offer"
            ),
            before = "PriceRecommendationRepository/json/shopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_whiteAdv_empty_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Белая ADV кампания без товаров с рекомендацией")
    @Test
    public void findByPartnerIdAndCampaignId_whiteAdv_empty() {
        test(
                "findByPartnerIdAndCampaignId_whiteAdv_empty_",
                12L,
                1010L,
                PartnerType.DBS,
                CampaignVersion.ADV,
                List.of()
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_whitePl_empty_" +
                            "shop_offer"
            ),
            before = "PriceRecommendationRepository/json/shopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_whitePl_empty_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Белая PL кампания без товаров с рекомендацией")
    @Test
    public void findByPartnerIdAndCampaignId_whitePl_empty() {
        test(
                "findByPartnerIdAndCampaignId_whitePl_empty_",
                12L,
                1010L,
                PartnerType.DBS,
                CampaignVersion.PL,
                List.of()
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_whitePl_list_" +
                            "shop_offer"
            ),
            before = "PriceRecommendationRepository/json/shopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findByPartnerIdAndCampaignId_whitePl_list_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Белая PL кампания со списком товаров с рекомендацией")
    @Test
    public void findByPartnerIdAndCampaignId_whitePl_list() {
        test(
                "findByPartnerIdAndCampaignId_whitePl_list_",
                12L,
                1011L,
                PartnerType.DBS,
                CampaignVersion.PL,
                List.of(
                        OfferRecommendation.builder()
                                .partnerId(12L)
                                .offerId("-100")
                                .name("Name_-100")
                                .bid(984)
                                .price(9841400)
                                .origPrice(98414)
                                .recommendedPrice(98402)
                                .recommendedPromocode(0)
                                .build(),
                        OfferRecommendation.builder()
                                .partnerId(12L)
                                .offerId("-101")
                                .name("Name_-101")
                                .bid(53)
                                .price(87442)
                                .origPrice(874.42)
                                .recommendedPrice(484.51)
                                .recommendedPromocode(41.853)
                                .build()
                )
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findAll_blue_empty_" +
                            "blue_shop_offer"
            ),
            before = "PriceRecommendationRepository/json/blueShopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findAll_blue_empty_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Синий партнер без товаров с рекомендацией")
    @Test
    public void findAll_blue_empty() {
        test(
                "findAll_blue_empty_",
                13L,
                PartnerType.FB,
                List.of()
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findAll_blue_list_" +
                            "blue_shop_offer"
            ),
            before = "PriceRecommendationRepository/json/blueShopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findAll_blue_list_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Синий партнер со списком товаров с рекомендацией")
    @Test
    public void findAll_blue_list() {
        test(
                "findAll_blue_list_",
                11L,
                PartnerType.FB,
                List.of(
                        OfferRecommendation.builder()
                                .partnerId(11L)
                                .offerId("-100")
                                .name("Name_-100")
                                .bid(984)
                                .price(9841400)
                                .origPrice(98414)
                                .recommendedPrice(98402)
                                .recommendedPromocode(0)
                                .build(),
                        OfferRecommendation.builder()
                                .partnerId(11L)
                                .offerId("-101")
                                .name("Name_-101")
                                .bid(530)
                                .price(874444)
                                .origPrice(874.42)
                                .recommendedPrice(484.51)
                                .recommendedPromocode(41.853)
                                .build(),
                        OfferRecommendation.builder()
                                .partnerId(11L)
                                .offerId("1100")
                                .name("Name_1100")
                                .bid(540)
                                .price(25343)
                                .origPrice(253.43)
                                .recommendedPrice(140.50)
                                .recommendedPromocode(12.0)
                                .build(),
                        OfferRecommendation.builder()
                                .partnerId(11L)
                                .offerId("1101")
                                .name("Name_1101")
                                .bid(1140)
                                .price(125520)
                                .origPrice(1255.2)
                                .recommendedPrice(1150)
                                .recommendedPromocode(50.59)
                                .build()
                )
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findAll_white_empty_" +
                            "shop_offer"
            ),
            before = "PriceRecommendationRepository/json/shopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findAll_white_empty_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Белый партнер без товаров с рекомендацией")
    @Test
    public void findAll_white_empty() {
        test(
                "findAll_white_empty_",
                14L,
                PartnerType.DBS,
                List.of()
        );
    }

    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/findAll_white_list_" +
                            "shop_offer"
            ),
            before = "PriceRecommendationRepository/json/shopOffer/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = PriceRecommendation.class,
                    path = "//tmp/findAll_white_list_" +
                            "price_recommendations"
            ),
            before = "PriceRecommendationRepository/json/priceRecommendations/" +
                    "findByPartnerIdAndCampaignId.json"
    )
    @DisplayName("Белый партнер со списком товаров с рекомендацией")
    @Test
    public void findAll_white_list() {
        test(
                "findAll_white_list_",
                12L,
                PartnerType.DBS,
                List.of(
                        OfferRecommendation.builder()
                                .partnerId(12L)
                                .offerId("1100")
                                .name("Name_1100")
                                .bid(540)
                                .price(25343)
                                .origPrice(253.43)
                                .recommendedPrice(140.50)
                                .recommendedPromocode(12.0)
                                .build(),
                        OfferRecommendation.builder()
                                .partnerId(12L)
                                .offerId("1101")
                                .name("Name_1101")
                                .bid(1140)
                                .price(125520)
                                .origPrice(1255.2)
                                .recommendedPrice(1150)
                                .recommendedPromocode(50.59)
                                .build(),
                        OfferRecommendation.builder()
                                .partnerId(12L)
                                .offerId("-100")
                                .name("Name_-100")
                                .bid(984)
                                .price(9841400)
                                .origPrice(98414)
                                .recommendedPrice(98402)
                                .recommendedPromocode(0)
                                .build(),
                        OfferRecommendation.builder()
                                .partnerId(12L)
                                .offerId("-101")
                                .name("Name_-101")
                                .bid(53)
                                .price(87442)
                                .origPrice(874.42)
                                .recommendedPrice(484.51)
                                .recommendedPromocode(41.853)
                                .build()
                )
        );
    }

    private void test(String prefix,
                      long partnerId,
                      long campaignId,
                      PartnerType partnerType,
                      CampaignVersion campaignVersion,
                      List<OfferRecommendation> expected) {
        run(prefix,
                () -> Assertions.assertThat(
                                priceRecommendationsRepository.findByPartnerIdAndCampaignId(
                                        partnerId,
                                        campaignId,
                                        partnerType,
                                        campaignVersion,
                                        10000
                                )
                        )
                        .containsExactlyInAnyOrderElementsOf(expected)
        );
    }

    private void test(String prefix,
                      long partnerId,
                      PartnerType partnerType,
                      List<OfferRecommendation> expected) {
        run(prefix,
                () -> Assertions.assertThat(
                                priceRecommendationsRepository.findAll(
                                        partnerId,
                                        partnerType,
                                        10000
                                )
                        )
                        .containsExactlyInAnyOrderElementsOf(expected)
        );
    }
}
