package ru.yandex.market.sc.core.domain.order;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.cell.CellField;
import ru.yandex.market.sc.core.domain.cell.model.CellCargoType;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseProperty;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderBufferReturnPolicyTest {

    private final TestFactory testFactory;
    private final OrderBufferReturnPolicy orderBufferReturnPolicy;

    SortingCenter sortingCenter;
    Courier courier;
    Warehouse warehouse;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        courier = testFactory.storedCourier();
        testFactory.setSortingCenterProperty(sortingCenter, BUFFER_RETURNS_ENABLED, true);
        warehouse = testFactory.storedWarehouse("w1", WarehouseType.SHOP);
        testFactory.setWarehouseProperty(
                warehouse.getYandexId(),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS,
                Boolean.TRUE.toString()
        );
    }

    @Nested
    @DisplayName("КГТ/МГТ на CargoType'ах")
    class BufferCellOnCargoType {

        @Test
        void findTheBestCellForBufferReturnByCargoTypePriority1() {
            CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                    .type(CellType.BUFFER)
                    .subType(CellSubType.BUFFER_RETURNS);

            cellFieldBuilder = cellFieldBuilder.cargoType(CellCargoType.MGT);
            var cell11 = testFactory.storedCell(sortingCenter, "cell11", cellFieldBuilder.sequenceNumber(1L));
            var cell12 = testFactory.storedCell(sortingCenter, "cell12", cellFieldBuilder.sequenceNumber(2L));
            testFactory.storedCell(sortingCenter, "cell13", cellFieldBuilder.sequenceNumber(3L));

            cellFieldBuilder = cellFieldBuilder.cargoType(CellCargoType.KGT);
            var cell21 = testFactory.storedCell(sortingCenter, "cell21", cellFieldBuilder.sequenceNumber(4L));
            testFactory.storedCell(sortingCenter, "cell22", cellFieldBuilder.sequenceNumber(5L));
            testFactory.storedCell(sortingCenter, "cell23", cellFieldBuilder.sequenceNumber(6L));

            cellFieldBuilder = cellFieldBuilder.cargoType(null);
            testFactory.storedCell(sortingCenter, "cell31", cellFieldBuilder.sequenceNumber(7L));
            testFactory.storedCell(sortingCenter, "cell32", cellFieldBuilder.sequenceNumber(8L));
            testFactory.storedCell(sortingCenter, "cell33", cellFieldBuilder.sequenceNumber(9L));

            testFactory.createOrder(order(sortingCenter).externalId("o1")
                            .warehouseReturnId(warehouse.getYandexId())
                            .places(List.of("o1p1", "o1p2"))
                            .build())
                    .acceptPlaces("o1p1", "o1p2").cancel()
                    .keepPlaces(cell11.getId(), "o1p1")
                    .get();

            testFactory.createOrder(order(sortingCenter).externalId("o2")
                            .warehouseReturnId(warehouse.getYandexId())
                            .places(List.of("o2p1", "o2p2"))
                            .build())
                    .acceptPlaces("o2p1", "o2p2").cancel()
                    .keepPlaces(cell12.getId(), "o2p1", "o2p2")
                    .get();

            var theBestCell1 = orderBufferReturnPolicy.findTheBestCellByCargoType(sortingCenter,
                    CellCargoType.MGT, warehouse.getId());
            assertThat(theBestCell1).isEqualTo(cell12.getId());

            var theBestCell2 = orderBufferReturnPolicy.findTheBestCellByCargoType(sortingCenter,
                    CellCargoType.KGT, warehouse.getId());
            assertThat(theBestCell2).isEqualTo(cell21.getId());
        }

        @Test
        void findTheBestCellForBufferReturnByCargoTypePriority2() {
            CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                    .type(CellType.BUFFER)
                    .subType(CellSubType.BUFFER_RETURNS);

            cellFieldBuilder = cellFieldBuilder.cargoType(CellCargoType.MGT);
            testFactory.storedCell(sortingCenter, "cell11", cellFieldBuilder.sequenceNumber(1L));
            testFactory.storedCell(sortingCenter, "cell12", cellFieldBuilder.sequenceNumber(2L));
            var cell13 = testFactory.storedCell(sortingCenter, "cell13", cellFieldBuilder.sequenceNumber(3L));
            var cell14 = testFactory.storedCell(sortingCenter, "cell14", cellFieldBuilder.sequenceNumber(4L));
            var cell15 = testFactory.storedCell(sortingCenter, "cell15", cellFieldBuilder.sequenceNumber(5L));

            testFactory.createOrder(order(sortingCenter).externalId("o1")
                            .warehouseReturnId(warehouse.getYandexId())
                            .places(List.of("o1p1", "o1p2"))
                            .build())
                    .acceptPlaces("o1p1", "o1p2")
                    .cancel()
                    .keepPlaces(cell14.getId(), "o1p1")
                    .get();

            testFactory.createOrder(order(sortingCenter).externalId("o2")
                            .warehouseReturnId(warehouse.getYandexId())
                            .places(List.of("o2p1", "o2p2"))
                            .build())
                    .acceptPlaces("o2p1", "o2p2")
                    .cancel()
                    .keepPlaces(cell15.getId(), "o2p1", "o2p2")
                    .get();

            var theBestCell1 = orderBufferReturnPolicy.findTheBestCellByCargoType(sortingCenter,
                    CellCargoType.MGT, warehouse.getId());
            assertThat(theBestCell1).isEqualTo(cell15.getId());

            testFactory.setFullnessToCell(cell14.getId(), true);
            var theBestCell2 = orderBufferReturnPolicy.findTheBestCellByCargoType(sortingCenter,
                    CellCargoType.MGT, warehouse.getId());
            assertThat(theBestCell2).isEqualTo(cell15.getId());

            testFactory.setFullnessToCell(cell15.getId(), true);
            var theBestCell3 = orderBufferReturnPolicy.findTheBestCellByCargoType(sortingCenter,
                    CellCargoType.MGT, warehouse.getId());
            assertThat(theBestCell3).isEqualTo(cell13.getId());
        }

        @Test
        void findTheBestCellForBufferReturnByCargoTypePriority3() {
            CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                    .type(CellType.BUFFER)
                    .subType(CellSubType.BUFFER_RETURNS);

            cellFieldBuilder = cellFieldBuilder.cargoType(CellCargoType.KGT);
            var cell11 = testFactory.storedCell(sortingCenter, "cell11", cellFieldBuilder.sequenceNumber(1L));
            var cell12 = testFactory.storedCell(sortingCenter, "cell12", cellFieldBuilder.sequenceNumber(2L));
            var cell13 = testFactory.storedCell(sortingCenter, "cell13", cellFieldBuilder.sequenceNumber(3L));
            var cell14 = testFactory.storedCell(sortingCenter, "cell14", cellFieldBuilder.sequenceNumber(4L));
            var cell15 = testFactory.storedCell(sortingCenter, "cell15", cellFieldBuilder.sequenceNumber(5L));

            keepOrderToBufferReturnCell(warehouse.getYandexId(), cell11, "o11");
            keepOrderToBufferReturnCell(warehouse.getYandexId(), cell11, "o12");
            var theBestCell1 = orderBufferReturnPolicy.findTheBestCellByCargoType(sortingCenter,
                    CellCargoType.KGT, warehouse.getId());
            assertThat(theBestCell1).isEqualTo(cell11.getId());

            var w2 = createWarehouse("w2");
            keepOrderToBufferReturnCell(w2.getYandexId(), cell12, "o21");

            var w3 = createWarehouse("w3");
            keepOrderToBufferReturnCell(w3.getYandexId(), cell13, "o31");

            var w4 = createWarehouse("w4");
            keepOrderToBufferReturnCell(w4.getYandexId(), cell14, "o41");

            var w5 = createWarehouse("w5");
            keepOrderToBufferReturnCell(w5.getYandexId(), cell15, "o51");

            var w6 = createWarehouse("w6");
            var theBestCell2 = orderBufferReturnPolicy.findTheBestCellByCargoType(sortingCenter,
                    CellCargoType.KGT, w6.getId());
            assertThat(theBestCell2).isEqualTo(cell11.getId());

            testFactory.setFullnessToCell(cell13.getId(), true);
            var theBestCell3 = orderBufferReturnPolicy.findTheBestCellByCargoType(sortingCenter,
                    CellCargoType.KGT, w3.getId());
            assertThat(theBestCell3).isEqualTo(cell11.getId());
        }
    }

    private Warehouse createWarehouse(String yandexId) {
        var w = testFactory.storedWarehouse(yandexId, WarehouseType.SHOP);
        testFactory.setWarehouseProperty(String.valueOf(w.getYandexId()),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS, "true");
        return w;
    }

    private void keepOrderToBufferReturnCell(String warehouseYandexId, Cell bufferReturnCell,
                                             String orderExternalId) {
        String placeExternalId = orderExternalId + "p1";
        testFactory.createOrder(order(sortingCenter)
                        .externalId(orderExternalId)
                        .warehouseReturnId(warehouseYandexId)
                        .places(List.of(placeExternalId)).build())
                .acceptPlaces(placeExternalId).cancel()
                .keepPlaces(bufferReturnCell.getId(), placeExternalId)
                .get();
    }
}
