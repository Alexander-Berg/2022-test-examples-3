package ru.yandex.market.delivery.mdbapp.components.service;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.delivery.mdbapp.exception.PvzLogisticsClientException;
import ru.yandex.market.delivery.mdbapp.integration.converter.ReturnItemCreateDtoConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.ReturnRequestCreateDtoConverter;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;
import ru.yandex.market.pvz.client.logistics.dto.ReturnRequestCreateDto;
import ru.yandex.market.pvz.client.logistics.dto.ReturnRequestResponseDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestCreateDtoWithItems;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestDtoWithItems;

@RunWith(MockitoJUnitRunner.class)
public class PvzLogisticsClientServiceTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Mock
    PvzLogisticsClient client;
    @Spy
    ReturnRequestCreateDtoConverter converter = new ReturnRequestCreateDtoConverter(new ReturnItemCreateDtoConverter());

    @InjectMocks
    public PvzLogisticsClientService service;

    @Test
    public void shouldReturnResponse() {
        // given:
        final var returnRequest = returnRequestDtoWithItems();
        final var response = ReturnRequestResponseDto.builder().build();
        when(client.createReturnRequest(any(ReturnRequestCreateDto.class)))
            .thenReturn(response);

        // when:
        final var actual = service.createReturnRequest(returnRequest);

        // then:
        verify(client).createReturnRequest(returnRequestCreateDtoWithItems());
        softly.assertThat(actual).isEqualTo(response);
    }

    @Test(expected = PvzLogisticsClientException.class)
    public void shouldThrowPvzLogisticsClientException_whenFailed() {
        // given:
        final var returnRequest = returnRequestDtoWithItems();
        doThrow(new RuntimeException())
            .when(client).createReturnRequest(any(ReturnRequestCreateDto.class));

        // when:
        service.createReturnRequest(returnRequest);

        // then:
        verify(client).createReturnRequest(returnRequestCreateDtoWithItems());
    }
}
