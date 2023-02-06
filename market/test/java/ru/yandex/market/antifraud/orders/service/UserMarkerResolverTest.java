package ru.yandex.market.antifraud.orders.service;

import java.util.Set;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.UserMarkerDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author dzvyagin
 */
public class UserMarkerResolverTest {

    @Test
    public void resolveMarkers() {
        UserMarkerResolver resolver = new UserMarkerResolver(mock(ConfigurationService.class));
        assertThat(resolver.resolveMarker("reseller"))
            .get().isEqualTo(UserMarkerResolver.RESELLER_MARKER);
        assertThat(resolver.resolveMarker("unknown"))
            .get().isEqualTo(UserMarkerDto.unknown("unknown"));
        assertThat(resolver.resolveMarker("blue"))
            .get().isEqualTo(UserMarkerResolver.BLUE_MARKER);
        assertThat(resolver.resolveMarker("bad_acc"))
            .get().isEqualTo(UserMarkerResolver.BAD_ACC_MARKER);
        assertThat(resolver.resolveMarker("cancel"))
            .get().isEqualTo(UserMarkerResolver.CANCEL_MARKER);
    }

    @Test
    public void hideMarkers() {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.hideMarkers()).thenReturn(Set.of("reseller", "blue"));
        UserMarkerResolver resolver = new UserMarkerResolver(configurationService);
        assertThat(resolver.resolveMarker("reseller"))
            .isEmpty();
        assertThat(resolver.resolveMarker("unknown"))
            .get().isEqualTo(UserMarkerDto.unknown("unknown"));
        assertThat(resolver.resolveMarker("blue"))
            .isEmpty();
        assertThat(resolver.resolveMarker("bad_acc"))
            .get().isEqualTo(UserMarkerResolver.BAD_ACC_MARKER);
        assertThat(resolver.resolveMarker("cancel"))
            .get().isEqualTo(UserMarkerResolver.CANCEL_MARKER);
    }
}
