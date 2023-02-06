package ru.yandex.market.loyalty.back.controller;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.config.Recommendations;
import ru.yandex.market.loyalty.core.model.personal.ExperimentPersonalPromo;
import ru.yandex.market.loyalty.core.service.report.PersonalPerksRecommendationResponse;
import ru.yandex.market.loyalty.core.service.report.RecommendationsService;
import ru.yandex.market.loyalty.test.TestFor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@TestFor(RecommendationsService.class)
public class RecommendationsServiceTest extends MarketLoyaltyBackMockedDbTestBase {

    @Autowired
    @Recommendations
    private RestTemplate recommendationRestTemplate;

    @Autowired
    private RecommendationsService recommendationsService;

    @Value("${market.loyalty.recommendation.search.url}")
    protected String personalPromoUrl;

    private final String PERSONAL_PROMOS = "personal_promos";
    private final String YALOGIN_PROMOS2 = "yalogin_promos2";
    private final String YETANOTHEREXP_PROMO = "yetanotherexp_promo";
    private final String EMPTY = "empty";

    private final String PERK1 = "perk1";
    private final String PERK2 = "perk2";
    private final String PERK3 = "perk3";
    private final String PERK4 = "perk4";

    @Before
    public void setEnablePrimeFlag() {
        setEnablePrimeFlag(PERSONAL_PROMOS, PERK1, PERK3);
        setEnablePrimeFlag(YALOGIN_PROMOS2, PERK1, PERK2);
        setEnablePrimeFlag(YETANOTHEREXP_PROMO, PERK4);
        setEnablePrimeFlag(EMPTY);
    }

    @Test
    public void onePersonal() {
        ExperimentPersonalPromo experimentPersonalPromo = createExperimentPersonalPromo(Set.of(PERSONAL_PROMOS));
        Set<String> personalPerks = recommendationsService.getPersonalPerks(123L, true, experimentPersonalPromo);
        assertEquals(2, personalPerks.size());
        assertTrue(personalPerks.containsAll(Set.of(PERK1, PERK3)));
    }

    @Test
    public void twoPersonal() {
        ExperimentPersonalPromo experimentPersonalPromo = createExperimentPersonalPromo(
                Set.of(PERSONAL_PROMOS, YALOGIN_PROMOS2));
        Set<String> personalPerks = recommendationsService.getPersonalPerks(123L, true, experimentPersonalPromo);
        assertEquals(3, personalPerks.size());
        assertTrue(personalPerks.containsAll(Set.of(PERK1, PERK2, PERK3)));
    }

    @Test
    public void threePersonal() {
        ExperimentPersonalPromo experimentPersonalPromo = createExperimentPersonalPromo(
                Set.of(PERSONAL_PROMOS, YALOGIN_PROMOS2, YETANOTHEREXP_PROMO));
        Set<String> personalPerks = recommendationsService.getPersonalPerks(123L, true, experimentPersonalPromo);
        assertEquals(4, personalPerks.size());
        assertTrue(personalPerks.containsAll(Set.of(PERK1, PERK2, PERK3, PERK4)));
    }

    @Test
    public void emptyPersonal() {
        ExperimentPersonalPromo experimentPersonalPromo = createExperimentPersonalPromo(Set.of(EMPTY));
        Set<String> personalPerks = recommendationsService.getPersonalPerks(123L, true, experimentPersonalPromo);
        assertEquals(0, personalPerks.size());
    }

    @Test
    public void emptyAndAnyPersonal() {
        ExperimentPersonalPromo experimentPersonalPromo = createExperimentPersonalPromo(Set.of(EMPTY, PERSONAL_PROMOS));
        Set<String> personalPerks = recommendationsService.getPersonalPerks(123L, true, experimentPersonalPromo);
        assertEquals(2, personalPerks.size());
        assertTrue(personalPerks.containsAll(Set.of(PERK1, PERK3)));
    }

    private void setEnablePrimeFlag(String experiment, String... perk) {
        PersonalPerksRecommendationResponse response = new PersonalPerksRecommendationResponse();
        response.setPerks(Set.of(perk));

        when(recommendationRestTemplate.getForEntity(
                eq(UriComponentsBuilder.fromUriString(personalPromoUrl)
                        .queryParam("experiment", experiment)
                        .queryParam("puid", 123L)
                        .queryParam("perks", "yaplus")
                        .queryParam("rearr-factors", "")
                        .build().toUri()),
                eq(PersonalPerksRecommendationResponse.class)
        )).thenReturn(ResponseEntity.ok(response));
    }

    private ExperimentPersonalPromo createExperimentPersonalPromo(Set<String> code) {
        return new ExperimentPersonalPromo(code, "");
    }
}
