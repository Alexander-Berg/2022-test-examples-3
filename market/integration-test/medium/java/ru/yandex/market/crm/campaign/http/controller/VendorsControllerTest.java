package ru.yandex.market.crm.campaign.http.controller;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.domain.vendors.Vendor;
import ru.yandex.market.crm.campaign.services.vendors.VendorsService;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumWithoutYtTest;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
class VendorsControllerTest extends AbstractControllerMediumWithoutYtTest {

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private VendorsService vendorsService;

    @BeforeEach
    void setUp() {
        vendorsService.setVendors(List.of(
                new Vendor(1, "Sams", "Publishing"),
                new Vendor(2, "Samsung", "Electronics"),
                new Vendor(3, "LG", "Electronics"),
                new Vendor(4, "Apple", "Electronics"),
                new Vendor(5, "Google", "Electronics"),
                new Vendor(6, "Yandex", "Electronics"),
                new Vendor(7, "Xiaomi", "Electronics"),
                new Vendor(8, "Microsoft", "Electronics"),
                new Vendor(9, "Sony", "Electronics"),
                new Vendor(10, "Acorp", "Electronics"),
                new Vendor(11, "HP", "Electronics")
        ));
    }

    /**
     * В выдачу ручки GET /api/vendors попадают только вендоры, название которых начинается со
     * значения параметра name_part
     */
    @Test
    void testGetVendors() throws Exception {
        var vendors = requestVendors("Sams");

        assertThat(vendors, hasSize(2));
        assertVendor(1, "Sams", "Publishing", vendors.get(0));
        assertVendor(2, "Samsung", "Electronics", vendors.get(1));
    }

    /**
     * В выдачу ручки GET /api/vendors попадают только вендоры, название которых начинается со
     * значения параметра name_part. При этом регистр в котором передано значение параметра не учитывается.
     */
    @Test
    void testNamePartIsCaseInsensitive() throws Exception {
        var vendors = requestVendors("sams");

        assertThat(vendors, hasSize(2));
        assertVendor(1, "Sams", "Publishing", vendors.get(0));
        assertVendor(2, "Samsung", "Electronics", vendors.get(1));
    }

    /**
     * Если значение параметра name_part пустое, выдача ручки GET /api/vendors представляет собой список
     * из 10 случайных элементов
     */
    @Test
    void testResponseIfNamePartIsNotSpecified() throws Exception {
        var vendors = requestVendors("");
        assertThat(vendors, hasSize(10));
    }

    private List<Vendor> requestVendors(String namePart) throws Exception {
        var result = mockMvc.perform(get("/api/vendors").param("name_part", namePart))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        return jsonDeserializer.readObject(new TypeReference<>() {}, result.getResponse().getContentAsString());
    }

    private static void assertVendor(long id, String name, String comment, Vendor vendor) {
        assertEquals(id, vendor.getId());
        assertEquals(name, vendor.getName());
        assertEquals(comment, vendor.getComment());
    }
}
