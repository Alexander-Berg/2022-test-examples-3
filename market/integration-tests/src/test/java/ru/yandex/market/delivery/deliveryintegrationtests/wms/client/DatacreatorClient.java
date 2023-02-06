package ru.yandex.market.delivery.deliveryintegrationtests.wms.client;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.request.DatacreatorGetItemRequest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.CarrierPriority;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.Cell;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.CreateLocationRequest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.PutawayZone;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ReceivingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SortStationModeDto;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SorterExit;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.SortingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ZoneConfig;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.permission.PermissionRequest;
import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.spring.dao.entity.TaskDetail;

import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.hamcrest.Matchers.notNullValue;

@Resource.Classpath("wms/infor.properties")
public class DatacreatorClient {

    private static final int REQUEST_TIMEOUT = 60000;

    private static final RestAssuredConfig CONFIG = RestAssured.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", REQUEST_TIMEOUT)
                    .setParam("http.socket.timeout", REQUEST_TIMEOUT)
                    .setParam("http.connection-manager.timeout", REQUEST_TIMEOUT)
            );

    private final Logger log = LoggerFactory.getLogger(WrapInfor.class);

    @Property("infor.host")
    private String inforHost;

    @Property("infor.token")
    private String inforToken;

    @Property("infor.username")
    private String username;

    @Property("infor.password")
    private String password;

    public DatacreatorClient() {
        PropertyLoader.newInstance().populate(this);
    }

    private ValidatableResponse baseGetRequest(String path) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssured
                .given()
                .config(CONFIG
                        .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .contentType(ContentType.TEXT)
                .header("Content-Type", "text/json")
                .header("X-Token", inforToken)
                .get(path)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    private ValidatableResponse basePostRequest(Object reqBody, String path) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        ValidatableResponse response = RestAssured
                .given()
                .config(CONFIG
                        .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                //Иногда падало на аллюрном аттаче.
                //Надеюсь доп логирование поможет если что разобраться.
                .filter(new RequestLoggingFilter())
                .filter(new ResponseLoggingFilter())
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/json")
                .header("X-Token", inforToken)
                .body(reqBody)
                .when()
                .post(path).then();

        response.statusCode(HttpStatus.SC_OK);

        return response;
    }

    private ValidatableResponse basePutRequest(Object reqBody, String path) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        ValidatableResponse response = RestAssured
                .given()
                .config(CONFIG
                        .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/json")
                .header("X-Token", inforToken)
                .body(reqBody)
                .when()
                .put(path).then();

        response.statusCode(HttpStatus.SC_OK);

        return response;
    }

    private ValidatableResponse baseDeleteRequest(String path) {
        return baseDeleteRequest(null, path);
    }

    private ValidatableResponse baseDeleteRequest(Object body, String path) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RequestSpecification specification = RestAssured
                .given()
                .config(CONFIG
                        .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .filter(new AllureRestAssured())
                .baseUri(inforHost)
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/json")
                .header("X-Token", inforToken);
        if (body != null) {
            specification = specification.body(body);
        }

        return specification.delete(path)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public ValidatableResponse getContainer(String containerType) {
        log.info("Calling Datacreator /label/container/");

        return Retrier.clientRetry(() ->
                basePostRequest("", "/datacreator/label/container/" + containerType)
        );
    }

    public ValidatableResponse getDrop() {
        log.info("Calling Datacreator /datacreator/label/drop/");

        return Retrier.clientRetry(() ->
                basePostRequest("", "/datacreator/label/drop/")
        );
    }

    public ValidatableResponse getDropWithCarrier(String carrierCode) {
        log.info("Calling Datacreator /datacreator/label/drop-with-carrier/%s".formatted(carrierCode));

        return Retrier.clientRetry(() ->
                basePostRequest("", "/datacreator/label/drop-with-carrier/" + carrierCode)
        );
    }

    public ValidatableResponse getParcel() {
        log.info("Calling Datacreator /datacreator/parcel/");

        return Retrier.clientRetry(() ->
                basePostRequest("", "/datacreator/label/parcel/")
        );
    }

    public ValidatableResponse getUit(String loc, String lot) {
        log.info("Calling Datacreator getUit");

        return Retrier.clientRetry(() ->
                baseGetRequest("/datacreator/uit?loc=" + loc + "&lot=" + lot)
        );
    }

    public ValidatableResponse getItem(DatacreatorGetItemRequest reqBody) {
        log.info("Calling Datacreator getItem");

        ValidatableResponse resp = Retrier.clientRetry(() -> {
            return basePostRequest(reqBody, "/datacreator/sku/getItem");
        });

        return resp;
    }

    public Item getShippedItem() {
        log.info("Calling Datacreator getShippedItem");

        ValidatableResponse resp = Retrier.clientRetry(() -> {
            return basePostRequest("", "/datacreator/sku/getShippedItem?getCis=false");
        });

        long vendorId = resp.extract().jsonPath().getLong("storerKey");
        String article = resp.extract().jsonPath().getString("manufacturerSku");
        String serialNumber = resp.extract().jsonPath().getString("serialNumber");

        log.info("Item created: vendorId = {}, article = {}, serialNumber = {}", vendorId, article, serialNumber);

        return Item.builder()
                .vendorId(vendorId)
                .article(article)
                .name("ReturnsTest")
                .serialNumber(serialNumber)
                .build();
    }

    public Item getShippedItemWithCis() {
        log.info("Calling Datacreator getShippedItemWithCis");

        ValidatableResponse resp =
                Retrier.clientRetry(() -> basePostRequest("", "/datacreator/sku/getShippedItemWithCis"));

        long vendorId = resp.extract().jsonPath().getLong("storerKey");
        String article = resp.extract().jsonPath().getString("manufacturerSku");
        String serialNumber = resp.extract().jsonPath().getString("serialNumber");
        String cis = resp.extract().jsonPath().getString("cis");
        String sku = resp.extract().jsonPath().getString("sku");

        log.info("Item created: vendorId = {}, article = {}, serialNumber = {}, CIS = {}", vendorId, article,
                serialNumber, cis);

        return Item.builder()
                .vendorId(vendorId)
                .article(article)
                .sku(sku)
                .name("ReturnsCisTest")
                .serialNumber(serialNumber)
                .instances(Map.of("CIS", cis))
                .build();
    }

    /**
     * Получение имени несуществующей ячеки с заданным префиксом
     */
    public String getNonexistentLocation(String prefix) {
        log.info("Calling Datacreator " +
                "/datacreator/location/getNonexistentLocation/" + prefix);

        return Retrier.clientRetry(() -> baseGetRequest(
                "/datacreator/location/getNonexistentLocation/" +
                        prefix
        ).extract()
                .body()
                .asString());
    }

    public String createLoc(CreateLocationRequest reqBody) throws Exception {
        log.info("Calling Datacreator /datacreator/location/loc");

        int weirdLimit = 10;
        if (reqBody.getCode().length() > weirdLimit) {
            throw new Exception("Location CODE max length is " + weirdLimit);
        }
        if (reqBody.getZone().length() > weirdLimit) {
            throw new Exception("Location ZONE max length is " + weirdLimit);
        }

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(reqBody, "/datacreator/location/loc")
        );

        return response.extract().body().asString();
    }


    public TaskDetail createTaskDetail(TaskDetail task) {
        String path = "/datacreator/taskdetail/createByDao";
        log.info("Calling Datacreator " + path);
        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(task, path)
        );
        return response.extract().body().as(TaskDetail.class);
    }

    public ValidatableResponse deleteBbxdTask(String username) {
        String path = "/datacreator/taskdetail/delete-bbxd/" + username;
        log.info("Calling Datacreator " + path);
        return Retrier.clientRetry(() ->
                baseDeleteRequest("", path)
        );
    }

    /**
     * Создание незанятых ячеек по префиксу
     * ячейки приемки, обмера, брака
     *
     * @param cells
     * @return cellSuffix
     */
    public ValidatableResponse createLocations(ArrayList<Cell> cells) {
        log.info("Calling Datacreator " +
                "/datacreator/location/locs");

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(cells, "/datacreator/location/locs")
        );

        return response;
    }

    /**
     * Удаление ячейки с перемещением всех товаров из неё в LOST
     *
     * @param loc
     * @return
     */
    public ValidatableResponse deleteLoc(String loc) {
        log.info("Calling Datacreator delete " +
                "/datacreator/location/loc/" + loc);

        return baseDeleteRequest(
                "/datacreator/location/loc/" + loc
        );
    }

    public String createArea() {
        return createArea("ATA");
    }

    /**
     * Создание участка с заданным префиксом
     *
     * @param prefix
     * @return
     */
    public String createArea(String prefix) {
        log.info("Calling Datacreator create" +
                "datacreator/area");
        Map<String, String> reqBody = Map.of("prefix", prefix);

        ValidatableResponse response = Retrier.clientRetry(() -> basePostRequest(reqBody, "datacreator/area"));

        String areaKey = response.extract().asString();

        log.info("Created area " + areaKey);

        return areaKey;
    }

    /**
     * Удаление участка по названию участка
     *
     * @param areaKey
     * @return
     */
    public ValidatableResponse deleteArea(String areaKey) {
        log.info("Calling Datacreator delete " +
                "datacreator/area/" + areaKey);

        return baseDeleteRequest("datacreator/area/" + areaKey);
    }

    /**
     * Создание зоны на определенном участке
     *
     * @param zone
     * @return
     */
    public String createPutawayZone(PutawayZone zone) {
        log.info("Calling Datacreator create " +
                "datacreator/putaway-zone");

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(zone, "datacreator/putaway-zone")
        );

        String zoneKey = response.extract().asString();

        log.info("Created putaway zone " + zoneKey);

        return zoneKey;
    }

    /**
     * Удаление зоны по названию
     *
     * @param zone
     * @return
     */
    public ValidatableResponse deletePutawayZone(String zone) {
        log.info("Calling Datacreator delete " +
                "datacreator/putaway-zone/" + zone);

        return baseDeleteRequest("datacreator/putaway-zone/" + zone);
    }

    /**
     * Создание одной ячейки любого типа
     *
     * @param cell
     * @return
     */
    public String createLoc(Cell cell) {
        log.info("Calling Datacreator create " +
                "datacreator/location/loc");

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(cell, "datacreator/location/loc")
        );

        String locationKey = response.extract().asString();

        log.info("Created location " + locationKey);

        return locationKey;
    }

    /**
     * Создание одной ячейки консолидации синглов
     *
     * @param cell
     * @return
     */
    public String createSinglesConsLoc(Cell cell) {
        log.info("Calling Datacreator create " +
                "datacreator/location/singlesConsLoc");

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(cell, "datacreator/location/singlesConsLoc")
        );

        String locationKey = response.extract().asString();

        log.info("Created singles consolidation location " + locationKey);

        return locationKey;
    }

    /**
     * Создание одной ячейки консолидации нонсортовых изъятий
     *
     * @param cell
     * @return
     */
    public String createWithdrawalOversizeConsLoc(Cell cell) {
        log.info("Calling Datacreator create " +
                "datacreator/location/withdrawalOversizeConsLoc");

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(cell, "datacreator/location/withdrawalOversizeConsLoc")
        );

        String locationKey = response.extract().asString();

        log.info("Created Withdrawal Oversize consolidation location " + locationKey);

        return locationKey;
    }

    /**
     * Создание сортировочной станции в определенной зоне выбранного участка
     *
     * @param sortingStation
     * @return
     */
    public SortingStation createSortingStation(SortingStation sortingStation) {
        log.info("Calling Datacreator create " +
                "datacreator/location/sort-station");

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(sortingStation, "datacreator/location/sort-station")
        );

        String sortingStationKey = response
                .extract()
                .jsonPath()
                .getString("station");

        List<String> sortingCells = response
                .extract()
                .jsonPath()
                .getList("sortLocations");

        log.info("Created sorting station " + sortingStationKey);

        return SortingStation.builder()
                .station(sortingStationKey)
                .sortLocations(sortingCells)
                .build();
    }

    public ValidatableResponse updateSortingStationMode(SortStationModeDto dto) {
        log.info("Calling Datacreator update " +
                "datacreator/location/sort-station/updateMode");

        return basePutRequest(dto, "datacreator/location/sort-station/updateMode");
    }

    public ValidatableResponse deleteSortingStation(String station) {
        log.info("Calling Datacreator delete " +
                "datacreator/location/sortingStation/" + station);
        return baseDeleteRequest("datacreator/location/sort-station/" + station);
    }

    public ValidatableResponse deletePickToInventoryTaskByPutAwayZones(Set<String> zones) {
        log.info("Calling Datacreator delete " +
                "datacreator/inventorization/pick-to-inventory-by-zones");
        return baseDeleteRequest(zones, "datacreator/inventorization/pick-to-inventory-by-zones");
    }

    /**
     * Создание разрешений на выполнение заданий на участке
     * то что в wms -> конфикурация -> производительность -> пользователь
     *
     * @param reqBody
     * @return
     */
    public String createPermission(PermissionRequest reqBody) {
        log.info("Calling Datacreator create " +
                "datacreator/task-manager-user-detail");

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(reqBody, "datacreator/task-manager-user-detail")
        );

        return response.extract().asString();
    }

    /**
     * Удаление разрешений на выполнение заданий на участке
     *
     * @param permissionSK
     * @return
     */
    public ValidatableResponse deletePermission(String permissionSK) {
        log.info("Calling Datacreator delete " +
                "datacreator/task-manager-user-detail/" + permissionSK);

        return Retrier.clientRetry(() ->
                baseDeleteRequest("datacreator/task-manager-user-detail/" + permissionSK)
        );
    }

    /**
     * Создание выхода сортировщика в определенной зоне выбранного участка
     *
     * @param sorterExit
     * @return
     */
    public SorterExit createSorterExit(SorterExit sorterExit) {
        log.info("Calling Datacreator create " +
                "datacreator/location/sorter-exit");

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(sorterExit, "datacreator/location/sorter-exit")
        );

        String sorterExitKey = response
                .extract()
                .jsonPath()
                .getString("sorterExit");

        log.info("Created sorted exit " + sorterExit);

        return SorterExit.builder()
                .sorterExitKey(sorterExitKey)
                .build();
    }

    public ValidatableResponse deleteSorterExit(String sorterExitKey) {
        log.info("Calling Datacreator delete " +
                "datacreator/location/sorter-exit/" + sorterExitKey);
        return Retrier.clientRetry(() ->
                baseDeleteRequest("datacreator/location/sorter-exit/" + sorterExitKey)
        );
    }

    /**
     * Получение незанятого пользователя
     */
    public ValidatableResponse usersLock(String loginPrefix, Integer lockDuration) {
        log.info("Calling Datacreator " +
                "/datacreator/users/lock");

        String reqString = String.format("/datacreator/users/lock?loginPrefix=%s&lockDuration=%s",
                loginPrefix,
                lockDuration
        );
        ValidatableResponse response = Retrier.clientRetry(() ->
                baseGetRequest(reqString)
        );

        return response;
    }

    /**
     * Снимаем лок с ранее занятого нами пользователя
     */
    public void usersUnlock(String username) {
        log.info("Calling Datacreator " +
                "/datacreator/users/" + username + "/unlock");

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest("", "/datacreator/users/" + username + "/unlock")
        );
    }

    public void createZoneConfig(ZoneConfig zoneConfig) {
        String url = "datacreator/zone-config";
        log.info("Calling Datacreator create " + url);

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(zoneConfig, url)
        );
        log.info("Created ZoneConfig for putaway zone " + zoneConfig.getZone());
    }

    public void deleteZoneConfig(String zone) {
        String url = "datacreator/zone-config";
        log.info("Calling Datacreator delete " + url);

        Retrier.clientRetry(() ->
                baseDeleteRequest(Map.of("zone", zone), url)
        );
        log.info("Deleted ZoneConfig for putaway zone " + zone);
    }

    public void createReceivingStation(ReceivingStation receivingStation) {
        String url = "datacreator/receiving-station";
        log.info("Calling Datacreator create " + url);

        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(receivingStation, url)
        );
        log.info("Created ReceivingStation " + receivingStation.getLoc());
    }

    public void setCarrierPriority(CarrierPriority carrierPriority) {
        String url = "datacreator/carrier/set-priority";
        log.info("Calling Datacreator create " + url);

        Retrier.clientRetry(() ->
                basePostRequest(carrierPriority, url)
        );
    }

    public void deleteReceivingStation(String receivingStationLoc) {
        String url = "datacreator/receiving-station";
        log.info("Calling Datacreator delete " + url);

        Retrier.clientRetry(() ->
                baseDeleteRequest(Map.of("loc", receivingStationLoc), url)
        );
        log.info("Deleted ReceivingStation " + receivingStationLoc);
    }

    /**
     * Получаем текущий операционный день
     *
     * @return
     */
    public LocalDate getCurrentOperationalDate() {
        log.info("Calling Datacreator GET " +
                "datacreator/settings/currentOperationalDate");

        ValidatableResponse response = Retrier.clientRetry(() ->
                baseGetRequest("datacreator/settings/currentOperationalDate")
        );

        return response.extract().as(LocalDate.class);
    }

    /**
     * Получить знаение настройки
     *
     * @param setting Настройка
     * @return Значение настройки
     */
    public String getSettingValue(String setting) {
        log.info("Calling Datacreator GET datacreator/settings/{}", setting);

        ValidatableResponse response = Retrier.clientRetry(() ->
                baseGetRequest("datacreator/settings/" + setting)
        );

        return response.extract().asString();
    }

    /**
     *
     * Создаем здание
     */
    public ValidatableResponse createBuilding(){
        log.info("Calling DataCreator post " +
                "/datacreator/building");
        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest("" ,"/datacreator/building/")
        );

        return response;
    }

    /**
     *
     * Удаляем здание
     */
    public void deleteBuilding(String building){
        log.info("Calling DataCreator delete " +
                "/datacreator/building");
        ValidatableResponse response = Retrier.clientRetry(() ->
                baseDeleteRequest("/datacreator/building/" + building)
        );
    }

    /**
     * Удаляем здание у заказа, чтобы можно было удалить здание
     */
    public void deleteBuildingInOrder(String order) {
        log.info("Calling DataCreator " +
                "/datacreator/order/deleteBuilding");
        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest(order, "/datacreator/order/deleteBuilding")
        );
    }

    /**
     * Создаем или получаем существующее задание на инвентаризацию ячейки
     */
    public void createOrGetExistingInventorization(String loc) {
        log.info("Calling DataCreator " +
                "/datacreator/inventorization/create/{}", loc);
        ValidatableResponse response = Retrier.clientRetry(() ->
                basePostRequest("", "/datacreator/inventorization/create/" + loc)
        );

        response.body(notNullValue());

        log.info("Created inventorization task {}", response.extract().asString());
    }

    public List<String> getStatusesByLot(String lot) {
        log.info("Calling DataCreator " +
                "/datacreator/inventory-hold/get-statuses-by-lot/{}", lot);
        ValidatableResponse response = Retrier.clientRetry(() ->
                baseGetRequest("/datacreator/inventory-hold/get-statuses-by-lot/" + lot)
        );

        List<String> statuses = response.extract().jsonPath().getList("");
        log.info("Lot {} has hold statuses: {}", lot, statuses);

        return statuses;

    }

    public void placeHoldOnLot(String lot, InventoryHoldStatus holdStatus) {
        log.info("Calling Datacreator inventory-hold/place-hold-on-lot");
        Map<String, String> reqBody = Map.of("lot", lot, "holdStatus", holdStatus.toString());

        ValidatableResponse response = Retrier.clientRetry(() -> basePostRequest(reqBody, "datacreator/inventory-hold/place-hold-on-lot"));
    }

    public void deleteExpiredSuffixes() {
        log.info("Calling Datacreator delete " +
                "/datacreator/suffixes/expired?hours=2");

        baseDeleteRequest(
                "/datacreator/suffixes/expired?hours=2");
    }

    public List<String> getOrderStatusHistory(String orderId) {
        log.info("Calling Datacreator get " +
                "/datacreator/order/gerOrderStatusHistory/" + orderId);
        ValidatableResponse response = Retrier.clientRetry(() ->
                baseGetRequest("/datacreator/order/gerOrderStatusHistory/" + orderId)
        );

        List<String> statuses = response.extract().jsonPath().getList("");
        log.info("Order {} was in statuses: {}", orderId, statuses);

        return statuses;
    }

    public String checkTotalOpenQty(String order) {
        log.info("Calling DataCreator " +
                "/datacreator/order/getSumQtyOpen/{}", order);

        ValidatableResponse response = Retrier.clientRetry(() ->
                baseGetRequest("/datacreator/order/getSumQtyOpen/" + order)
        );

        return response.extract().asString();
    }

    public ValidatableResponse deleteTtsNotifications(String username) {
        log.info("Calling Datacreator delete " +
                "/datacreator/tts/notifications/?userName=" + username);

        return baseDeleteRequest(
                "/datacreator/tts/notifications/?userName=" + username
        );
    }
}
