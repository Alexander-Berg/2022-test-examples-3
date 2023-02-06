package ru.yandex.market.sc.core.domain.transfer_act;

import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;
import ru.yandex.market.sc.core.domain.movement_courier.repository.MovementCourier;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.WarehouseQueryService;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.common.transferact.client.model.TransferCreateRequestDto;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OnlineTransferActPutTransferServiceTest {
    private final TransferApi transferApi = Mockito.mock(TransferApi.class);
    private final Clock clock = Mockito.mock(Clock.class);
    private final WarehouseQueryService warehouseQueryService = Mockito.mock(WarehouseQueryService.class);

    private OnlineTransferActPutTransferService onlineTransferActPutTransferService;

    SortingCenter sortingCenter;
    MovementCourier courier;
    List<RegistryOrder> registryOrders;
    User user;
    String transportationId;

    @BeforeEach
    void init() {
        doReturn(Instant.ofEpochSecond(1587033394L)).when(clock).instant();
        doReturn(ZoneId.systemDefault()).when(clock).getZone();

        sortingCenter = TestFactory.sortingCenter(456456L);
        courier = new MovementCourier("courier-1", "Иван Иванов", "ИП Иванов", null, null);
        registryOrders = List.of(
                new RegistryOrder("order-1", "place-1", 1L, null),
                new RegistryOrder("order-1", "place-2", 1L, null),
                new RegistryOrder("order-2", "place-1", 1L, null)
        );
        user = new User(sortingCenter, 623754L, "kolya@yandex.ru", "Kolya Kolya");
        transportationId = "transportation-1";

        onlineTransferActPutTransferService = new OnlineTransferActPutTransferService(
                transferApi,
                warehouseQueryService,
                clock
        );
    }

    @Test
    @SneakyThrows
    void putInboundTransfer() {
        var inbound = new Inbound("inbound-1", null, courier, null, sortingCenter, InboundType.DS_SC, null,
                OffsetDateTime.now(clock), "comment", null, null, transportationId, false, clock.instant());

        onlineTransferActPutTransferService.putInboundTransfer(inbound, registryOrders, user);

        var expectedJson = new String(Files.readAllBytes(
                new ClassPathResource("/inbound_transfer_create_request_dto.json").getFile().toPath()
        ));
        var expected = JacksonUtil.fromString(expectedJson, TransferCreateRequestDto.class);

        Mockito.verify(transferApi).transferPut(argThat(arg -> {
            assertThat(arg)
                    .usingRecursiveComparison()
                    .ignoringFields("idempotencyKey")
                    .isEqualTo(expected);
            return true;
        }));
    }

    @Test
    @SneakyThrows
    void putInboundFromDropshipTransfer() {

        var shopWhYandexId = "shopWhYandexId";
        var shopWh = new Warehouse(shopWhYandexId, "123", null, null, false, null, "shopWh", null, null, null,
                List.of(), WarehouseType.SHOP);
        doReturn(Optional.of(shopWh)).when(warehouseQueryService).findWarehouseByYandexId(shopWhYandexId);
        doReturn(true).when(warehouseQueryService).warehouseIsDropship(ArgumentMatchers.<Warehouse>any(), ArgumentMatchers.any());

        var inbound = new Inbound("inbound-1", shopWhYandexId, null, null, sortingCenter, InboundType.DS_SC, null,
                OffsetDateTime.now(clock), "comment", null, null, transportationId, false, clock.instant());

        onlineTransferActPutTransferService.putInboundTransfer(inbound, registryOrders, user);

        var expectedJson = new String(Files.readAllBytes(
                new ClassPathResource("/inbound_from_dropship_transfer_create_request_dto.json").getFile().toPath()
        ));
        var expected = JacksonUtil.fromString(expectedJson, TransferCreateRequestDto.class);

        Mockito.verify(transferApi).transferPut(argThat(arg -> {
            assertThat(arg)
                    .usingRecursiveComparison()
                    .ignoringFields("idempotencyKey")
                    .isEqualTo(expected);
            return true;
        }));
    }

    @Test
    @SneakyThrows
    void putOutboundTransfer() {
        var outbound = new Outbound();
        outbound.setMovementCourier(courier);
        outbound.setTransportationId(transportationId);
        outbound.setExternalId("outbound-1");
        outbound.setType(OutboundType.DS_SC);
        outbound.setStatus(OutboundStatus.SHIPPED);
        outbound.setToTime(clock.instant());
        outbound.setSortingCenter(sortingCenter);

        onlineTransferActPutTransferService.putOutboundTransfer(outbound, registryOrders, user);

        var expectedJson = new String(Files.readAllBytes(
                new ClassPathResource("/outbound_transfer_create_request_dto.json").getFile().toPath()
        ));
        var expected = JacksonUtil.fromString(expectedJson, TransferCreateRequestDto.class);

        Mockito.verify(transferApi).transferPut(argThat(arg -> {
            assertThat(arg)
                    .usingRecursiveComparison()
                    .ignoringFields("idempotencyKey")
                    .isEqualTo(expected);
            return true;
        }));
    }
}
