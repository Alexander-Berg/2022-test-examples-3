package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InboundTable;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.AnomalyConsolidationCell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.BbxdDroppingCell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.BbxdShippingCell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.BbxdSortingCell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.CarrierPriority;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.Cell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ConsolidationCell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ConveyorLocation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ConveyorTransit;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.DAMAGE;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.DOOR;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.DoorOut;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.DroppingCell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.LocationType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.PackingTable;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.TurnoverReplBuffer;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.PalleteStorageCell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.PickingCell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.PutawayZone;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.PutawayZoneType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ReceiptTable;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ShippingStandardCell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ShippingWithdrawalCell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SortStationModeDto;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SorterExit;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SortingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.VGH;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.WithdrawalReplBuffer;
import ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode;

import java.util.ArrayList;
import java.util.Set;

import javax.annotation.Nullable;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

public class Location {
    private static final DatacreatorClient dataCreator = new DatacreatorClient();

    //Пытается создать один стол 2 раза при одновременном к нему обращении
    //поэтому ставлю synchronized
    public synchronized InboundTable createInboundTable() {
        return createInboundTable("DOCK");
    }

    @Step("Создание стола приемки с ячейками STAGE, STAGEOBM, DOOR и DAMAGE")
    public synchronized InboundTable createInboundTable(String zone) {

        ArrayList<Cell> inboundCells = new ArrayList<>();

        inboundCells.add(new VGH(zone));
        inboundCells.add(new ReceiptTable(zone));
        inboundCells.add(new DOOR(zone));
        inboundCells.add(new DAMAGE(zone));

        ValidatableResponse response = dataCreator.createLocations(inboundCells);

        String suffix = getSuffix(response);
        validateCreatedLocations(inboundCells, response, suffix);

        return new InboundTable(
                "STAGE" + suffix,
                "STAGEOBM" + suffix,
                "DOOR" + suffix,
                "DAMAGE" + suffix
        );
    }

    @Step("Удаление стола приемки с ячейками STAGE, STAGEOBM, DOOR и DAMAGE")
    public void deleteInboundTable(InboundTable inboundTable) {
        dataCreator.deleteLoc(inboundTable.getStageCell());
        dataCreator.deleteLoc(inboundTable.getObmCell());
        dataCreator.deleteLoc(inboundTable.getDoorCell());
        dataCreator.deleteLoc(inboundTable.getDamageCall());
    }

    @Step("Получаем суффикс для созданных локаций")
    private String getSuffix(ValidatableResponse response) {
        return response
                .body("suffix", matchesPattern("..?"))
                .extract()
                .jsonPath()
                .getString("suffix");
    }

    @Step("Проверяем, что создались нужные локации с одинаковым суффиксом")
    private void validateCreatedLocations(ArrayList<Cell> cells, ValidatableResponse response, String suffix) {
        cells.forEach(cell ->
                response.body(
                        "locations." + cell.getLocationType(),
                        is(cell.getPrefix() + suffix))
        );
    }

    @Step("Создаем участок")
    public String createArea() {
        return dataCreator.createArea();
    }

    public String createPutawayZone(String area) {
        return createPutawayZone(area, null);
    }

    @Step("Создаем зону")
    public String createPutawayZone(String area, @Nullable String building) {
        return dataCreator.createPutawayZone(PutawayZone.builder()
                .areaKey(area)
                .building(building)
                .build()
        );
    }

    @Step("Создаем зону с определенным типом")
    public String createPutawayZoneWithType(String area, PutawayZoneType type) {
        return dataCreator.createPutawayZone(PutawayZone.builder()
                .areaKey(area)
                .type(type)
                .build()
        );
    }

    @Step("Создаем ячейку отбора")
    public String createPickingCell(String putawayZone) {
        return dataCreator.createLoc(new PickingCell(putawayZone));
    }

    @Step("Создаем ячейку отбора")
    public String createPickingCell(String putawayZone, boolean loseId) {
        return dataCreator.createLoc(new PickingCell(putawayZone, loseId));
    }

    @Step("Создаем ячейку консолидации")
    public String createConsolidationCell(String putawayZone) {
        return dataCreator.createLoc(new ConsolidationCell(putawayZone));
    }

    @Step("Создаем ячейку консолидации синглов")
    public String createSinglesConsolidationCell(String putawayZone) {
        return dataCreator.createSinglesConsLoc(new ConsolidationCell(putawayZone));
    }

    @Step("Создаем ячейку консолидации нонсортовых изъятий")
    public String createWithdrawalOversizeConsolidationCell(String putawayZone) {
        return dataCreator.createWithdrawalOversizeConsLoc(new ConsolidationCell(putawayZone));
    }

    @Step("Создаем стол упаковки")
    public String createPackingTable(String putawayZone) {
        return dataCreator.createLoc(new PackingTable(putawayZone));
    }

    @Step("Создаем стол упаковки")
    public String createConveyorTransit(String putawayZone) {
        return dataCreator.createLoc(new ConveyorTransit(putawayZone));
    }

    @Step("Создаем стол конвейерной приемки")
    public String createConveyorLocation(String putawayZone, LocationType locationType, String transporterLoc) {
        return dataCreator.createLoc(new ConveyorLocation(putawayZone, locationType, transporterLoc));
    }

    @Step("Создаем сортировочную станцию")
    public SortingStation createSortingStation(String putawayZone) {
        SortingStation sortingStation = SortingStation.builder()
                .zone(putawayZone)
                .build();
        return dataCreator.createSortingStation(sortingStation);
    }

    @Step("Задаем приоритет направления")
    public void setCarrierPriority(String carrierCode, int priority) {
        CarrierPriority carrierPriority = CarrierPriority.builder()
                .carrierCode(carrierCode)
                .priority(priority)
                .build();
        dataCreator.setCarrierPriority(carrierPriority);
    }

    @Step("Создаем сортировочную станцию c нестандартным префиксом {prefix}")
    public SortingStation createSortingStation(String prefix, String putawayZone) {
        SortingStation sortingStation = SortingStation.builder()
                .prefix(prefix)
                .zone(putawayZone)
                .build();
        return dataCreator.createSortingStation(sortingStation);
    }

    @Step("Создаем выход сортировочного конвейера")
    public SorterExit createSorterExit(String putawayZone, boolean isAlternateExit, boolean isErrorExit) {
        SorterExit sorterExit = SorterExit.builder()
                .zone(putawayZone)
                .isAlternateExit(isAlternateExit)
                .isErrorExit(isErrorExit)
                .build();
        return dataCreator.createSorterExit(sorterExit);
    }

    @Step("Создаем выход сортировочного конвейера с префиксом {prefix}")
    public SorterExit createSorterExit(
            String prefix,
            String putawayZone,
            boolean isAlternateExit,
            boolean isErrorExit
    ) {
        SorterExit sorterExit = SorterExit.builder()
                .prefix(prefix)
                .zone(putawayZone)
                .isAlternateExit(isAlternateExit)
                .isErrorExit(isErrorExit)
                .build();
        return dataCreator.createSorterExit(sorterExit);
    }

    @Step("Создаем ячейку паллетного хранения")
    public String createPalletStorageCell(String putawayZone) {
        return dataCreator.createLoc(new PalleteStorageCell(putawayZone));
    }

    @Step("Создаем буферную ячейку для пополнения под оборачиваемость")
    public String createTurnoverReplBuffer(String putawayZone) {
        return dataCreator.createLoc(new TurnoverReplBuffer(putawayZone));
    }

    @Step("Создаем буферную ячейку для пополнения под изъятия")
    public String createWithdrawalReplBuffer(String putawayZone) {
        return dataCreator.createLoc(new WithdrawalReplBuffer(putawayZone));
    }

    @Step("Создаем ячейку консолидации аномалий")
    public String createAnomalyConsolidationCell(String putawayZone) {
        return dataCreator.createLoc(new AnomalyConsolidationCell(putawayZone));
    }

    @Step("Изменяем режим работы сортировочной станции {station} на {mode}")
    public void updateSortStationMode(String station, AutoStartSortingStationMode mode) {
        if (station != null) {
            SortStationModeDto dto = SortStationModeDto.builder()
                    .station(station)
                    .mode(mode.getCode())
                    .build();
            dataCreator.updateSortingStationMode(dto);
        }
    }

    @Step("Удаляем ячейку {cell}")
    public void deleteCell(String cell) {
        if (cell != null) dataCreator.deleteLoc(cell);
    }

    @Step("Удаляем сортировочную станцию {station}")
    public void deleteSotingStation(String station) {
        if (station != null) dataCreator.deleteSortingStation(station);
    }

    @Step("Удаляем выход сортировщика {sorterExitKey}")
    public void deleteSorterExit(String sorterExitKey) {
        if (sorterExitKey != null) dataCreator.deleteSorterExit(sorterExitKey);
    }

    @Step("Удаляем зону {zone}")
    public void deletePutawayZone(String zone) {
        if (zone != null) dataCreator.deletePutawayZone(zone);
    }

    @Step("Удаляем задания на инвентаризацию для {zones}")
    public void deletePickToInventoryTasks(Set<String> zones) {
        dataCreator.deletePickToInventoryTaskByPutAwayZones(zones);
    }

    @Step("Удаляем участок {area}")
    public void deleteArea(String area) {
        if (area != null) dataCreator.deleteArea(area);
    }


    @Step("Создаем ворота")
    public String createShippingDoor(String zone) {
        return dataCreator.createLoc(new DoorOut(zone));
    }

    @Step("Создаем ячейку для размещения дропок на отгрузке (стандатрная)")
    public String createShippingStandardCell(String zone) {
        return dataCreator.createLoc(new ShippingStandardCell(zone));
    }

    @Step("Создаем ячейку для размещения дропок на отгрузке (BBXD)")
    public String createShippingBbxdCell(String zone) {
        return dataCreator.createLoc(new BbxdShippingCell(zone));
    }

    @Step("Создаем ячейку для размещения дропок на отгрузке (изъятие)")
    public String createShippingWithdrawalCell(String zone) {
        return dataCreator.createLoc(new ShippingWithdrawalCell(zone));
    }

    @Step("Создаем здание")
    public String createBuilding() {
        return dataCreator.createBuilding().extract().asString();
    }

    @Step("Удаляем здание")
    public void deleteBuilding(String building) {
        dataCreator.deleteBuilding(building);
    }

    @Step("Удалить здание у заказа")
    public void deleteBuildingInOrder(String order){
        if (order != null){
            dataCreator.deleteBuildingInOrder(order);
        }
    }
    @Step("Очистить устаревшие суффиксы")
    public void deleteExpiredSuffixes(){
        dataCreator.deleteExpiredSuffixes();
    }

    @Step("Создаем ячейку дропинга")
    public String createDroppingCell(String zone) {
        return dataCreator.createLoc(new DroppingCell(zone));
    }

    @Step("Создаем ячейку дропинга BBXD")
    public String createBbxdDroppingCell(String zone) {
        return dataCreator.createLoc(new BbxdDroppingCell(zone));
    }

    @Step("Создаем ячейку сортировки BBXD")
    public String createBbxdSortingCell(String zone) {
        return dataCreator.createLoc(new BbxdSortingCell(zone));
    }
}
