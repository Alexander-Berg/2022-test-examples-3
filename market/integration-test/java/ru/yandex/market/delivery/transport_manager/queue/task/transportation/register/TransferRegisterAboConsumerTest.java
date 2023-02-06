package ru.yandex.market.delivery.transport_manager.queue.task.transportation.register;

import java.time.LocalDate;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.abo.api.client.AboAPI;
import ru.yandex.market.abo.api.entity.resupply.registry.RegistryPosition;
import ru.yandex.market.abo.api.entity.resupply.registry.RegistryType;
import ru.yandex.market.abo.api.entity.resupply.registry.UploadRegistryRequest;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.abo.TransferAboRegisterTaskDto;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.abo.TransferRegisterAboConsumer;
import ru.yandex.market.delivery.transport_manager.util.DbQueueUtils;
import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;
import ru.yandex.market.tpl.common.lrm.client.model.SearchReturnsResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class TransferRegisterAboConsumerTest extends AbstractContextualTest {

    @Autowired
    private TransferRegisterAboConsumer consumer;
    @Autowired
    private AboAPI aboAPI;
    @Autowired
    private ReturnsApi client;


    @BeforeEach
    void setUpMock() {
        when(client.searchReturns(any())).thenReturn(new SearchReturnsResponse());
    }
    @Test
    @DatabaseSetup("/repository/register/transfer_register_abo.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/transfer_register_abo.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void registerSent() {
        UploadRegistryRequest expectedAboRequest = UploadRegistryRequest.Builder.newBuilder()
            .setType(RegistryType.UNPAID)
            .setDate(LocalDate.of(2020, 10, 22))
            .setDeliveryServiceId(42L)
            .setName("42-doc-26")
            .setWarehouseId(6L)
            .setLogisticPointId(2L)
            .setRegistryPositions(List.of(
                new RegistryPosition("42000000", null),
                new RegistryPosition("42000001", "edwe3r4fe2ef")
            ))
            .build();

        DbQueueUtils.assertExecutedSuccessfully(
            consumer,
            new TransferAboRegisterTaskDto(1L)
        );

        Mockito.verify(aboAPI, times(1))
            .uploadRegistry(Mockito.eq(expectedAboRequest));
    }

    @Test
    @DatabaseSetup("/repository/register/transfer_register_abo.xml")
    @ExpectedDatabase(
        value = "/repository/register/transfer_register_abo.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void registerSendingRetry() {
        Mockito.doThrow(HttpServerErrorException.class)
            .when(aboAPI).uploadRegistry(any());

        // После неуспешного выполнения будет повторная попытка
        DbQueueUtils.assertExecutedWithFailure(
            consumer,
            new TransferAboRegisterTaskDto(1L)
        );

        Mockito.verify(aboAPI, times(1))
            .uploadRegistry(any());
    }

    @Test
    @DatabaseSetup("/repository/register/transfer_register_abo.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/transfer_register_abo_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidRegisterSendingFailed() {
        Mockito.doThrow(HttpClientErrorException.class)
            .when(aboAPI).uploadRegistry(any());

        DbQueueUtils.assertExecutedSuccessfully(
            consumer,
            new TransferAboRegisterTaskDto(1L)
        );

        Mockito.verify(aboAPI, times(1))
            .uploadRegistry(any());
    }

    @Test
    @DatabaseSetup("/repository/register/transfer_invalid_register_abo.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/transfer_register_abo_wrong_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void registerSendingFailedOnConversionException() {
        // Пробуем отправить реестр типа FACT
        DbQueueUtils.assertExecutedSuccessfully(
            consumer,
            new TransferAboRegisterTaskDto(1L)
        );

        Mockito.verifyZeroInteractions(aboAPI);
    }
}
