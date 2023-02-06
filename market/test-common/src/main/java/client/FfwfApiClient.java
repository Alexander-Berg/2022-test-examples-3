package client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import api.FfwfApi;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.ff.client.dto.CreateSupplyRequestDTO;
import ru.yandex.market.ff.client.dto.CreateSupplyRequestItemDTO;
import ru.yandex.market.ff.client.dto.RealSupplierInfoDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDetailsDTO;
import ru.yandex.market.ff.client.enums.CalendaringMode;
import ru.yandex.market.ff.client.enums.RequestType;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/ffwfapi.properties")
public class FfwfApiClient {

    private final FfwfApi ffwfApi;

    @Property("ffwfapi.host")
    private String host;

    public FfwfApiClient() {
        PropertyLoader.newInstance().populate(this);
        ffwfApi = RETROFIT.getRetrofit(host).create(FfwfApi.class);
    }

    @SneakyThrows
    public ShopRequestDetailsDTO getRequest(Long requestId) {
        log.debug("Getting shop request information...");
        Response<ShopRequestDetailsDTO> execute = ffwfApi.getRequest(requestId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить shop request по id " + requestId);
        Assertions.assertNotNull(execute.body(), "Пустой ответ при получении shop request'a c id " + requestId);
        return execute.body();
    }

    @SneakyThrows
    public ShopRequestDTO createXdocRequest() {
        log.debug("Creating shop request for 2 XDoc transportations...");
        CreateSupplyRequestDTO createSupplyRequestDTO = new CreateSupplyRequestDTO();
        createSupplyRequestDTO.setxDocDate(
            OffsetDateTime.of(LocalDateTime.now().plus(172800, ChronoUnit.SECONDS), ZoneOffset.UTC)
        );
        createSupplyRequestDTO.setxDocServiceId(2000L);
        createSupplyRequestDTO.setSupplierId(10264281L);
        createSupplyRequestDTO.setType(RequestType.X_DOC_PARTNER_SUPPLY_TO_FF.getId());
        createSupplyRequestDTO.setServiceId(172L);
        createSupplyRequestDTO.setCalendaringMode(CalendaringMode.NOT_REQUIRED);
        createSupplyRequestDTO.setComment("it's a QA autotests generated request");
        CreateSupplyRequestItemDTO createSupplyRequestItemDTO = new CreateSupplyRequestItemDTO();
        createSupplyRequestItemDTO.setArticle("100438892408");
        createSupplyRequestItemDTO.setName("Подушка Theraline для беременных 190 см, джерси капучино");
        createSupplyRequestItemDTO.setBarcodes(List.of("100438892408"));
        createSupplyRequestItemDTO.setCount(10);
        createSupplyRequestItemDTO.setPrice(BigDecimal.valueOf(100.5));
        createSupplyRequestItemDTO.setComment("Test");
        createSupplyRequestDTO.setItems(List.of(createSupplyRequestItemDTO));
        Response<ShopRequestDTO> execute = ffwfApi.createXdocRequest(createSupplyRequestDTO).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось создать shop request для XDoc перемещений");
        Assertions.assertNotNull(execute.body(), "Пустой ответ при создании shop request'a для XDoc перемещений");
        return execute.body();
    }

    @SneakyThrows
    public ShopRequestDTO create1PXDocRequest(Long xDocServiceId, Long serviceId) {
        log.debug("Creating shop request for 2 XDoc transportations...");
        CreateSupplyRequestDTO createSupplyRequestDTO = new CreateSupplyRequestDTO();
        createSupplyRequestDTO.setxDocDate(
            OffsetDateTime.of(LocalDateTime.now().plus(120, ChronoUnit.SECONDS), ZoneOffset.UTC)
        );
        createSupplyRequestDTO.setxDocServiceId(xDocServiceId);
        createSupplyRequestDTO.setSupplierId(10264169L);
        createSupplyRequestDTO.setType(RequestType.X_DOC_PARTNER_SUPPLY_TO_FF.getId());
        createSupplyRequestDTO.setServiceId(serviceId);
        createSupplyRequestDTO.setCalendaringMode(CalendaringMode.NOT_REQUIRED);
        createSupplyRequestDTO.setComment("it's a QA autotests generated request for 1P");
        CreateSupplyRequestItemDTO createSupplyRequestItemDTO = new CreateSupplyRequestItemDTO();
        createSupplyRequestItemDTO.setArticle("00065.00026.100126176174");
        createSupplyRequestItemDTO.setName("Подушка Theraline для беременных 170 см, малиновый");
        createSupplyRequestItemDTO.setBarcodes(List.of("100438892408"));
        createSupplyRequestItemDTO.setCount(10);
        createSupplyRequestItemDTO.setPrice(BigDecimal.valueOf(100.5));
        createSupplyRequestItemDTO.setComment("Test");
        RealSupplierInfoDTO realSupplier = new RealSupplierInfoDTO();
        realSupplier.setId("10266717");
        realSupplier.setName("1P поставщик «00065»");
        createSupplyRequestItemDTO.setRealSupplier(realSupplier);
        createSupplyRequestDTO.setItems(List.of(createSupplyRequestItemDTO));
        Response<ShopRequestDTO> execute = ffwfApi.createXdocRequest(createSupplyRequestDTO).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось создать shop request для 1P XDoc перемещений");
        Assertions.assertNotNull(execute.body(), "Пустой ответ при создании shop request'a для 1P XDoc перемещений");
        return execute.body();
    }

}
