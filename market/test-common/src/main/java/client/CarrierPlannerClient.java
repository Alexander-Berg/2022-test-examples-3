package client;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import api.CarrierPlannerApi;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.tpl.carrier.planner.manual.movement.ManualCreateMovementDto;
import ru.yandex.market.tpl.carrier.planner.manual.run.ManualCreateRunDto;
import ru.yandex.market.tpl.carrier.planner.manual.run.ManualCreateRunItemDto;
import ru.yandex.market.tpl.carrier.planner.manual.run.ManualRunDto;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/carrierplanner.properties")
public class CarrierPlannerClient {

    private static final long WAREHOUSE_TO_ID = 86L;
    private final CarrierPlannerApi carrierPlannerApi;

    @Property("carrierplanner.host")
    private String host;

    public CarrierPlannerClient() {
        PropertyLoader.newInstance().populate(this);
        carrierPlannerApi = RETROFIT.getRetrofit(host).create(CarrierPlannerApi.class);
    }

    @SneakyThrows
    public ManualRunDto createRun() {
        log.debug("Creating run...");
        ManualCreateRunDto createRunDto = new ManualCreateRunDto()
            .setCampaignId(1001381924L)
            .setDeliveryServiceId(223463L)
            .setExternalId(new Timestamp(System.currentTimeMillis()).toString())
            .setRunDate(LocalDate.now());
        ManualCreateRunItemDto runItemDto0 = buildRunItemDto(0, 6343L, WAREHOUSE_TO_ID);
        ManualCreateRunItemDto runItemDto1 = buildRunItemDto(1, 6359L, WAREHOUSE_TO_ID);
        List<ManualCreateRunItemDto> createRunItemDto = List.of(runItemDto0, runItemDto1);
        createRunDto.setItems(createRunItemDto);
        Response<ManualRunDto> execute = carrierPlannerApi.createRun(
            TVM.INSTANCE.getServiceTicket(TVM.CARRIER_PLANNER),
            createRunDto).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось создать рейс в курьерской платформе");
        Assertions.assertNotNull(execute.body(), "Пустой ответ на запрос создания рейса в курьерской платформе");
        return execute.body();
    }

    private ManualCreateMovementDto createMovementDtoForManualRun(Long orderWarehouseId, Long orderWarehouseToId) {
        ManualCreateMovementDto createMovementDto = new ManualCreateMovementDto();
        createMovementDto.setDeliveryIntervalFrom(Instant.now());
        createMovementDto.setDeliveryIntervalTo(Instant.now());
        createMovementDto.setDeliveryServiceId(223463L);
        createMovementDto.setExternalId(Instant.now().toString());
        createMovementDto.setOrderWarehouseId(orderWarehouseId);
        createMovementDto.setOrderWarehouseToId(orderWarehouseToId);
        createMovementDto.setPallets(5);
        createMovementDto.setVolume(BigDecimal.valueOf(7));
        createMovementDto.setWeight(BigDecimal.valueOf(9));
        return createMovementDto;
    }

    private ManualCreateRunItemDto buildRunItemDto(int order, long orderWarehouseId, long orderWarehouseToId) {
        ManualCreateRunItemDto runItemDto = new ManualCreateRunItemDto();
        runItemDto.setOrder(order);
        runItemDto.setMovementDto(createMovementDtoForManualRun(orderWarehouseId, orderWarehouseToId));
        return runItemDto;
    }

    @SneakyThrows
    public void confirmRun(long runId) {
        log.debug("Confirming run...");
        Response<ResponseBody> execute = carrierPlannerApi.confirmRun(
            TVM.INSTANCE.getServiceTicket(TVM.CARRIER_PLANNER),
            runId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось назначить транспорт на рейс");
    }

    @SneakyThrows
    public void assignUser(long runId, long userId) {
        log.debug("Assigning user to run...");
        Response<ResponseBody> execute = carrierPlannerApi.assignUser(
            TVM.INSTANCE.getServiceTicket(TVM.CARRIER_PLANNER),
            runId,
            userId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось назначить водителя на рейс");
    }

    @SneakyThrows
    public void assignTransport(long runId, long transportId) {
        log.debug("Assigning transport to run...");
        Response<ResponseBody> execute = carrierPlannerApi.assignTransport(
            TVM.INSTANCE.getServiceTicket(TVM.CARRIER_PLANNER),
            runId,
            transportId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось назначить транспорт на рейс");
    }

}
