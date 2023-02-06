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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.regulator.utils.RegularOrderHelper.buildOrderTemplate;
import static ru.yandex.market.regulator.utils.RegularOrderItemsHelper.createItem;

/**
 * @author Anastasiya Emelianova / orphie@ / 10/13/21
 */
@Disabled
@ExtendWith(SpringExtension.class)
@Transactional
@Rollback
@WebAppConfiguration
@ContextConfiguration(classes = {RegularOrderApiController.class, RegularOrderApiService.class, TemplateService.class,
        PgJdbcConfigEmbedded.class, MockConfig.class, RegularOrderDao.class})
public class RegularOrderUpdateTest extends AbstractPostgresTest {
    @Autowired
    private NamedParameterJdbcTemplate postgresJdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testSimpleUpdateOrder() throws Exception {
        int deliveryInterval = 20;
        String uid = "111";
        String persAddressId = "addressId";
        String recepientPersContactId = "contactId";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = new ArrayList<>(List.of(item1, item2));
        RegularOrderTemplate orderToSave = buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .content(FormatUtils.toJson(orderToSave)))
                .andExpect(status().isOk());

        List<RegularOrderTemplate> orderTemplateModels = selectAllOrdersFromBase();
        RegularOrderTemplate orderFromBase = orderTemplateModels.get(0);
        items.add(createItem(1, 789L, 100.0, "100"));

        Integer newDeliveryInterval = 7;
        RegularOrderTemplate orderToUpdate = buildOrderTemplate(
                items, newDeliveryInterval, uid, persAddressId, recepientPersContactId);
        orderToUpdate.setId(orderFromBase.getId());
        orderToUpdate.setWeekDay(orderFromBase.getWeekDay());
        mockMvc.perform(
                put("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .content(FormatUtils.toJson(orderToUpdate)))
                .andExpect(status().isOk());

        orderTemplateModels = selectAllOrdersFromBase();
        RegularOrderTemplate updatedOrder = orderTemplateModels.get(0);
        assertNotNull(updatedOrder);
        assertEquals(newDeliveryInterval, updatedOrder.getDeliveryInterval());
        assertEquals(orderToSave.getUid(), updatedOrder.getUid());
        assertEquals(orderToSave.getPersAddressId(), updatedOrder.getPersAddressId());
        assertEquals(orderToSave.getRecepientPersContactId(), updatedOrder.getRecepientPersContactId());

        List<RegularOrderTemplateItem> newItems = selectAllItemsForOrder(updatedOrder.getId());
        assertNotNull(newItems);
        assertEquals(3, newItems.size());
    }

    @Test
    public void updateWithoutOrderId() throws Exception {
        int deliveryInterval = 20;
        String uid = "111";
        String persAddressId = "addressId";
        String recepientPersContactId = "contactId";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = new ArrayList<>(List.of(item1, item2));

        RegularOrderTemplate orderToUpdate = buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                put("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .content(FormatUtils.toJson(orderToUpdate)))
                .andExpect(status().isOk());

        RegularOrderTemplate orderAfterCreation = selectAllOrdersFromBase().get(0);
        assertNotNull(orderAfterCreation.getId());
        assertEquals(orderToUpdate.getUid(), orderAfterCreation.getUid());

        List<RegularOrderTemplateItem> newItems = selectAllItemsForOrder(orderAfterCreation.getId());
        assertNotNull(newItems);
    }

    @Test
    public void updateWithNotExistingInDbOrderId() throws Exception {
        int deliveryInterval = 20;
        String uid = "111";
        String persAddressId = "addressId";
        String recepientPersContactId = "contactId";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = new ArrayList<>(List.of(item1, item2));

        RegularOrderTemplate orderToUpdate = buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);
        orderToUpdate.setId(111L);

        mockMvc.perform(
                put("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .content(FormatUtils.toJson(orderToUpdate)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateOrderItem() throws Exception {
        int deliveryInterval = 20;
        String uid = "111";
        String persAddressId = "addressId";
        String recepientPersContactId = "contactId";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = new ArrayList<>(List.of(item1, item2));

        RegularOrderTemplate orderToSave = buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .content(FormatUtils.toJson(orderToSave)))
                .andExpect(status().isOk());

        List<RegularOrderTemplate> orderTemplateModels = selectAllOrdersFromBase();
        RegularOrderTemplate orderFromBase = orderTemplateModels.get(0);
        List<RegularOrderTemplateItem> itemsFromBase = selectAllItemsForOrder(orderFromBase.getId());
        Long firstItemId = itemsFromBase.get(0).getId();
        Long secondItemId = itemsFromBase.get(1).getId();

        RegularOrderTemplateItem firstItemToUpdate = createItem(10, 890L, 100.0, "111");
        firstItemToUpdate.setId(firstItemId);
        RegularOrderTemplateItem secondItemToUpdate = createItem(5, 456L, 500.0, "900");
        secondItemToUpdate.setId(secondItemId);

        RegularOrderTemplate orderToUpdate = buildOrderTemplate(List.of(firstItemToUpdate, secondItemToUpdate),
                deliveryInterval, uid, persAddressId, recepientPersContactId);
        orderToUpdate.setId(orderFromBase.getId());
        orderToUpdate.setWeekDay(5);

        mockMvc.perform(
                put("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .content(FormatUtils.toJson(orderToUpdate)))
                .andExpect(status().isOk());

        RegularOrderTemplate orderAfterUpdate = selectAllOrdersFromBase().get(0);
        List<RegularOrderTemplateItem> itemsAfterUpdate = selectAllItemsForOrder(orderAfterUpdate.getId());

        assertEquals(2, itemsAfterUpdate.size());
        assertEquals(firstItemToUpdate.getCount(), itemsAfterUpdate.get(0).getCount());
        assertEquals(firstItemToUpdate.getMsku(), itemsAfterUpdate.get(0).getMsku());
        assertEquals(firstItemToUpdate.getPrice(), itemsAfterUpdate.get(0).getPrice());
        assertEquals(firstItemToUpdate.getShopSku(), itemsAfterUpdate.get(0).getShopSku());

    }

    @Test
    public void deleteOrderItem() throws Exception {
        int deliveryInterval = 20;
        String uid = "111";
        String persAddressId = "addressId";
        String recepientPersContactId = "contactId";
        RegularOrderTemplateItem item1 = createItem(2, 123L, 12.3, "890");
        RegularOrderTemplateItem item2 = createItem(5, 456L, 500.0, "900");
        List<RegularOrderTemplateItem> items = new ArrayList<>(List.of(item1, item2));

        RegularOrderTemplate orderToSave = buildOrderTemplate(
                items, deliveryInterval, uid, persAddressId, recepientPersContactId);

        mockMvc.perform(
                post("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .content(FormatUtils.toJson(orderToSave)))
                .andExpect(status().isOk());

        List<RegularOrderTemplate> orderTemplateModels = selectAllOrdersFromBase();
        RegularOrderTemplate orderFromBase = orderTemplateModels.get(0);
        List<RegularOrderTemplateItem> itemsFromBase = selectAllItemsForOrder(orderFromBase.getId());
        Long firstItemId = itemsFromBase.get(0).getId();
        Long secondItemId = itemsFromBase.get(1).getId();

        RegularOrderTemplateItem firstItemToUpdate = createItem(2, 123L, 12.3, "890");
        firstItemToUpdate.setId(firstItemId);

        RegularOrderTemplate orderToUpdate = buildOrderTemplate(Collections.singletonList(firstItemToUpdate),
                deliveryInterval, uid, persAddressId, recepientPersContactId);
        orderToUpdate.setId(orderFromBase.getId());
        orderToUpdate.setWeekDay(5);

        mockMvc.perform(
                put("/v1/api/order_template")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .content(FormatUtils.toJson(orderToUpdate)))
                .andExpect(status().isOk());

        RegularOrderTemplate orderAfterUpdate = selectAllOrdersFromBase().get(0);
        List<RegularOrderTemplateItem> itemsAfterUpdate = selectAllItemsForOrder(orderAfterUpdate.getId());

        assertEquals(1, itemsAfterUpdate.size());
        assertEquals(firstItemToUpdate.getCount(), itemsAfterUpdate.get(0).getCount());
        assertEquals(firstItemToUpdate.getMsku(), itemsAfterUpdate.get(0).getMsku());
        assertEquals(firstItemToUpdate.getPrice(), itemsAfterUpdate.get(0).getPrice());
        assertEquals(firstItemToUpdate.getShopSku(), itemsAfterUpdate.get(0).getShopSku());

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
