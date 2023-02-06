package ru.yandex.market.wms.servicebus.configuration;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.common.spring.servicebus.vendor.VendorProvider;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.model.entity.VendorSettings;
import ru.yandex.market.wms.servicebus.repository.VendorSettingsRepository;

@Configuration
public class VendorSettingsRepositoryMockConfiguration {

    private static final String DEMATIC_UNIT_ID_PATTERN =
            "^[TCPBASEIFDHVLRGM][TSMLHCOXABDEFGIJKNPQRUVWYZ]\\d{6}[a-zA-Z\\d]{2}$";
    private static final String SCHAEFER_UNIT_ID_PATTERN = "^[T|L|P][0-9a-zA-Z]{1,19}|^UNKNOWN$";

    @Bean
    @Primary
    public VendorSettingsRepository repository() {
        final VendorSettingsRepository repository = Mockito.mock(VendorSettingsRepository.class);
        final List<VendorSettings> vendorSettings = getVendorSettings();

        Mockito.when(repository.getByVendorName(VendorProvider.SCHAEFER)).thenReturn(vendorSettings.get(0));
        Mockito.when(repository.getByVendorName(VendorProvider.DEMATIC)).thenReturn(vendorSettings.get(1));
        Mockito.when(repository.getAll()).thenReturn(vendorSettings);
        return repository;
    }

    private static List<VendorSettings> getVendorSettings() {
        return ImmutableList.of(
                VendorSettings.builder()
                        .id(1)
                        .vendorName(VendorProvider.SCHAEFER.name())
                        .url(getMockWebServerUrl())
                        .token(VendorProvider.SCHAEFER.name())
                        .unitIdPattern(SCHAEFER_UNIT_ID_PATTERN)
                        .build(),
                VendorSettings.builder()
                        .id(2)
                        .vendorName(VendorProvider.DEMATIC.name())
                        .url(getMockWebServerUrl())
                        .token(VendorProvider.DEMATIC.name())
                        .unitIdPattern(DEMATIC_UNIT_ID_PATTERN)
                        .build()
        );
    }

    private static String getMockWebServerUrl() {
        return String.format("http://localhost:%s", IntegrationTest.MOCK_WEB_SERVER_PORT);
    }
}
