package ru.yandex.market.loyalty.back.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.loyalty.api.model.ExperimentGetFlagsResponseDto;
import ru.yandex.market.loyalty.api.model.ExperimentPostFlagsResponseDto;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.perk.WelcomeCoinParams;
import ru.yandex.market.loyalty.core.service.perks.PerkService;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(ExperimentsController.class)
public class ExperimentsControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private CheckouterClient checkouterClient;
    @Autowired
    private PerkService perkService;

    @Test
    public void shouldNotGetStoredFlagsForDisabledPerkType() {
        List<String> flags = Collections.singletonList("testFlag");

        WelcomeCoinParams welcomeCoinParams = new WelcomeCoinParams();
        welcomeCoinParams.setFlags(flags);
        perkService.grantPerkIfNotExistsWithoutOrder(DEFAULT_UID, PerkType.WELCOME_COIN, welcomeCoinParams,
                WelcomeCoinParams.class);

        ExperimentGetFlagsResponseDto welcomeFlags = marketLoyaltyClient.getWelcomeFlags(DEFAULT_UID);
        assertThat(
                welcomeFlags.getFlags(),
                empty()
        );
        assertFalse(welcomeFlags.getHasBindings());
        assertNull(welcomeFlags.getWillCreateBindings());
    }

    @Test
    public void shouldNotGetNotStoredFlags() {
        ExperimentGetFlagsResponseDto welcomeFlags = marketLoyaltyClient.getWelcomeFlags(DEFAULT_UID);
        assertThat(
                welcomeFlags.getFlags(),
                empty()
        );
        assertFalse(welcomeFlags.getHasBindings());
        assertNull(welcomeFlags.getWillCreateBindings());
    }

    @Test
    public void shouldNotGetNotStoredFlagsIfOrdersExists() {
        when(checkouterClient.getOrdersCount(any(), any())).thenReturn(1);

        ExperimentGetFlagsResponseDto welcomeFlags = marketLoyaltyClient.getWelcomeFlags(DEFAULT_UID, true);
        assertThat(
                welcomeFlags.getFlags(),
                empty()
        );
        assertFalse(welcomeFlags.getHasBindings());
        assertEquals(false, welcomeFlags.getWillCreateBindings());
    }

    @Test
    public void shouldNotGetNotStoredFlagsIfOrdersDoesNotExists() {
        ExperimentGetFlagsResponseDto welcomeFlags = marketLoyaltyClient.getWelcomeFlags(DEFAULT_UID, true);
        assertThat(
                welcomeFlags.getFlags(),
                empty()
        );
        assertFalse(welcomeFlags.getHasBindings());
        assertEquals(true, welcomeFlags.getWillCreateBindings());
    }

    @Test
    public void shouldNotSaveIfOrdersExistsAndGetStoredFlags() {
        List<String> flags = Collections.singletonList("testFlag");

        when(checkouterClient.getOrdersCount(any(), any(), any())).thenReturn(1);

        ExperimentPostFlagsResponseDto experimentPostFlagsResponseDto = marketLoyaltyClient.putWelcomeFlags(
                DEFAULT_UID, flags);
        assertThat(
                experimentPostFlagsResponseDto.getFlags(),
                empty()
        );
        assertFalse(experimentPostFlagsResponseDto.getAlreadyHasBindings());
        assertFalse(experimentPostFlagsResponseDto.isBindingsStored());

        ExperimentGetFlagsResponseDto welcomeFlags = marketLoyaltyClient.getWelcomeFlags(DEFAULT_UID);
        assertThat(
                welcomeFlags.getFlags(),
                empty()
        );
        assertFalse(welcomeFlags.getHasBindings());
        assertNull(welcomeFlags.getWillCreateBindings());
    }
}
