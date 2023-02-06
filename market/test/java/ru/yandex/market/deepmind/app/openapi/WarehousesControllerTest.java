package ru.yandex.market.deepmind.app.openapi;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.deepmind.app.controllers.WarehousesController;
import ru.yandex.market.deepmind.app.openapi.exception.ApiResponseEntityExceptionHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class WarehousesControllerTest extends BaseOpenApiTest {
    private final String warehouseUrl = "/api/warehouses/";

    @Autowired
    private DeepmindWarehouseRepository deepmindWarehouseRepository;

    private Warehouse warehouse1;
    private Warehouse warehouse2;
    private Warehouse warehouse3;

    @Before
    @SuppressWarnings("checkstyle:magicnumber")
    public void setUp() {
        var fulfillmentWarehouseService =
            new AvailableWarehouseServiceImpl(deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_FULFILLMENT);
        var crossdockWarehouseService =
            new AvailableWarehouseServiceImpl(deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_CROSSDOCK);
        var controller = new WarehousesController(
            fulfillmentWarehouseService,
            crossdockWarehouseService,
            deepmindWarehouseRepository
        );
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new ApiResponseEntityExceptionHandler())
            .build();

        warehouse1 = deepmindWarehouseRepository.save(
                new Warehouse()
                        .setId(1L)
                        .setName("uno")
                        .setCargoTypeLmsIds(1L, 2L, 3L)
                        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
                        .setType(WarehouseType.FULFILLMENT)
        );
        warehouse2 = deepmindWarehouseRepository.save(
                new Warehouse()
                        .setId(2L)
                        .setName("dos")
                        .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
                        .setType(WarehouseType.FULFILLMENT));
        warehouse3 = deepmindWarehouseRepository.save(
                new Warehouse()
                        .setId(3L)
                        .setName("dropship warehouse")
                        .setType(WarehouseType.DROPSHIP)
        );
    }

    @Test
    public void testList() throws Exception {
        MvcResult listResult = getJson(buildUrl("list"));
        List<Warehouse> page = readJsonList(listResult, Warehouse.class);
        assertThat(page).containsExactly(warehouse1, warehouse2, warehouse3);
    }

    @Test
    public void testFindById() throws Exception {
        String path = "get-by-id";
        MvcResult result = getJson(buildUrl(path, "id=1"));
        Warehouse found = readJson(result, Warehouse.class);
        assertThat(found).isEqualTo(warehouse1);

        result = getJson(buildUrl(path, "id=3"));
        found = readJson(result, Warehouse.class);
        assertThat(found).isEqualTo(warehouse3);

        MvcResult notFound = get404Json(buildUrl(path, "id=4"));
        Assertions.assertThat(
            notFound.getResponse().getStatus())
            .isEqualTo(404);
    }

    @Test
    public void testFindByName() throws Exception {
        String path = "get-by-name";
        MvcResult listResult = getJson(buildUrl(path, "name=uno"));
        Warehouse found = readJson(listResult, Warehouse.class);
        assertThat(found).isEqualTo(warehouse1);

        MvcResult notFound = get404Json(buildUrl(path, "name=tres"));
        Assertions.assertThat(
            notFound.getResponse().getStatus())
            .isEqualTo(404);
    }

    private String buildUrl(String path, String... parameters) {
        StringBuilder url = new StringBuilder(warehouseUrl).append(path).append("?");
        for (int i = 0; i < parameters.length; i++) {
            url.append(parameters[i]);
            if (i + 1 < parameters.length) {
                url.append("&");
            }
        }
        return url.toString();
    }
}
