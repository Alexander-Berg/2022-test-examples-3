package ru.yandex.market.replenishment.autoorder.api.demand_union;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.api.dto.DemandUnionRequest;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandIdentityDTO;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.dtoToString;
@WithMockLogin
public class DemandUnionTest extends ControllerTest {

    @Test
    @DbUnitDataSet(before = "DemandUnionTest.testUniteDemandsWithDifferentSuppliers.before.csv")
    public void testUniteDemandsWithDifferentSuppliers() throws Exception {
        final DemandUnionRequest request = new DemandUnionRequest(1L, Collections.singletonList(3L));
        request.setDemandKeys(Arrays.asList(new DemandIdentityDTO(1L, 1), new DemandIdentityDTO(3L, 1)));
        mockMvc.perform(post("/api/v1/demands/unite")
                        .content(dtoToString(request))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message").value("Потребность 3 на поставщика Одуванчик"));
    }

    @Test
    @DbUnitDataSet(before = "DemandUnionTest.testUniteDemandsWrongStatus.before.csv")
    public void testUniteDemandsWrongStatus() throws Exception {
        final DemandUnionRequest request = new DemandUnionRequest(1L, Collections.singletonList(3L));
        request.setDemandKeys(Arrays.asList(new DemandIdentityDTO(1L, 1), new DemandIdentityDTO(3L, 1)));
        mockMvc.perform(post("/api/v1/demands/unite")
                        .content(dtoToString(request))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message").value("Потребность 3 в статусе PROCESSED"));
    }

    @Test
    @DbUnitDataSet(before = "DemandUnionTest.testUniteAnotherWarehouse.before.csv")
    public void testUniteDemandsAnotherWarehouse() throws Exception {
        final DemandUnionRequest request = new DemandUnionRequest(1L, Collections.singletonList(3L));
        request.setDemandKeys(Arrays.asList(new DemandIdentityDTO(1L, 1), new DemandIdentityDTO(3L, 1)));
        mockMvc.perform(post("/api/v1/demands/unite")
                        .content(dtoToString(request))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message").value("Потребность 3 на другой склад Ростов"));
    }

    @Test
    @DbUnitDataSet(before = "DemandUnionTest.testUniteDemandsSpecialAndRegular.before.csv",
        after = "DemandUnionTest.testUniteDemandsSpecialAndRegular.after.csv")
    public void testUniteDemandsSpecialAndRegular() throws Exception {
        final DemandUnionRequest request = new DemandUnionRequest(101L, Collections.singletonList(3L));
        request.setDemandKeys(Arrays.asList(new DemandIdentityDTO(101L, 1), new DemandIdentityDTO(3L, 1)));
        mockMvc.perform(post("/api/v1/demands/unite")
                .content(dtoToString(request))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandUnionTest.testUniteDemandsPositive.before.csv",
            after = "DemandUnionTest.testUniteDemandsPositive.after.csv")
    public void testUniteDemandsPositive() throws Exception {
        final DemandUnionRequest request = new DemandUnionRequest(101L, Collections.singletonList(102L));
        request.setDemandKeys(Arrays.asList(new DemandIdentityDTO(101L, 1), new DemandIdentityDTO(102L, 1)));
        mockMvc.perform(post("/api/v1/demands/unite")
                        .content(dtoToString(request))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandUnionTest.testUndoUnion.before.csv",
            after = "DemandUnionTest.testUndoUnion.after.csv")
    public void testUndoUnion() throws Exception {
        mockMvc.perform(post("/api/v1/demands/undo-union")
                        .content(dtoToString(new DemandIdentityDTO(1L, 1)))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids[0]").value(101))
                .andExpect(jsonPath("$.ids[1]").value(102));
    }

    @Test
    @DbUnitDataSet(before = "DemandUnionTest.testGetUnionDemands.before.csv")
    public void testGetUnionDemands() throws Exception {

        mockMvc.perform(get("/api/v1/demands/1/union-demands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))

                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].warehouse.id").value(145))
                .andExpect(jsonPath("$[0].supplier.id").value(1))
                .andExpect(jsonPath("$[0].supplyRoute").value("direct"))
                .andExpect(jsonPath("$[0].deliveryDate").value("2019-03-13"))
                .andExpect(jsonPath("$[0].orderDate").value("2019-03-10"))
                .andExpect(jsonPath("$[0].status").value("DELETED"))
                .andExpect(jsonPath("$[0].mskus").value(1))
                .andExpect(jsonPath("$[0].items").value(10))
                .andExpect(jsonPath("$[0].adjustedItems").value(10))

                .andExpect(jsonPath("$[1].id").value(102))
                .andExpect(jsonPath("$[1].warehouse.id").value(145))
                .andExpect(jsonPath("$[1].supplier.id").value(1))
                .andExpect(jsonPath("$[1].supplyRoute").value("direct"))
                .andExpect(jsonPath("$[1].deliveryDate").value("2019-03-13"))
                .andExpect(jsonPath("$[1].orderDate").value("2019-03-10"))
                .andExpect(jsonPath("$[1].status").value("DELETED"))
                .andExpect(jsonPath("$[1].mskus").value(1))
                .andExpect(jsonPath("$[1].items").value(15))
                .andExpect(jsonPath("$[1].adjustedItems").value(15));
    }

    @Test
    @DbUnitDataSet(before = "DemandUnionTest.testGetUnionDemands.before.csv")
    public void testGetUnionDemandsEmpty() throws Exception {

        mockMvc.perform(get("/api/v1/demands/101/union-demands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DbUnitDataSet(before = "DemandUnionTest.testUniteDemandsWithDifferentSuppliers.before.csv")
    public void testUniteDemands_WrongDemand() throws Exception {
        final DemandUnionRequest request = new DemandUnionRequest(123L, Collections.singletonList(3L));
        request.setDemandKeys(Arrays.asList(new DemandIdentityDTO(1L, 1), new DemandIdentityDTO(3L, 1)));
        mockMvc.perform(post("/api/v1/demands/unite")
                        .content(dtoToString(request))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Потребности с типом 1P и c id 123 нет в базе, вероятно произошел реимпорт рекомендаций " +
                                "и у них поменялись id, попробуйте снова найти их на экране с календарем и " +
                                "открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "DemandUnionTest.testUndoUnion.before.csv",
            after = "DemandUnionTest.testUndoUnion.before.csv")
    public void testUndoUnion_WrongDemand() throws Exception {
        mockMvc.perform(post("/api/v1/demands/undo-union")
                        .content(dtoToString(new DemandIdentityDTO(123L, 1)))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Потребности с типом 1P и c id 123 нет в базе, вероятно произошел реимпорт рекомендаций " +
                                "и у них поменялись id, попробуйте снова найти их на экране с календарем и " +
                                "открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }
}
