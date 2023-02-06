package ru.yandex.market.logistics.management.controller.businessWarehouse;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.noContent;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

@DisplayName("Получение бизнес-склада")
@DatabaseSetup({
    "/data/controller/businessWarehouse/prepare.xml",
    "/data/controller/businessWarehouse/prepare_get.xml",
})
public class BusinessWarehouseGetTest extends AbstractContextualAspectValidationTest {

    @DatabaseSetup(
        value = "/data/controller/businessWarehouse/prepare_get_retail.xml",
        type = DatabaseOperation.INSERT
    )
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Получение бизнес-склада")
    void getBusinessWarehouse(
        @SuppressWarnings("unused") String name,
        Long partnerId,
        String responsePath
    ) throws Exception {
        performGet(partnerId).andExpect(status().isOk()).andExpect(content().json(pathToJson(responsePath)));
    }

    @Test
    @DisplayName("Получение бизнес-склада - у партнера нет складов")
    void getBusinessWarehouseNoWarehouses() throws Exception {
        performGet(7L)
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Test
    @DatabaseSetup(value = "/data/controller/businessWarehouse/warehouse.xml", type = DatabaseOperation.INSERT)
    @DisplayName("Получение бизнес-склада - у партнера больше одного активного склада")
    void getBusinessWarehouseTwoWarehouses() throws Exception {
        performGet(1L)
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/businessWarehouse/response/get_business_warehouse_more_than_one.json")
            ));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/businessWarehouse/partner_with_invalid_type.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Получение бизнес-склада - партнер невалидного типа")
    void getBusinessWarehouseInvalidPartnerType() throws Exception {
        performGet(300L)
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Nonnull
    private ResultActions performGet(Long partnerId) throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders.get("/externalApi/business-warehouse/" + partnerId.toString())
        );
    }

    @Nonnull
    private static Stream<Arguments> getBusinessWarehouse() {
        return Stream.of(
            Arguments.of(
                "Забор",
                1L,
                "data/controller/businessWarehouse/response/get_business_warehouse.json"
            ),
            Arguments.of(
                "Самопривоз",
                2L,
                "data/controller/businessWarehouse/response/get_business_warehouse_import.json"
            ),
            Arguments.of(
                "Экспресс",
                3L,
                "data/controller/businessWarehouse/response/get_business_warehouse_express_with_relation.json"
            ),
            Arguments.of(
                "Экспресс без связки",
                4L,
                "data/controller/businessWarehouse/response/get_business_warehouse_express.json"
            ),
            Arguments.of(
                "Экспресс DBS",
                5L,
                "data/controller/businessWarehouse/response/get_business_warehouse_express_dbs.json"
            ),
            Arguments.of(
                "Tpl",
                8L,
                "data/controller/businessWarehouse/response/get_business_warehouse_tpl.json"
            ),
            Arguments.of(
                "Retail",
                10L,
                "data/controller/businessWarehouse/response/get_business_warehouse_retail.json"
            )
        );
    }
}
