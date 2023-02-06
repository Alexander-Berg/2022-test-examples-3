package ru.yandex.direct.core.entity.client.service;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.client.repository.ClientCurrencyConversionTeaserRepository;
import ru.yandex.direct.core.entity.currency.model.CurrencyConversionState;
import ru.yandex.direct.core.entity.currency.repository.CurrencyConvertQueueRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CoreTest
@ParametersAreNonnullByDefault
public class ClientCurrencyConversionTeaserServiceTest {
    private ClientCurrencyConversionTeaserRepository mockClientCurrencyConversionTeaserRepository;
    private CurrencyConvertQueueRepository mockCurrencyConvertQueueRepository;
    private ShardHelper mockShardHelper;
    private ClientCurrencyConversionTeaserService clientCurrencyConversionTeaserService;
    private DirectConfig mockDirectConfig;

    @Before
    public void setUp() {
        mockClientCurrencyConversionTeaserRepository = mock(ClientCurrencyConversionTeaserRepository.class);
        mockCurrencyConvertQueueRepository = mock(CurrencyConvertQueueRepository.class);

        mockDirectConfig = mock(DirectConfig.class);
        when(mockDirectConfig.getDuration("client_currency_conversion.stop_operation_before"))
                .then(invocation -> Duration.ofMinutes(15));

        mockShardHelper = mock(ShardHelper.class);
        when(mockShardHelper.getShardByClientId(ClientId.fromLong(1L))).then(invocation -> 1);
        when(mockShardHelper.getShardByClientId(ClientId.fromLong(2L))).then(invocation -> 2);

        clientCurrencyConversionTeaserService = new ClientCurrencyConversionTeaserService(
                mockClientCurrencyConversionTeaserRepository,
                mockCurrencyConvertQueueRepository,
                mockShardHelper,
                mockDirectConfig);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_isClientConvertingSoon() {
        when(
                mockCurrencyConvertQueueRepository.fetchConvertingClients(
                        anyInt(),
                        anyCollection(),
                        any(),
                        anyCollection())
        ).then(
                invocation -> {
                    Object[] arguments = invocation.getArguments();

                    int shard = (Integer) arguments[0];
                    Collection<ClientId> clientIds = invocation.getArgument(1);
                    Duration duration = (Duration) arguments[2];
                    Collection<CurrencyConversionState> excludeStates =
                            (Collection<CurrencyConversionState>) arguments[3];

                    assertThat("clientIds пробросились в сервис правильно", clientIds, contains(ClientId.fromLong(1L)));
                    assertThat("excludeStates пробросились в сервис правильно",
                            excludeStates, contains(CurrencyConversionState.DONE));
                    assertThat("stopOperationMinutesBeforeConvert пробросился в сервис правильно",
                            duration, equalTo(Duration.ofMinutes(15)));
                    assertThat("shard пробросился в сервис правильно",
                            shard, is(1));

                    return Collections.singleton(ClientId.fromLong(1L));
                });

        assertThat("isClientConvertingSoon с фальшивым ответом от репозитория отдаёт правильный ответ",
                clientCurrencyConversionTeaserService.isClientConvertingSoon(
                        ClientId.fromLong(1L),
                        Collections.singletonList(CurrencyConversionState.DONE)),
                is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_isClientConvertingSoon_onearg() {
        when(
                mockCurrencyConvertQueueRepository.fetchConvertingClients(
                        anyInt(),
                        anyCollection(),
                        any(),
                        anyCollection())
        ).then(
                invocation -> {
                    Object[] arguments = invocation.getArguments();

                    int shard = (Integer) arguments[0];
                    Collection<ClientId> clientIds = invocation.getArgument(1);
                    Duration duration = (Duration) arguments[2];
                    Collection<CurrencyConversionState> excludeStates =
                            (Collection<CurrencyConversionState>) arguments[3];

                    assertThat("clientIds пробросились в сервис правильно", clientIds, contains(ClientId.fromLong(1L)));
                    assertThat("excludeStates пробросились в сервис правильно",
                            excludeStates, empty());
                    assertThat("stopOperationMinutesBeforeConvert пробросился в сервис правильно",
                            duration, equalTo(Duration.ofMinutes(15)));
                    assertThat("shard пробросился в сервис правильно",
                            shard, is(1));

                    return Collections.singleton(ClientId.fromLong(1L));
                });

        assertThat("isClientConvertingSoon с фальшивым ответом от репозитория отдаёт правильный ответ",
                clientCurrencyConversionTeaserService.isClientConvertingSoon(ClientId.fromLong(1L)),
                is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_doesClientHaveToConvert() {
        when(
                mockClientCurrencyConversionTeaserRepository.getClientsThatHaveToConvert(
                        anyInt(), anyCollection()
                )
        ).then(
                invocation -> {
                    Object[] arguments = invocation.getArguments();
                    int shard = (Integer) arguments[0];
                    Collection<ClientId> clientIds = (Collection<ClientId>) arguments[1];

                    assertThat("clientIds пробросились в сервис правильно", clientIds, contains(ClientId.fromLong(2L)));
                    assertThat("shard пробросился в сервис правильно",
                            shard, is(2));

                    return Collections.singleton(ClientId.fromLong(2L));
                });

        assertThat("doesClientHaveToConvert с фальшивым ответом от репозитория отдаёт правильный ответ",
                clientCurrencyConversionTeaserService.doesClientHaveToConvert(ClientId.fromLong(2L)),
                is(true));
    }

}
