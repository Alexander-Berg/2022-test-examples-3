package ru.yandex.market.core.fulfillment.tariff;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Тесты для {@link TariffsIterator}
 */
@ParametersAreNonnullByDefault
class TariffsIteratorTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 100, 500, 800})
    void testIterator(int batchSize) {
        TariffsIterator tariffsIterator = new TariffsIterator(new MockTariffFindFunction(), batchSize);
        int tariffsCount = 0;
        while (tariffsIterator.hasNext()) {
            List<TariffDTO> data = tariffsIterator.next();
            tariffsCount += data.size();
        }
        assertEquals(Math.min(batchSize, 500) * 3, tariffsCount);
    }

    private static class MockTariffFindFunction implements TariffFindFunction {
        private final int MAX_FETCH_COUNT = 3;
        private int fetchCount = 0;

        @Override
        public List<TariffDTO> fetch(int pageNumber, int batchSize) {
            if (fetchCount >= MAX_FETCH_COUNT) {
                return List.of();
            }
            fetchCount++;

            return Stream.generate(() -> mock(TariffDTO.class))
                    .limit(batchSize)
                    .collect(Collectors.toList());

        }
    }
}
