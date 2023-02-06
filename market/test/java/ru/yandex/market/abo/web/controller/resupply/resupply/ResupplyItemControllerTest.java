package ru.yandex.market.abo.web.controller.resupply.resupply;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.abo.AbstractControllerTest;
import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.resupply.entity.ResupplyEntity;
import ru.yandex.market.abo.core.resupply.entity.ResupplyItemEntity;
import ru.yandex.market.abo.core.resupply.entity.ResupplyStatus;
import ru.yandex.market.abo.core.resupply.entity.Warehouse;
import ru.yandex.market.abo.core.resupply.repo.ResupplyItemRepository;
import ru.yandex.market.abo.core.resupply.repo.ResupplyRepository;
import ru.yandex.market.abo.core.resupply.stock.ResupplyStock;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResupplyItemControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResupplyItemRepository resupplyItemRepository;

    @Autowired
    private ResupplyRepository resupplyRepository;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private ConfigurationService aboConfigurationService;

    @AfterEach
    public void deleteRegistryAndItems() {
        resupplyItemRepository.deleteAll();
        resupplyRepository.deleteAll();
    }

    @Test
    public void showResupplyItem() throws Exception {
        ResupplyEntity resupplyEntity = new ResupplyEntity();
        resupplyEntity.setStatus(ResupplyStatus.DRAFT);
        resupplyEntity.setWarehouse(Warehouse.SOFINO);
        ResupplyEntity savedResupplyEntity = resupplyRepository.saveAndFlush(resupplyEntity);
        aboConfigurationService.updateValue(CoreConfig.RECEIVING_IN_ABO_ENABLED.getIdAsString(), "1");

        ResupplyItemEntity resupplyItemEntity = ResupplyItemEntity.builder()
                .orderId(1L)
                .orderItemId(1L)
                .supplierId(1L)
                .shopSku("12345")
                .marketSku(213L)
                .categoryId(1)
                .title("Title")
                .price(new BigDecimal(123))
                .createdAt(LocalDateTime.now())
                .resupplyStock(ResupplyStock.GOOD)
                .supplierTypeId(SupplierType.FIRST_PARTY.getId())
                .resupply(savedResupplyEntity)
                .build();
        ResupplyItemEntity savedResupplyItemEntity = resupplyItemRepository.saveAndFlush(resupplyItemEntity);

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);

        when(checkouterClient.getOrder(any(), any()))
                .thenReturn(order);

        mockMvc.perform(put(String.format("/resupplies/%d/items/%d",
                savedResupplyEntity.getId(), savedResupplyItemEntity.getId()))
                .param("sentAt", "2021-10-31")
                .param("count", "1")
                .with(csrf()))
                .andExpect(status().isOk());
    }
}
