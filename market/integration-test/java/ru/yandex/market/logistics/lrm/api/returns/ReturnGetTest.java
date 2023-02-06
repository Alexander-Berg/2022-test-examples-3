package ru.yandex.market.logistics.lrm.api.returns;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi.GetReturnOper;
import ru.yandex.market.logistics.lrm.client.model.FulfilmentReceivedBoxInfo;
import ru.yandex.market.logistics.lrm.client.model.GetReturnBox;
import ru.yandex.market.logistics.lrm.client.model.GetReturnItem;
import ru.yandex.market.logistics.lrm.client.model.GetReturnResponse;
import ru.yandex.market.logistics.lrm.client.model.NotFoundError;
import ru.yandex.market.logistics.lrm.client.model.OptionalRequestPart;
import ru.yandex.market.logistics.lrm.client.model.ReceivedBoxItem;
import ru.yandex.market.logistics.lrm.client.model.ResourceType;
import ru.yandex.market.logistics.lrm.client.model.ReturnSource;
import ru.yandex.market.logistics.lrm.client.model.ReturnStatus;
import ru.yandex.market.logistics.lrm.client.model.ShipmentRecipientType;
import ru.yandex.market.logistics.lrm.client.model.UnitCountType;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.model.FulfilmentReceivedBoxMeta;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

@ParametersAreNonnullByDefault
@DisplayName("Получение возврата по идентификатору")
@DatabaseSetup("/database/api/returns/get/before/common.xml")
class ReturnGetTest extends AbstractIntegrationYdbTest {

    private static final Instant FIXED_TIME = Instant.parse("2021-11-11T11:11:11.00Z");

    @Autowired
    private EntityMetaTableDescription entityMetaTable;

    @Autowired
    private EntityMetaService entityMetaService;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTable);
    }

    @Test
    @DisplayName("Возврат не найден")
    void notFound() {
        NotFoundError error = getReturn(2)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error.getIds()).containsExactly(2L);
        softly.assertThat(error.getResourceType()).isEqualTo(ResourceType.RETURN);
        softly.assertThat(error.getMessage()).isNotNull();
    }

    @Test
    @DisplayName("Успех")
    void success() {
        GetReturnResponse response = getReturn(1)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .usingRecursiveComparison()
            .isEqualTo(expectedReturnResponseWithoutOptionalParts());
    }

    @Test
    @DisplayName("Успех (есть активная контрольная точка)")
    @DatabaseSetup(
        value = "/database/api/returns/get/before/active_control_point.xml",
        type = DatabaseOperation.INSERT
    )
    void successWithActiveControlPoint() {
        GetReturnResponse response = getReturn(1)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        GetReturnResponse expectedResponse = expectedReturnResponseWithoutOptionalParts();
        Instant expectedBoxStorageExpiresAt = Instant.parse("2022-06-07T08:09:00Z");
        expectedResponse.getBoxes().forEach(box -> box.setStoragePeriodExpiresAt(expectedBoxStorageExpiresAt));

        softly.assertThat(response)
            .usingRecursiveComparison()
            .isEqualTo(expectedResponse);
    }

    @Test
    @DatabaseSetup(
        value = "/database/api/returns/get/before/boxes_statuses_for_fulfilment_received.xml",
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Получение возврата с опцией полученных коробок на складе, возврат в неподходящем статусе")
    void withBoxReceivedItemsAndIncorrectReturnStatus() {
        setUpBoxesMetaData();
        GetReturnResponse response = getReturn(1)
            .optionalPartsQuery(OptionalRequestPart.RECEIVED_BOX_ITEMS)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .usingRecursiveComparison()
            .isEqualTo(expectedReturnResponseWithoutOptionalParts());
    }

    @Test
    @DatabaseSetup(
        value = {
            "/database/api/returns/get/before/boxes_statuses_for_fulfilment_received.xml",
            "/database/api/returns/get/before/return_status_for_fulfilment_received.xml",
        },
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Получение возврата с опцией полученных коробок на складе")
    void withFulfilmentBoxesReceived() {
        setUpBoxesMetaData();
        GetReturnResponse response = getReturn(1)
            .optionalPartsQuery(OptionalRequestPart.RECEIVED_BOX_ITEMS)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .usingRecursiveComparison()
            .isEqualTo(expectedReturnResponseWithReceivedBoxes());
    }

    private void setUpBoxesMetaData() {
        entityMetaService.save(new DetachedTypedEntity(EntityType.RETURN_BOX, 1L), fulfilmentBoxReceived(1L));
        entityMetaService.save(new DetachedTypedEntity(EntityType.RETURN_BOX, 2L), fulfilmentBoxReceived(2L));
    }

    @Nonnull
    private FulfilmentReceivedBoxMeta fulfilmentBoxReceived(long boxId) {
        return FulfilmentReceivedBoxMeta.builder()
            .boxExternalId("box-" + boxId)
            .ffRequestId(123L)
            .deliveryServicePartnerId(111L)
            .warehousePartnerId(222L)
            .timestamp(FIXED_TIME)
            .items(List.of(
                FulfilmentReceivedBoxMeta.ReceivedBoxItemMeta.builder()
                    .supplierId(1L)
                    .vendorCode("vendor-code")
                    .stock(FulfilmentReceivedBoxMeta.UnitCountType.EXPIRED)
                    .attributes(List.of("attrs"))
                    .instances(Map.of("UIT", "123456789"))
                    .build()
            ))
            .build();
    }

    @Nonnull
    private GetReturnOper getReturn(long returnId) {
        return apiClient.returns().getReturn().returnIdPath(returnId);
    }

    @Nonnull
    private GetReturnResponse expectedReturnResponseWithReceivedBoxes() {
        GetReturnResponse response = expectedReturnResponseWithoutOptionalParts()
            .status(ReturnStatus.FULFILMENT_RECEIVED);
        for (GetReturnBox box : response.getBoxes()) {
            box.fulfilmentReceivedInfo(receivedBoxInfo(box.getExternalId()));
        }

        return response;
    }

    @Nonnull
    private FulfilmentReceivedBoxInfo receivedBoxInfo(String boxExternalId) {
        return new FulfilmentReceivedBoxInfo()
            .boxExternalId(boxExternalId)
            .ffRequestId(123L)
            .deliveryServicePartnerId(111L)
            .warehousePartnerId(222L)
            .timestamp(FIXED_TIME)
            .items(List.of(
                new ReceivedBoxItem()
                    .supplierId(1L)
                    .vendorCode("vendor-code")
                    .stock(UnitCountType.EXPIRED)
                    .attributes(List.of("attrs"))
                    .instances(Map.of("UIT", "123456789"))
            ));
    }

    @Nonnull
    private GetReturnResponse expectedReturnResponseWithoutOptionalParts() {
        return new GetReturnResponse()
            .source(ReturnSource.PICKUP_POINT)
            .orderExternalId("987654")
            .externalId("654987")
            .status(ReturnStatus.IN_TRANSIT)
            .boxes(List.of(
                new GetReturnBox()
                    .externalId("box-1")
                    .recipientType(ShipmentRecipientType.SHOP)
                    .destinationLogisticPointId(200L),
                new GetReturnBox()
                    .externalId("box-2")
                    .recipientType(ShipmentRecipientType.SHOP)
                    .destinationLogisticPointId(300L)
            ))
            .items(List.of(
                new GetReturnItem()
                    .supplierId(1000L)
                    .vendorCode("item-1")
                    .instances(Map.of()),
                new GetReturnItem()
                    .supplierId(2000L)
                    .vendorCode("item-2")
                    .instances(Map.of("CIS", "654321"))
            ));
    }
}
