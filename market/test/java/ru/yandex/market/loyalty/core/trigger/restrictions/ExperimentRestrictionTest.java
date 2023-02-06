package ru.yandex.market.loyalty.core.trigger.restrictions;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.core.config.Uaas;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.core.service.uaas.UaasService.EXP_BOXES_HEADER;
import static ru.yandex.market.loyalty.core.trigger.restrictions.ExperimentsDto.withFlag;
import static ru.yandex.market.loyalty.core.trigger.restrictions.ExperimentsDto.withTestIds;
import static ru.yandex.market.loyalty.core.utils.EventFactory.orderStatusUpdated;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withExperiments;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUid;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.experimentRestriction;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.negateExperimentRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.lightweight.CollectionUtils.concat;

public class ExperimentRestrictionTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoService promoService;
    @Uaas
    @Autowired
    private RestTemplate uaasRestTemplate;

    @Test
    public void shouldNotCreateCoinIfUserInExceptExperiment() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        Set<String> someExperiments = ImmutableSet.of("1", "2", "3");
        Set<String> someExcludeExperiments = ImmutableSet.of("66");

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                experimentRestriction(withTestIds(someExperiments)),
                negateExperimentRestriction(withTestIds(someExcludeExperiments))
        );

        setUserExperiments(DEFAULT_UID, concat(someExperiments, someExcludeExperiments).collect(Collectors.toSet()));

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(withUid(DEFAULT_UID)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS)
        );
    }

    @Test
    public void shouldCreateCoinIfUserInExperiment() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        Set<String> someExperiments = ImmutableSet.of("1", "2", "3");
        Set<String> someExcludeExperiments = ImmutableSet.of("66");

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                experimentRestriction(withTestIds(someExperiments)),
                negateExperimentRestriction(withTestIds(someExcludeExperiments))
        );

        setUserExperiments(DEFAULT_UID, someExperiments);

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withUid(DEFAULT_UID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldCreateCoinIfUserInExperimentByFlag() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        String someFlag = "market_loyalty_flag";

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                experimentRestriction(withFlag(someFlag))
        );

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withExperiments(someFlag),
                withUid(DEFAULT_UID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldParseExperimentsWithDuplicates() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        String someFlag = "market_loyalty_flag=1;market_loyalty_flag=1";

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                experimentRestriction(withFlag("market_loyalty_flag"))
        );

        triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withExperiments(someFlag),
                withUid(DEFAULT_UID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }


    private void setUserExperiments(long uid, Set<String> splitIds) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(EXP_BOXES_HEADER, splitIds.stream().map(p -> p + ",12,223").collect(Collectors.joining(";")));
        when(uaasRestTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(headers, HttpStatus.OK));
    }
}
