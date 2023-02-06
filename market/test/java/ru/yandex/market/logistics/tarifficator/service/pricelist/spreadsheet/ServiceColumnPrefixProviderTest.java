package ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.AbstractUnitTest;
import ru.yandex.market.logistics.tarifficator.model.enums.ServiceType;

@DisplayName("Unit-тест провайдера ServiceColumnPrefixProviderTest")
class ServiceColumnPrefixProviderTest extends AbstractUnitTest {

    private ServiceColumnPrefixProvider columnPrefixProvider = new ServiceColumnPrefixProvider();

    @Test
    @DisplayName("Проверка поддержки реализацией источника данных")
    void provide() {
        List<ServiceType> notFound = Arrays.stream(ServiceType.values())
            .filter(t -> columnPrefixProvider.provide(t) == null)
            .collect(Collectors.toList());

        softly.assertThat(notFound).isEmpty();
    }
}
