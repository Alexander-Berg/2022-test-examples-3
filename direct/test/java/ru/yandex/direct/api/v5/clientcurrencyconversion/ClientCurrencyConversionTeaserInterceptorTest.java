package ru.yandex.direct.api.v5.clientcurrencyconversion;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.security.exception.AccessToApiDeniedException;
import ru.yandex.direct.core.TranslatableException;
import ru.yandex.direct.core.entity.client.service.ClientCurrencyConversionTeaserService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.security.AccessDeniedException;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class ClientCurrencyConversionTeaserInterceptorTest {
    private ApiContext mockApiContext;
    private boolean unitsChargedForRequest = true;
    private ClientCurrencyConversionTeaserService mockClientCurrencyConversionTeaserService;
    private ClientCurrencyConversionTeaserInterceptor clientCurrencyConversionTeaserInterceptor;

    @Parameterized.Parameter
    public String testCaseName;

    @Parameterized.Parameter(1)
    public boolean isClientConvertingSoon;

    @Parameterized.Parameter(2)
    public boolean doesClientHaveToConvert;

    @Parameterized.Parameter(3)
    public Class<? extends TranslatableException> expectedExceptionClass;

    @Parameterized.Parameter(4)
    public boolean expectUnitsChargedForRequest;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        Object[][] result = new Object[][]{
                new Object[]{
                        "клиент сейчас или в ближайшем будущем не конвертируется, конвертация не требуется",
                        false, false,
                        null, true
                },
                new Object[]{
                        "клиент сейчас или в ближайшем будущем конвертируется, конвертация не требуется",
                        true, false,
                        AccessToApiDeniedException.class, false
                },
                new Object[]{
                        "клиент сейчас или в ближайшем будущем не конвертируется, но конвертация требуется",
                        false, true,
                        AccessDeniedException.class, false
                },
                new Object[]{
                        // нереалистичный тест-кейс: если задание есть в очереди (сделанное или нет, не важно),
                        // тизер "согласитесь на конвертацию" уже не показывается, интерфейс и API блокируются
                        // по-другому до её окончания
                        "клиент сейчас или в ближайшем будущем конвертируется и конвертация требуется",
                        true, true,
                        AccessToApiDeniedException.class, false
                },
        };

        return Arrays.asList(result);
    }

    @Before
    public void setUp() throws Exception {
        mockApiContext = mock(ApiContext.class);
        doAnswer(invocation -> {
            unitsChargedForRequest = false;
            return null;
        })
                .when(mockApiContext).setShouldChargeUnitsForRequest(false);

        ApiContextHolder mockApiContextHolder = mock(ApiContextHolder.class);
        when(mockApiContextHolder.get()).then(invocation -> mockApiContext);

        mockClientCurrencyConversionTeaserService = mock(ClientCurrencyConversionTeaserService.class);

        when(mockClientCurrencyConversionTeaserService.isClientConvertingSoon(any(ClientId.class)))
                .then(invocation -> isClientConvertingSoon);
        when(
                mockClientCurrencyConversionTeaserService.isClientConvertingSoon(
                        any(ClientId.class), anyCollection()))
                .then(invocation -> isClientConvertingSoon);

        when(mockClientCurrencyConversionTeaserService.doesClientHaveToConvert(any(ClientId.class)))
                .then(invocation -> doesClientHaveToConvert);

        ApiUser mockApiUser = mock(ApiUser.class);
        when(mockApiUser.getClientId()).thenReturn(ClientId.fromLong(1L));
        ApiAuthenticationSource apiAuthenticationSource = mock(ApiAuthenticationSource.class);
        when(apiAuthenticationSource.getSubclient()).thenReturn(mockApiUser);

        clientCurrencyConversionTeaserInterceptor = new ClientCurrencyConversionTeaserInterceptor(
                mockClientCurrencyConversionTeaserService, mockApiContextHolder, apiAuthenticationSource);
    }

    @Test
    public void test() throws Exception {
        Class<? extends TranslatableException> caughtExceptionClass = null;

        try {
            clientCurrencyConversionTeaserInterceptor.handleRequest(null, null);
        } catch (TranslatableException e) {
            caughtExceptionClass = e.getClass();
        }
        if(expectedExceptionClass != null) {
            assertThat(caughtExceptionClass)
                    .as("получили исключение нужного класса")
                    .hasSameClassAs(expectedExceptionClass);
        }

        assertThat(unitsChargedForRequest).as("флаг списания баллов поставлен как надо")
                .isEqualTo(expectUnitsChargedForRequest);
    }

}
