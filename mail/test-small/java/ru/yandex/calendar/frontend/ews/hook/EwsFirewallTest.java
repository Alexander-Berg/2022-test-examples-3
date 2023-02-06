package ru.yandex.calendar.frontend.ews.hook;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.commune.a3.security.UnauthenticatedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EwsFirewallTest {
    @Test
    public void emptyFirewallListShouldRaiseException() {
        assertThatThrownBy(() -> EwsFirewallImpl.checkSrc("2a02:6b8:b010:5011::be20",
                new EwsFirewallConfig(true, Collections.emptyList())))
            .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    public void checkSameAddressInDifferentFormats() {
        EwsFirewallImpl.checkSrc("2a02:6b8:b010:5011::be20", new EwsFirewallConfig(true,
                Arrays.asList("2a02:6b8:b010:5011:0000::be20")));
    }

    @Test
    public void checkSameAddressInDifferentRegistry() {
        EwsFirewallImpl.checkSrc("2a02:6b8:b010:5011::be20", new EwsFirewallConfig(true,
                Arrays.asList("2a02:6b8:b010:5011::be20".toUpperCase())));
    }

    @Test
    public void firewallListWithAnotherIpShouldRaiseException() {
        assertThatThrownBy(() -> EwsFirewallImpl.checkSrc("2a02:6b8:b010:5011::be20", new EwsFirewallConfig(true,
                Arrays.asList("2a02:6b8:b010:5011::be21"))))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    public void wrongFormatShouldRaiseException() {
        assertThatThrownBy(() -> EwsFirewallImpl.checkSrc("Weird", new EwsFirewallConfig(true,
                Arrays.asList("2a02:6b8:b010:5011::be21"))))
                .isInstanceOf(UnauthenticatedException.class);
    }
}
