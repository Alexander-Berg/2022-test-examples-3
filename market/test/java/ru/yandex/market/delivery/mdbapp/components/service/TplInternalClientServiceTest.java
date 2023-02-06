package ru.yandex.market.delivery.mdbapp.components.service;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.delivery.mdbapp.exception.TplInternalClientException;
import ru.yandex.market.delivery.mdbapp.integration.converter.ClientReturnCreateDtoConverter;
import ru.yandex.market.tpl.internal.client.TplInternalClient;
import ru.yandex.market.tpl.internal.client.model.clientreturn.ClientReturnCreateDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestDto;

@RunWith(MockitoJUnitRunner.class)
public class TplInternalClientServiceTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Mock
    public TplInternalClient client;
    @Spy
    public ClientReturnCreateDtoConverter converter = new ClientReturnCreateDtoConverter();

    @InjectMocks
    public TplInternalClientService service;

    @Test
    public void shouldCallWithoutErrors() {
        // when:
        service.createClientReturn(returnRequestDto());

        // then:
        verify(client).createClientReturn(any(ClientReturnCreateDto.class));
    }

    @Test(expected = TplInternalClientException.class)
    public void shouldThrowTplInternalClientException_whenFailed() {
        // given:
        doThrow(new RuntimeException())
            .when(client).createClientReturn(any(ClientReturnCreateDto.class));

        // when:
        service.createClientReturn(returnRequestDto());

        // then:
        verify(client).createClientReturn(any(ClientReturnCreateDto.class));
    }
}
