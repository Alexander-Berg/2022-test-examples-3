package ru.yandex.market.logistics.lom.admin;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение контактов заказа")
@DatabaseSetup("/controller/admin/contact/before/contacts.xml")
class GetContactsTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получить контакты заказа")
    void getContactsOk() throws Exception {
        getContacts(1L)
            .andExpect(status().isOk())
            .andExpect(content().json(
                extractFileContent("controller/admin/contact/response/all.json"), true)
            );
    }

    @Test
    @DisplayName("Получить пустой список контактов заказа")
    void getContactsEmpty() throws Exception {
        getContacts(2L)
            .andExpect(status().isOk())
            .andExpect(content().json(
                extractFileContent("controller/admin/contact/response/empty.json"), true)
            );
    }

    @Test
    @DisplayName("Заказ с указанным id не существует")
    void getContactsNotFound() throws Exception {
        getContacts(3L)
            .andExpect(status().isNotFound())
            .andExpect(content().json(
                extractFileContent("controller/admin/contact/response/order_not_found.json"), true)
            );
    }

    @Nonnull
    private ResultActions getContacts(long orderId) throws Exception {
        return mockMvc.perform(get("/admin/orders/contacts").param("orderId", String.valueOf(orderId)));
    }
}
