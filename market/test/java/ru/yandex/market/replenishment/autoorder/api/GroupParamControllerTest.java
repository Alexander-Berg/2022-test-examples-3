package ru.yandex.market.replenishment.autoorder.api;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.GroupParam;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.GroupParamService;
import ru.yandex.market.replenishment.autoorder.service.excel.GroupParamReader;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class GroupParamControllerTest extends ControllerTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Autowired
    GroupParamService groupParamService;

    private final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.simple.after.csv")
    public void testLoadGroupParams() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.simple.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.simple.after.csv")
    @WithMockLogin("group-param-admin")
    public void testLoadGroupParams_admin() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/user/boris/group-param/excel",
                "GroupParamControllerTest.simple.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_emptySupplier() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.emptySupplier.xlsx"
        ).andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("В строке 2 не указан код поставщика"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_emptyGroup() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.emptyGroup.xlsx"
        ).andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Не указано слово 'группа' для строки 2"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_wrongGroupType() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.wrongGroupType.xlsx"
        ).andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Несуществующий тип группировки 'Шарообразность'"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_notGroupTypes() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.notGroupTypes.xlsx"
        ).andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("В настройках группы не указано ни одного параметра"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_wrongSupplier() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.wrongSupplier.xlsx"
        ).andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Не существует поставщика с кодом 000666"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_wrongVendor() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.wrongVendor.xlsx"
        ).andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Не существует бренда с именем 'WRONG'"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_wrongManufacturer() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.wrongManufacturer.xlsx"
        ).andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Не существует поставщика с именем 'WRONG'"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.before.csv",
            after = "GroupParamControllerTest.simple.after.csv")
    public void testGetExcel() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/group-param/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        List<GroupParam> groupParams = GroupParamReader.read(new ByteArrayInputStream(excelData));
        groupParamService.validateAndReload(1, groupParams);
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.before.csv",
            after = "GroupParamControllerTest.simple.after.csv")
    @WithMockLogin("group-param-admin")
    public void testGetExcel_admin() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/user/boris/group-param/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        List<GroupParam> groupParams = GroupParamReader.read(new ByteArrayInputStream(excelData));
        groupParamService.validateAndReload(1, groupParams);
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_wrongGroupsOrder_1() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.wrongGroupsOrder_1.xlsx"
        ).andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value( "Для поставщика 'TestSupplier1' в группе 2 указана разбивка с перечислением брендов " +
                                        "после группы 1, в которой нет разбивки по брендам"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_wrongGroupsOrder_2() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.wrongGroupsOrder_2.xlsx"
        ).andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Для поставщика 'TestSupplier1' указанные бренды в группе 2 " +
                                "пересекаются с указанными брендами в группе 1"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_wrongGroupsOrder_3() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.wrongGroupsOrder_3.xlsx"
        ).andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Для поставщика 'TestSupplier1' в группе 2 указана разбивка с перечислением производителей " +
                                        "после группы 1, в которой обычная разбивка по производителям"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
            after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_wrongGroupsOrder_4() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.wrongGroupsOrder_4.xlsx"
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для поставщика 'TestSupplier1' указанные производители в группе 2 " +
                    "пересекаются с указанными производителями в группе 1"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
        after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_wrongCategory() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.wrongCategory.xlsx"
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Не существует категории с id '555'"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.old.before.csv",
        after = "GroupParamControllerTest.old.before.csv")
    public void testLoadGroupParams_wrongMsku() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/group-param/excel",
                "GroupParamControllerTest.wrongMsku.xlsx"
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Не существует msku '21783612'"));
    }

    @Test
    @DbUnitDataSet(before = "GroupParamControllerTest.before.csv")
    public void getGroupParamsBySupplierIds() throws Exception {
        mockMvc.perform(get("/api/v1/user/boris/group-param?supplierIds=1,3"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))

            .andExpect(jsonPath("$[0].id").value(101L))
                .andExpect(jsonPath("$[0].supplier.name").value("TestSupplier1"))
                .andExpect(jsonPath("$[0].byPackageAndItems").value(true))
                .andExpect(jsonPath("$[0].byVendors").value(true))
                .andExpect(jsonPath("$[0].vendors[0].name").value("MirPack"))
                .andExpect(jsonPath("$[0].vendors[1].name").value("Liaara"))
                .andExpect(jsonPath("$[0].vendors[2].name").value("MSI"))
                .andExpect(jsonPath("$[0].byManufacturers").value(false))
                .andExpect(jsonPath("$[0].weight").value(5000.0))
                .andExpect(jsonPath("$[0].volume").value(7.0))

                .andExpect(jsonPath("$[1].id").value(102L))
                .andExpect(jsonPath("$[1].supplier.name").value("TestSupplier1"))
                .andExpect(jsonPath("$[1].byPackageAndItems").value(false))
                .andExpect(jsonPath("$[1].promoPurchase").value(true))
                .andExpect(jsonPath("$[1].byVendors").value(false))
                .andExpect(jsonPath("$[1].byManufacturers").value(true))
                .andExpect(jsonPath("$[1].manufacturers").isEmpty())
                .andExpect(jsonPath("$[1].weight").value(5000.0))
                .andExpect(jsonPath("$[1].volume").value(7.0))

                .andExpect(jsonPath("$[2].id").value(105L))
                .andExpect(jsonPath("$[2].supplier.name").value("TestSupplier3"))
                .andExpect(jsonPath("$[2].byPackageAndItems").value(false))
                .andExpect(jsonPath("$[2].byVendors").value(false))
                .andExpect(jsonPath("$[2].byManufacturers").value(true))
                .andExpect(jsonPath("$[2].manufacturers.length()").value(2))
                .andExpect(jsonPath("$[2].weight").value(IsNull.nullValue()))
                .andExpect(jsonPath("$[2].volume").value(IsNull.nullValue()));
    }
}
