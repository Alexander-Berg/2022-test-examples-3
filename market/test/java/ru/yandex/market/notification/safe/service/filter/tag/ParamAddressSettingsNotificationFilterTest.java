package ru.yandex.market.notification.safe.service.filter.tag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.market.notification.model.context.AddressFilterSettingsProviderContext;
import ru.yandex.market.notification.model.context.FilterParamCreatorContext;
import ru.yandex.market.notification.model.filter.AddressFilterSettings;
import ru.yandex.market.notification.model.filter.FilterParams;
import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.safe.model.data.PersistentDeliveryData;
import ru.yandex.market.notification.safe.model.type.NotificationStatus;
import ru.yandex.market.notification.service.provider.filter.AddressFilterSettingsProvider;
import ru.yandex.market.notification.service.provider.filter.FilterParamProvider;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.service.filter.ParamAddressSettingsNotificationFilter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.MOBILE_PUSH;

@SuppressWarnings("unchecked")
public class ParamAddressSettingsNotificationFilterTest {
    NotificationAddress address1 = mock(NotificationAddress.class);
    NotificationAddress address2 = mock(NotificationAddress.class);
    NotificationAddress address3 = mock(NotificationAddress.class);
    FilterParamProvider<FilterParams> paramProvider = mock(FilterParamProvider.class);
    AddressFilterSettingsProvider<AddressFilterSettings> addressFilterSettingsProvider = mock(AddressFilterSettingsProviderImpl.class);

    @BeforeEach
    void prepareTests(){

        AddressFilterSettings filterSettings1 = filterSettings("SCOPE1");
        AddressFilterSettings filterSettings2 = filterSettings("SCOPE2");

        when(addressFilterSettingsProvider.provide(argThat(context -> matchContextByAddress(context, address1))))
                .thenReturn(List.of());
        when(addressFilterSettingsProvider.provide(argThat(context -> matchContextByAddress(context, address2))))
                .thenReturn(List.of(filterSettings2));
        when(addressFilterSettingsProvider.provide(argThat(context -> matchContextByAddress(context, address3))))
                .thenReturn(List.of(filterSettings1, filterSettings2));
    }

    @Test
    public void testFilterAddressesWhenTrue() {
        FilterParams filterParams1 = filterParams("SCOPE1", true);
        FilterParams filterParams2 = filterParams("SCOPE2", false);
        when(paramProvider.provide(eq(0L))).thenReturn(
                List.of(
                        filterParams1,
                        filterParams2
                )
        );
        ParamAddressSettingsNotificationFilter<AddressFilterSettings, FilterParams> filter = new ParamAddressSettingsNotificationFilter<>(paramProvider, addressFilterSettingsProvider) {
            @Override
            public boolean doFiltration(FilterParams params, AddressFilterSettings settings) {
                return true;
            }

            @Override
            public void createParams(FilterParamCreatorContext context, long notificationId) {
            }
        };

        Collection<NotificationAddress> res = filter.filterAddresses(List.of(address1, address2, address3), createNotification());
        Assertions.assertTrue(res.contains(address3));
        Assertions.assertTrue(res.contains(address2));
        Assertions.assertFalse(res.contains(address1));
    }

    @Test
    public void testFilterAddressesWhenFalse() {
        FilterParams filterParams1 = filterParams("SCOPE1", true);
        FilterParams filterParams2 = filterParams("SCOPE2", true);
        when(paramProvider.provide(eq(0L))).thenReturn(
                List.of(
                        filterParams1,
                        filterParams2
                )
        );
        ParamAddressSettingsNotificationFilter<AddressFilterSettings, FilterParams> filter = new ParamAddressSettingsNotificationFilter<>(paramProvider, addressFilterSettingsProvider) {
            @Override
            public boolean doFiltration(FilterParams params, AddressFilterSettings settings) {
                return false;
            }

            @Override
            public void createParams(FilterParamCreatorContext context, long notificationId) {
            }
        };

        Collection<NotificationAddress> res = filter.filterAddresses(List.of(address1, address2, address3), createNotification());
        Assertions.assertFalse(res.contains(address3));
        Assertions.assertFalse(res.contains(address2));
        Assertions.assertTrue(res.contains(address1));
    }

    private boolean matchContextByAddress(AddressFilterSettingsProviderContext context, NotificationAddress address){
        return Optional.ofNullable(context)
                .map(AddressFilterSettingsProviderContext::getAddress)
                .map(obj -> obj.equals(address)).orElse(false);
    }

    private PersistentNotification createNotification() {
        PersistentDeliveryData deliveryData = mock(PersistentDeliveryData.class);
        when(deliveryData.getTransportType()).thenReturn(MOBILE_PUSH);
        return new PersistentNotification(
                0L,
                0L, new
                CodeNotificationType(0L),
                NotificationStatus.NEW,
                Instant.now(),
                mock(PersistentBinaryData.class),
                deliveryData,
                "",
                false
        );
    }

    private FilterParams filterParams(String scope, boolean ignoreIfAbsent) {
        FilterParams params = mock(FilterParams.class);
        when(params.getScope()).thenReturn(scope);
        when(params.getIgnoreIfAbsent()).thenReturn(ignoreIfAbsent);
        return params;
    }

    private AddressFilterSettings filterSettings(String scope) {
        AddressFilterSettings filterSettings = mock(AddressFilterSettings.class);
        when(filterSettings.getScope()).thenReturn(scope);
        return filterSettings;
    }

}
