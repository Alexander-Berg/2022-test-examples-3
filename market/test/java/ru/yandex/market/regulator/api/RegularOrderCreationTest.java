package ru.yandex.market.regulator.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.mj.generated.server.api.RegularOrderApiController;
import ru.yandex.mj.generated.server.model.RegularOrderTemplate;
import ru.yandex.mj.generated.server.model.RegularOrderTemplateItem;
import ru.yandex.market.regulator.config.MockConfig;
import ru.yandex.market.regulator.config.PgJdbcConfigEmbedded;
import ru.yandex.market.regulator.dao.RegularOrderDao;
import ru.yandex.market.regulator.postgres.AbstractPostgresTest;
import ru.yandex.market.regulator.service.TemplateService;
import ru.yandex.market.regulator.utils.RegularOrderHelper;
import ru.yandex.market.regulator.utils.RegularOrderItemsHelper;
import ru.yandex.market.util.FormatUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.regulator.utils.RegularOrderItemsHelper.createItem;

/**
 * @author Anastasiya Emelianova / orphie@ / 10/6/21
 */
@Disabled
@ExtendWith(SpringExtension.class)
@Transactional
@Rollback
@WebAppConfiguration
@ContextConfiguration(classes = {RegularOrderApiController.class, RegularOrderApiService.class, TemplateService.class,
        PgJdbcConfigEmbedded.class, MockConfig.class, RegularOrderDao.class})
public class RegularOrderCreationTest extends AbstractPostgresTest {
    @Autowired
    private NamedParameterJdbcTemplate postgresJdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void simpleSaveTest() throws Exception {
        int deliveryInterval = 20;
        String uid = "111";
        String persAddressId = "addressId";
        String recepientPersContactId = "contactId";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isOk());

        List<RegularOrderTemplate> template = selectAllOrdersFromBase();

        assertNotNull(template);
        assertEquals(1, template.size());

        RegularOrderTemplate orderTemplate = template.get(0);
        assertEquals(deliveryInterval, orderTemplate.getDeliveryInterval());
        assertEquals(persAddressId, orderTemplate.getPersAddressId());
        assertEquals(recepientPersContactId, orderTemplate.getRecepientPersContactId());

        List<RegularOrderTemplateItem> itemModels = selectAllItemsForOrder(orderTemplate.getId());

        assertNotNull(itemModels);
        assertEquals(2, itemModels.size());
        assertEquals(item1.getCount(), itemModels.get(0).getCount());
        assertEquals(item1.getMsku(), itemModels.get(0).getMsku());
        assertEquals(item1.getShopSku(), itemModels.get(0).getShopSku());
        assertEquals(item1.getPrice(), itemModels.get(0).getPrice());
        assertEquals(item2.getMsku(), itemModels.get(1).getMsku());
    }

    @Test
    public void testSaveWithoutDeliveryInterval() throws Exception {
        String uid = "111";
        String persAddressId = "addressId";
        String recepientPersContactId = "contactId";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, null, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithIncorrectDeliveryInterval() throws Exception {
        int deliveryInterval = 0;
        String uid = "111";
        String persAddressId = "addressId";
        String recepientPersContactId = "contactId";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithEmptyUid() throws Exception {
        int deliveryInterval = 1;
        String uid = "";
        String persAddressId = "addressId";
        String recepientPersContactId = "contactId";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithEmptyPersAddressId() throws Exception {
        int deliveryInterval = 1;
        String uid = "123";
        String persAddressId = "";
        String recepientPersContactId = "contactId";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithEmptyRecipientPersContactId() throws Exception {
        int deliveryInterval = 1;
        String uid = "123";
        String persAddressId = "address";
        String recepientPersContactId = "";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithEmptyRegularOrderTemplateItemList() throws Exception {
        int deliveryInterval = 1;
        String uid = "123";
        String persAddressId = "address";
        String recepientPersContactId = "contact";
        List<RegularOrderTemplateItem> items = Collections.emptyList();

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithoutRegularOrderTemplateItemList() throws Exception {
        int deliveryInterval = 1;
        String uid = "123";
        String persAddressId = "address";
        String recepientPersContactId = "contact";
        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                null, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithoutItemMsku() throws Exception {
        int deliveryInterval = 1;
        String uid = "123";
        String persAddressId = "address";
        String recepientPersContactId = "contact";
        RegularOrderTemplateItem item1 = createItem(2, null, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithIncorrectMsku() throws Exception {
        int deliveryInterval = 1;
        String uid = "123";
        String persAddressId = "address";
        String recepientPersContactId = "contact";
        RegularOrderTemplateItem item1 = createItem(2, 0L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithEmptyShopSku() throws Exception {
        int deliveryInterval = 1;
        String uid = "123";
        String persAddressId = "address";
        String recepientPersContactId = "contact";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithoutItemCount() throws Exception {
        int deliveryInterval = 1;
        String uid = "123";
        String persAddressId = "address";
        String recepientPersContactId = "contact";
        RegularOrderTemplateItem item1 = createItem(null, 123L, 12.3, "122");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithIncorrectItemCount() throws Exception {
        int deliveryInterval = 1;
        String uid = "123";
        String persAddressId = "address";
        String recepientPersContactId = "contact";
        RegularOrderTemplateItem item1 = createItem(0, 123L, 12.3, "122");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithoutItemPrice() throws Exception {
        int deliveryInterval = 1;
        String uid = "123";
        String persAddressId = "address";
        String recepientPersContactId = "contact";
        RegularOrderTemplateItem item1 = createItem(2, 123L, null, "122");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveWithIncorrectItemPrice() throws Exception {
        int deliveryInterval = 1;
        String uid = "123";
        String persAddressId = "address";
        String recepientPersContactId = "contact";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 0.0, "122");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = List.of(item1, item2);

        RegularOrderTemplate order = RegularOrderHelper.buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FormatUtils.toJson(order)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    private List<RegularOrderTemplate> selectAllOrdersFromBase() {
        String query = "select * from regular_order_template";

        return postgresJdbcTemplate.query(query, (rs) -> {
            List<RegularOrderTemplate> list = new ArrayList<>();
            while (rs.next()) {
                list.add(RegularOrderHelper.valueOf(rs));
            }
            return list;
        });
    }

    private List<RegularOrderTemplateItem> selectAllItemsForOrder(long orderId) {
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
        sqlParameterSource.addValue("order_id", orderId);

        String query = "select * from regular_order_template_item as i where i.regular_order_template_id = :order_id";
        return postgresJdbcTemplate.query(query, sqlParameterSource, (rs) -> {
            List<RegularOrderTemplateItem> list = new ArrayList<>();
            while (rs.next()) {
                list.add(RegularOrderItemsHelper.valueOf(rs));
            }
            return list;
        });
    }
}
