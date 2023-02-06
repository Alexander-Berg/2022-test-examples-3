package ru.yandex.market.tpl.core.test.factory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.mockito.Mockito;
import org.springframework.stereotype.Service;

import ru.yandex.market.tpl.core.domain.client.Client;
import ru.yandex.market.tpl.core.domain.client.ClientData;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.clientreturn.item.ClientReturnItem;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestPoint;
import ru.yandex.market.tpl.core.domain.order.Dimensions;
import ru.yandex.market.tpl.core.domain.order.DimensionsClass;

import static ru.yandex.market.tpl.core.util.TplCoreTestUtils.OBJECT_GENERATOR;

@Service
@RequiredArgsConstructor
public class TestClientReturnFactory {
    public static final DimensionsClass DEFAULT_TEST_DIMENSION_CLASS = DimensionsClass.MEDIUM_SIZE_CARGO;
    private final ClientReturnCommandService clientReturnCommandService;

    public ClientReturn buildAndSave(long dsId, LocalDateTime arriveFrom) {
        var cmd =
                OBJECT_GENERATOR.nextObject(ClientReturnCommand.CreateReturnFromClient.CreateReturnFromClientBuilder.class)
                        .client(null)
                        .items(List.of())
                        .arriveIntervalFrom(arriveFrom)
                        .deliveryServiceId(dsId)
                        .logisticRequestPointFrom(null)
                        .build();
        return clientReturnCommandService.create(cmd);
    }

    public ClientReturn buildAndSave(long dsId, LocalDateTime arriveFrom, LocalDateTime arriveTo, String phone,
                                     BigDecimal lat, BigDecimal lon) {
        return buildAndSave(dsId, arriveFrom, arriveTo, phone, lat, lon, List.of(), null);
    }

    public ClientReturn buildAndSave(long dsId, LocalDateTime arriveFrom, LocalDateTime arriveTo, String phone,
                                     BigDecimal lat, BigDecimal lon, Collection<ItemDimensions> items) {
        List<ClientReturnItem> clientReturnItems = items
                .stream()
                .map(itemDimensions -> ClientReturnItem.builder()
                        .dimensions(toEntity(itemDimensions))
                        .name(OBJECT_GENERATOR.nextObject(String.class))
                        .build())
                .collect(Collectors.toList());
        return buildAndSave(dsId, arriveFrom, arriveTo, phone, lat, lon, clientReturnItems, null);
    }

    public ClientReturn buildAndSave(long dsId,
                                     LocalDateTime arriveFrom,
                                     LocalDateTime arriveTo,
                                     String phone,
                                     BigDecimal lat,
                                     BigDecimal lon,
                                     List<ClientReturnItem> items,
                                     String externalOrderId) {
        var cmd =
                OBJECT_GENERATOR.nextObject(ClientReturnCommand.CreateReturnFromClient
                                .CreateReturnFromClientBuilder.class)
                        .client(
                                OBJECT_GENERATOR.nextObject(Client.ClientBuilder.class)
                                        .clientData(ClientData.builder()
                                                .fullName("vasya")
                                                .phone(phone)
                                                .email("vasya@yandex.ru")
                                                .build())
                                        .build())
                        .items(items)
                        .arriveIntervalFrom(arriveFrom)
                        .arriveIntervalTo(arriveTo)
                        .deliveryServiceId(dsId)
                        .logisticRequestPointFrom(
                                OBJECT_GENERATOR.nextObject(LogisticRequestPoint.LogisticRequestPointBuilder.class)
                                        .preciseLatitude(lat)
                                        .preciseLongitude(lon)
                                        .pickupPointId(null)
                                        .build()
                        )
                        .externalOrderId(externalOrderId)
                        .build();
        return clientReturnCommandService.create(cmd);
    }

    public static ClientReturn buildMock(Long clientReturnId) {
        var mocked = Mockito.mock(ClientReturn.class);
        Mockito.when(mocked.getId()).thenReturn(clientReturnId);
        return mocked;
    }

    public static ClientReturn buildMock(Long clientReturnId, String clientReturnExternalId) {
        var mocked = buildMock(clientReturnId);
        Mockito.when(mocked.getExternalReturnId()).thenReturn(clientReturnExternalId);
        return mocked;
    }

    public static ClientReturn buildMock(LogisticRequestPoint logisticRequestPoint) {
        var mocked = Mockito.mock(ClientReturn.class);
        Mockito.when(mocked.getLogisticRequestPointFrom()).thenReturn(logisticRequestPoint);
        return mocked;
    }

    public static ClientReturn buildMock(List<BigDecimal> itemVolumeInCubicList) {
        var clientReturnItems = itemVolumeInCubicList
                .stream()
                .map(itemVolumeInCubic -> {
                    var mockedDimensions = Mockito.mock(Dimensions.class);
                    Mockito.when(mockedDimensions.getDimensionsClass()).thenReturn(DEFAULT_TEST_DIMENSION_CLASS);
                    Mockito.when(mockedDimensions.calculateVolumeInCubicMeters()).thenReturn(itemVolumeInCubic);
                    return ClientReturnItem.builder()
                            .dimensions(mockedDimensions)
                            .build();
                })
                .collect(Collectors.toList());
        var mocked = Mockito.mock(ClientReturn.class);
        Mockito.when(mocked.getItems()).
                thenReturn(clientReturnItems);
        return mocked;
    }

    private Dimensions toEntity(ItemDimensions dto) {
        return new Dimensions(
                dto.getWeight(),
                dto.getLengthMeters() * 100,
                dto.getWidthMeters() * 100,
                dto.getHeightMeters() * 100
        );
    }

    @Getter
    @Value(staticConstructor = "of")
    public static class ItemDimensions {
        @NonNull
        BigDecimal weight;
        @NonNull
        Integer lengthMeters;
        @NonNull
        Integer widthMeters;
        @NonNull
        Integer heightMeters;
    }
}
