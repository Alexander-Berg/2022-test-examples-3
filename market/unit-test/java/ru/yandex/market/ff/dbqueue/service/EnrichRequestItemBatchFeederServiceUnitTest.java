package ru.yandex.market.ff.dbqueue.service;

import java.util.List;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.market.ff.dbqueue.producer.EnrichRequestItemQueueProducer;
import ru.yandex.market.ff.model.dbqueue.EnrichRequestItemPayload;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnrichRequestItemBatchFeederServiceUnitTest {

    @InjectMocks
    private EnrichRequestItemBatchFeederService service;
    @Mock
    private ConcreteEnvironmentParamService concreteEnvironmentParamService;
    @Mock
    private EnrichRequestItemQueueProducer producer;


    @Test
    public void feedTestWhenOk() {

        when(concreteEnvironmentParamService.getCountOfRequestItemsInBatchForEnrichment())
                .thenReturn(20);

        service.feed(1L, 99);

        ArgumentCaptor<EnrichRequestItemPayload> argument = ArgumentCaptor.forClass(EnrichRequestItemPayload.class);
        verify(producer, times(5)).produceSingle(argument.capture());

        List<EnrichRequestItemPayload> values = argument.getAllValues();
        assertEquals(0, values.get(0).getOffset());
        assertEquals(20, values.get(0).getLimit());

        assertEquals(20, values.get(1).getOffset());
        assertEquals(20, values.get(1).getLimit());

        assertEquals(40, values.get(2).getOffset());
        assertEquals(20, values.get(2).getLimit());

        assertEquals(60, values.get(3).getOffset());
        assertEquals(20, values.get(3).getLimit());

        assertEquals(80, values.get(4).getOffset());
        assertEquals(20, values.get(4).getLimit());
    }

    @Test
    public void feedTestWhenCountZero() {
        service.feed(1L, 0);
        verify(producer, times(0)).produceSingle(any());
    }


    @Test
    public void feedTestWhenBatchCountZero() {
        when(concreteEnvironmentParamService.getCountOfRequestItemsInBatchForEnrichment())
                .thenReturn(0);
        service.feed(1L, 100);
        verify(producer, times(1)).produceSingle(any());
    }


    private static Stream<Arguments> provideIntsForFeedParameterizedTest() {
        return Stream.of(
                Arguments.of(1, 1),
                Arguments.of(1000, 1),
                Arguments.of(4999, 1),
                Arguments.of(5000, 1),
                Arguments.of(5001, 2),
                Arguments.of(99999, 20),
                Arguments.of(999999, 200)
        );
    }

    @ParameterizedTest
    @MethodSource("provideIntsForFeedParameterizedTest")
    void feedParameterizedTest(int itemCount, int timesExpected) {
        when(concreteEnvironmentParamService.getCountOfRequestItemsInBatchForEnrichment())
                .thenReturn(5000);

        service.feed(1L, itemCount);
        ArgumentCaptor<EnrichRequestItemPayload> argument = ArgumentCaptor.forClass(EnrichRequestItemPayload.class);
        verify(producer, times(timesExpected)).produceSingle(argument.capture());

        validateBatchOrder(argument.getAllValues(), itemCount);
    }

    private void validateBatchOrder(List<EnrichRequestItemPayload> values, int itemCount) {
        int limitsSum = values.stream().map(EnrichRequestItemPayload::getLimit).reduce(0, Integer::sum);
        assertTrue(limitsSum >= itemCount, "Sum of limits should be greater or equals to item count");

        int last = 0;
        for (EnrichRequestItemPayload value : values) {
            assertEquals(last, value.getOffset(), "Some of items are not included in any batch");
            last = value.getLimit() + value.getOffset();
        }
    }
}
