package ru.yandex.market.hrms.api.controller.transfer;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TransferControllerGetPageTest extends AbstractApiTest {

    @Test
    @DbUnitDataSet(before = "TransferControllerGetPageTest.before.csv")
    void smokeTest() throws Exception {
        mockMvc.perform(get("/lms/transfer"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("transfer_page.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "TransferControllerGetPageTestNew.before.csv")
    void getTransferByDomainId() throws Exception {
        mockMvc.perform(get("/lms/transfer-new")
                        .queryParam("domainId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("transfer_page_by_domain_id.json"), false));
    }

    @Test
    @DbUnitDataSet(before = "TransferControllerGetPageTestNew.before.csv")
    void getTransferByName() throws Exception {
        mockMvc.perform(get("/lms/transfer-new")
                        .queryParam("domainId", "1")
                        .queryParam("name", "андр"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("transfer_page_by_name.json"), false));
    }

    @Test
    @DbUnitDataSet(before = "TransferControllerGetPageTestNew.before.csv")
    void getTransferByPosition() throws Exception {
        mockMvc.perform(get("/lms/transfer-new")
                        .queryParam("domainId", "1")
                        .queryParam("position", "клад"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("transfer_page_position.json"), false));
    }

    @Test
    @DbUnitDataSet(before = "TransferControllerGetPageTestNew.before.csv")
    void getTransferByDate() throws Exception {
        mockMvc.perform(get("/lms/transfer-new")
                        .queryParam("domainId", "1")
                        .queryParam("date", "2020-02-02"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("transfer_page_by_date.json"), false));
    }
}
