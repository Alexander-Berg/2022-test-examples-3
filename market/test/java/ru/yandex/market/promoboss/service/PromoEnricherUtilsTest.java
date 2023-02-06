package ru.yandex.market.promoboss.service;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.yandex.market.promoboss.model.GenerateableUrl;
import ru.yandex.market.promoboss.model.MechanicsType;
import ru.yandex.market.promoboss.model.Promo;
import ru.yandex.market.promoboss.model.PromoField;
import ru.yandex.market.promoboss.model.PromoMainParams;
import ru.yandex.market.promoboss.model.PromoMechanicsParams;
import ru.yandex.market.promoboss.model.mechanics.CheapestAsGift;
import ru.yandex.market.promoboss.utils.PromoEnricherUtils;
import ru.yandex.market.promoboss.utils.PromoFieldUtilsTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PromoEnricherUtilsTest {

    private final static String promoId = "42";
    private static Promo promo;

    @BeforeAll
    public static void setup() {
        promo = Promo.builder()
                .promoId(promoId)
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(CheapestAsGift.builder()
                                        .count(4)
                                        .build())
                                .build()
                )
                .build();
    }

    @Test
    public void shouldEnrichPromo() {

        // setup
        String expectedPromoKey = "odDG6D8CcyfYRhBj9KxYpg";
        String expectedLandingUrl = "https://market.yandex.ru/special/cheapest-as-gift-3-4-landing?shopPromoId=42";
        String expectedRulesUrl = "https://market.yandex.ru/special/cheapest-as-gift?shopPromoId=42";

        promo.getMainParams().setPromoKey(null);

        promo.getMainParams().setLandingUrl(
                GenerateableUrl.builder()
                        .url(null)
                        .auto(true)
                        .build()
        );

        promo.getMainParams().setRulesUrl(
                GenerateableUrl.builder()
                        .url(null)
                        .auto(true)
                        .build()
        );

        // act
        PromoEnricherUtils.enrichPromoBeforeCreate(PromoFieldUtilsTest.createAll(), promo);

        // verify
        assertEquals(promoId, promo.getPromoId());
        assertEquals(expectedPromoKey, promo.getMainParams().getPromoKey());
        assertEquals(expectedLandingUrl, promo.getMainParams().getLandingUrl().getUrl());
        assertTrue(promo.getMainParams().getLandingUrl().isAuto());
        assertEquals(expectedRulesUrl, promo.getMainParams().getRulesUrl().getUrl());
        assertTrue(promo.getMainParams().getRulesUrl().isAuto());
    }

    @Test
    public void shouldNotGenerateNewUrl() {

        // setup
        String promoId = "42";
        CheapestAsGift cheapestAsGift = new CheapestAsGift(4);

        String landingUrl = "someLandingUrl";
        String rulesUrl = "someRulesUrl";

        Promo promo = Promo.builder()
                .promoId(promoId)
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url(landingUrl)
                                                .auto(false)
                                                .build()
                                )
                                .rulesUrl(
                                        GenerateableUrl.builder()
                                                .url(rulesUrl)
                                                .auto(false)
                                                .build()
                                )
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(cheapestAsGift)
                                .build()
                )
                .build();

        String expectedPromoKey = "odDG6D8CcyfYRhBj9KxYpg";

        // act
        assertNull(promo.getMainParams().getPromoKey());
        assertNotNull(promo.getMainParams().getLandingUrl());
        assertNotNull(promo.getMainParams().getRulesUrl());

        PromoEnricherUtils.enrichPromoBeforeCreate(PromoFieldUtilsTest.createAll(), promo);

        // verify
        assertEquals(promoId, promo.getPromoId());
        assertEquals(landingUrl, promo.getMainParams().getLandingUrl().getUrl());
        assertFalse(promo.getMainParams().getLandingUrl().isAuto());
        assertEquals(rulesUrl, promo.getMainParams().getRulesUrl().getUrl());
        assertFalse(promo.getMainParams().getRulesUrl().isAuto());

        assertEquals(expectedPromoKey, promo.getMainParams().getPromoKey());
    }

    @Test
    public void shouldGeneratePromoKey() {

        // setup
        String expectedPromoKey = "odDG6D8CcyfYRhBj9KxYpg";

        // act
        String promoKey = PromoEnricherUtils.generatePromoKey(promo);

        // verify
        assertEquals(expectedPromoKey, promoKey);
    }


    @Test
    public void shouldGenerateLandingUrlWithCountOfGift() {
        // setup
        String expectedLandingUrl = "https://market.yandex.ru/special/cheapest-as-gift-3-4-landing?shopPromoId=42";

        // act
        String landingUrl = PromoEnricherUtils.generateLandingUrl(promo);

        // verify
        assertEquals(expectedLandingUrl, landingUrl);
    }

    @Test
    public void shouldGenerateLandingUrlWithoutCountOfGiftWhenCountMoreThenThreshold() {
        // setup
        Promo promo = Promo.builder()
                .promoId("42")
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(
                                        CheapestAsGift.builder()
                                                .count(6)
                                                .build()
                                )
                                .build()
                )
                .build();

        String expectedLandingUrl = "https://market.yandex.ru/special/cheapest-as-gift-landing?shopPromoId=42";

        // act
        String landingUrl = PromoEnricherUtils.generateLandingUrl(promo);

        // verify
        assertEquals(expectedLandingUrl, landingUrl);
    }

    @Test
    void shouldGenerateRulesUrl() {
        // setup
        String expectedRulesUrl = "https://market.yandex.ru/special/cheapest-as-gift?shopPromoId=42";

        // act
        String landingUrl = PromoEnricherUtils.generateRulesUrl(promo);

        // verify
        assertEquals(expectedRulesUrl, landingUrl);
    }

    @Test
    public void shouldAddField() {

        // setup
        String promoId = "42";
        CheapestAsGift cheapestAsGift = new CheapestAsGift(4);

        Promo promo = Promo.builder()
                .promoId(promoId)
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url("")
                                                .auto(true)
                                                .build()
                                )
                                .rulesUrl(
                                        GenerateableUrl.builder()
                                                .url(null)
                                                .auto(true)
                                                .build()
                                )
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(cheapestAsGift)
                                .build()
                )
                .build();

        Set<PromoField> promoFields = PromoFieldUtilsTest.createAll();
        promoFields.remove(PromoField.MAIN);

        // act
        PromoEnricherUtils.enrichPromoBeforeCreate(promoFields, promo);

        // verify
        assertTrue(promoFields.contains(PromoField.MAIN));
    }

    @Test
    public void shouldUpdateUrlsIfAutogeneratedAndNotEqualsWithNew() {

        // setup
        String landingUrl = "landingUrlOld";
        String rulesUrl = "rulesUrlOld";

        Promo promo = Promo.builder()
                .promoId("cf_123")
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url(landingUrl)
                                                .auto(true)
                                                .build()
                                )
                                .rulesUrl(
                                        GenerateableUrl.builder()
                                                .url(rulesUrl)
                                                .auto(true)
                                                .build()
                                )
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(CheapestAsGift.builder()
                                        .count(123)
                                        .build())
                                .build()
                )
                .build();

        // act
        PromoEnricherUtils.enrichPromoBeforeUpdate(PromoFieldUtilsTest.createAll(), promo);

        // verify
        assertFalse(StringUtils.isBlank(promo.getMainParams().getLandingUrl().getUrl()));
        assertNotEquals(landingUrl, promo.getMainParams().getLandingUrl().getUrl());
        assertTrue(promo.getMainParams().getLandingUrl().isAuto());

        assertFalse(StringUtils.isBlank(promo.getMainParams().getRulesUrl().getUrl()));
        assertNotEquals(rulesUrl, promo.getMainParams().getRulesUrl().getUrl());
        assertTrue(promo.getMainParams().getLandingUrl().isAuto());
    }

    @Test
    public void shouldNotUpdateUrlsIfNotAutogenerated() {

        // setup
        String landingUrl = "landingUrlOld";
        String rulesUrl = "rulesUrlOld";

        Promo promo = Promo.builder()
                .promoId("cf_123")
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url(landingUrl)
                                                .auto(false)
                                                .build()
                                )
                                .rulesUrl(
                                        GenerateableUrl.builder()
                                                .url(rulesUrl)
                                                .auto(false)
                                                .build()
                                )
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(CheapestAsGift.builder()
                                        .count(123)
                                        .build())
                                .build()
                )
                .build();

        // act
        PromoEnricherUtils.enrichPromoBeforeUpdate(PromoFieldUtilsTest.createAll(), promo);

        // verify
        assertFalse(StringUtils.isBlank(promo.getMainParams().getLandingUrl().getUrl()));
        assertEquals(landingUrl, promo.getMainParams().getLandingUrl().getUrl());
        assertFalse(promo.getMainParams().getLandingUrl().isAuto());

        assertFalse(StringUtils.isBlank(promo.getMainParams().getRulesUrl().getUrl()));
        assertEquals(rulesUrl, promo.getMainParams().getRulesUrl().getUrl());
        assertFalse(promo.getMainParams().getRulesUrl().isAuto());
    }

    @Test
    public void shouldUpdateUrlsIfAutogeneratedAndCountIsDifferent() {

        // setup
        Promo oldPromo = Promo.builder()
                .promoId("cf_123")
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url("landingUrl")
                                                .auto(true)
                                                .build()
                                )
                                .rulesUrl(
                                        GenerateableUrl.builder()
                                                .url("rulesUrl")
                                                .auto(true)
                                                .build()
                                )
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(CheapestAsGift.builder()
                                        .count(3)
                                        .build())

                                .build()
                )
                .build();

        String landingUrl = PromoEnricherUtils.generateLandingUrl(oldPromo);
        String rulesUrl = PromoEnricherUtils.generateRulesUrl(oldPromo);

        Promo promo = Promo.builder()
                .promoId("cf_123")
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url(landingUrl)
                                                .auto(true)
                                                .build()
                                )
                                .rulesUrl(
                                        GenerateableUrl.builder()
                                                .url(rulesUrl)
                                                .auto(true)
                                                .build()
                                )
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(CheapestAsGift.builder()
                                        .count(4)
                                        .build())
                                .build()
                )
                .build();

        // act
        PromoEnricherUtils.enrichPromoBeforeUpdate(PromoFieldUtilsTest.createAll(), promo);

        // verify
        assertFalse(StringUtils.isBlank(promo.getMainParams().getLandingUrl().getUrl()));
        assertNotEquals(landingUrl, promo.getMainParams().getLandingUrl().getUrl());
        assertTrue(promo.getMainParams().getLandingUrl().isAuto());

        assertFalse(StringUtils.isBlank(promo.getMainParams().getRulesUrl().getUrl()));
        assertEquals(rulesUrl, promo.getMainParams().getRulesUrl().getUrl());
        assertTrue(promo.getMainParams().getRulesUrl().isAuto());
    }

    @Test
    public void shouldNotUpdateUrlsIfNotAutogeneratedAndEqualsWithNewAndCountIsDifferent() {

        // setup
        Promo oldPromo = Promo.builder()
                .promoId("cf_123")
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url("landingUrl")
                                                .auto(false)
                                                .build()
                                )
                                .rulesUrl(
                                        GenerateableUrl.builder()
                                                .url("rulesUrl")
                                                .auto(false)
                                                .build()
                                )
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(CheapestAsGift.builder()
                                        .count(3)
                                        .build())
                                .build()
                )
                .build();

        String landingUrl = PromoEnricherUtils.generateLandingUrl(oldPromo);
        String rulesUrl = PromoEnricherUtils.generateRulesUrl(oldPromo);

        Promo promo = Promo.builder()
                .promoId("cf_123")
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url(landingUrl)
                                                .auto(false)
                                                .build()
                                )
                                .rulesUrl(
                                        GenerateableUrl.builder()
                                                .url(rulesUrl)
                                                .auto(false)
                                                .build()
                                )
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(CheapestAsGift.builder()
                                        .count(4)
                                        .build())
                                .build()
                )
                .build();

        // act
        PromoEnricherUtils.enrichPromoBeforeUpdate(PromoFieldUtilsTest.createAll(), promo);

        // verify
        assertFalse(StringUtils.isBlank(promo.getMainParams().getLandingUrl().getUrl()));
        assertEquals(landingUrl, promo.getMainParams().getLandingUrl().getUrl());
        assertFalse(promo.getMainParams().getLandingUrl().isAuto());

        assertFalse(StringUtils.isBlank(promo.getMainParams().getRulesUrl().getUrl()));
        assertEquals(rulesUrl, promo.getMainParams().getRulesUrl().getUrl());
        assertFalse(promo.getMainParams().getRulesUrl().isAuto());
    }
}
