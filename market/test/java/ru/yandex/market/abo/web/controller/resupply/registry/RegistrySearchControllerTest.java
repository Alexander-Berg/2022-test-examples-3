package ru.yandex.market.abo.web.controller.resupply.registry;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.abo.AbstractControllerTest;
import ru.yandex.market.abo.core.resupply.registry.Registry;
import ru.yandex.market.abo.core.resupply.registry.RegistryItem;
import ru.yandex.market.abo.core.resupply.registry.RegistryItemRepo;
import ru.yandex.market.abo.core.resupply.registry.RegistryRepo;
import ru.yandex.market.abo.api.entity.resupply.registry.RegistryType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RegistrySearchControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistryItemRepo registryItemRepo;

    @Autowired
    private RegistryRepo registryRepo;

    @AfterEach
    public void deleteRegistryAndItems() {
        registryItemRepo.deleteAll();
        registryRepo.deleteAll();
    }

    @Test
    public void approveByBoxIdAndOrderIdTest() throws Exception {
        Registry r = new Registry();
        RegistryItem item = new RegistryItem();
        item.setApproved(false);
        item.setOrderId("1");
        item.setTrackCode("1234");
        item.setBoxesArray(new String[]{});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{});
        item.setBoxCount(2);

        Registry savedRegistry = registryRepo.saveAndFlush(r);
        item.setRegistry(savedRegistry);
        registryItemRepo.saveAndFlush(item);

        mockMvc.perform(get(String.format("/resupplies/registry/search/%d?trackCode=1&boxId=EXT123456",
                savedRegistry.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format("/resupplies/registry/search/%d?trackCode=1&boxId=222333",
                savedRegistry.getId())))
                .andExpect(status().isOk());

        List<RegistryItem> items = registryItemRepo.findAllByRegistryId(savedRegistry.getId());
        Assertions.assertEquals(1, items.size());
        RegistryItem savedItem = items.get(0);
        Assertions.assertArrayEquals(new String[]{ "EXT123456", "222333" }, savedItem.getBoxesApprovedArray());
    }

    @Test
    public void approveByAlreadyApprovedBoxIdAndOrderIdTest() throws Exception {
        Registry r = new Registry();
        RegistryItem item = new RegistryItem();
        item.setApproved(false);
        item.setOrderId("1");
        item.setTrackCode("1234");
        item.setBoxesArray(new String[]{});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{"222333"});
        item.setBoxCount(2);

        Registry savedRegistry = registryRepo.saveAndFlush(r);
        item.setRegistry(savedRegistry);
        registryItemRepo.saveAndFlush(item);

        mockMvc.perform(get(String.format("/resupplies/registry/search/%d?trackCode=1&boxId=222333",
                savedRegistry.getId())))
                .andExpect(status().isOk());

        List<RegistryItem> items = registryItemRepo.findAllByRegistryId(savedRegistry.getId());
        Assertions.assertEquals(1, items.size());
        RegistryItem savedItem = items.get(0);
        Assertions.assertArrayEquals(new String[]{ "222333" }, savedItem.getBoxesApprovedArray());
    }

    @Test
    public void approveByTrackCodeForRefundWithoutBoxIds() throws Exception {
        Registry r = new Registry();
        r.setType(RegistryType.REFUND);
        RegistryItem item = new RegistryItem();
        item.setApproved(false);
        item.setOrderId("EXT1256");
        item.setTrackCode("1234");
        item.setBoxesArray(new String[]{});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{});
        item.setBoxCount(2);

        Registry savedRegistry = registryRepo.saveAndFlush(r);
        item.setRegistry(savedRegistry);
        registryItemRepo.saveAndFlush(item);

        mockMvc.perform(get(String.format("/resupplies/registry/search/%d?trackCode=EXT1256", savedRegistry.getId())))
                .andExpect(status().isOk());

        List<RegistryItem> items = registryItemRepo.findAllByRegistryId(savedRegistry.getId());
        Assertions.assertEquals(1, items.size());
        RegistryItem savedItem = items.get(0);
        Assertions.assertArrayEquals(new String[]{ "EXT1256" }, savedItem.getBoxesApprovedArray());
    }

    @Test
    public void approveByTrackCodeForUnpaidWithoutBoxIds() throws Exception {
        Registry r = new Registry();
        r.setType(RegistryType.UNPAID);
        RegistryItem item = new RegistryItem();
        item.setApproved(false);
        item.setOrderId("EXT1256");
        item.setTrackCode("1234");
        item.setBoxesArray(new String[]{});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{});
        item.setBoxCount(2);

        Registry savedRegistry = registryRepo.saveAndFlush(r);
        item.setRegistry(savedRegistry);
        registryItemRepo.saveAndFlush(item);

        mockMvc.perform(get(String.format("/resupplies/registry/search/%d?trackCode=EXT1256", savedRegistry.getId())))
                .andExpect(status().isOk());

        List<RegistryItem> items = registryItemRepo.findAllByRegistryId(savedRegistry.getId());
        Assertions.assertEquals(1, items.size());
        RegistryItem savedItem = items.get(0);
        Assertions.assertArrayEquals(new String[]{}, savedItem.getBoxesApprovedArray());
    }

    @Test
    public void approveSingleBoxUnpaidByOrderIdWhenBoxAlreadyApproved() throws Exception {
        Registry r = new Registry();
        r.setType(RegistryType.UNPAID);
        RegistryItem item = new RegistryItem();
        item.setApproved(false);
        item.setOrderId("123456");
        item.setTrackCode("1234");
        item.setBoxesArray(new String[]{ "P00123456" });
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{ "P00123456" });
        item.setBoxCount(1);

        Registry savedRegistry = registryRepo.saveAndFlush(r);
        item.setRegistry(savedRegistry);
        registryItemRepo.saveAndFlush(item);

        mockMvc.perform(get(String.format("/resupplies/registry/search/%d?trackCode=123456", savedRegistry.getId())))
                .andExpect(status().isOk());

        List<RegistryItem> items = registryItemRepo.findAllByRegistryId(savedRegistry.getId());
        Assertions.assertEquals(1, items.size());
        RegistryItem savedItem = items.get(0);
        Assertions.assertArrayEquals(new String[]{ "P00123456" }, savedItem.getBoxesApprovedArray());
    }

    @Test
    public void approveByEmptyBoxIdAndOrderIdTest() throws Exception {
        Registry r = new Registry();
        RegistryItem item = new RegistryItem();
        item.setApproved(false);
        item.setOrderId("1");
        item.setTrackCode("1234");
        item.setBoxesArray(new String[]{});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{"222333"});
        item.setBoxCount(2);

        Registry savedRegistry = registryRepo.saveAndFlush(r);
        item.setRegistry(savedRegistry);
        registryItemRepo.saveAndFlush(item);

        mockMvc.perform(get(String.format("/resupplies/registry/search/%d?trackCode=1&boxId=",
                savedRegistry.getId())))
                .andExpect(status().isOk());

        List<RegistryItem> items = registryItemRepo.findAllByRegistryId(savedRegistry.getId());
        Assertions.assertEquals(1, items.size());
        RegistryItem savedItem = items.get(0);
        Assertions.assertArrayEquals(new String[]{ "222333" }, savedItem.getBoxesApprovedArray());
    }

    @Test
    public void approveByBoxIdAndOrderIdWhenBoxIsApprovedForAnotherOrder() throws Exception {
        Registry r = new Registry();
        r.setType(RegistryType.UNPAID);
        RegistryItem item = new RegistryItem();
        item.setApproved(false);
        item.setOrderId("1");
        item.setTrackCode("1234");
        item.setBoxesArray(new String[]{});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{"222333"});
        item.setBoxCount(2);

        RegistryItem item2 = new RegistryItem();
        item2.setApproved(false);
        item2.setOrderId("2");
        item2.setTrackCode("1235");
        item2.setBoxesArray(new String[]{});
        item2.setSourceFulfillmentId(2L);
        item2.setBoxesApprovedArray(new String[]{});
        item2.setBoxCount(1);

        Registry savedRegistry = registryRepo.saveAndFlush(r);
        item.setRegistry(savedRegistry);
        item2.setRegistry(savedRegistry);
        registryItemRepo.saveAndFlush(item);
        registryItemRepo.saveAndFlush(item2);

        mockMvc.perform(get(String.format("/resupplies/registry/search/%d?trackCode=2&boxId=222333",
                savedRegistry.getId())))
                .andExpect(status().isOk());

        List<RegistryItem> items = registryItemRepo.findAllByRegistryId(savedRegistry.getId());
        Assertions.assertEquals(2, items.size());
        RegistryItem secondItemAfterSave = items.stream().filter(it -> "2".equals(it.getOrderId())).findAny().get();
        Assertions.assertArrayEquals(new String[]{}, secondItemAfterSave.getBoxesApprovedArray());
    }

    @Test
    public void approveByBoxIdAndOrderIdWhenBoxIsApprovedForAnotherOrderAndAnotherWarehouse() throws Exception {
        Registry r = new Registry();
        r.setType(RegistryType.UNPAID);
        RegistryItem item = new RegistryItem();
        item.setApproved(false);
        item.setOrderId("1");
        item.setTrackCode("1234");
        item.setBoxesArray(new String[]{});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{"222333"});
        item.setBoxCount(2);

        RegistryItem item2 = new RegistryItem();
        item2.setApproved(false);
        item2.setOrderId("2");
        item2.setTrackCode("1235");
        item2.setBoxesArray(new String[]{});
        item2.setSourceFulfillmentId(3L);
        item2.setBoxesApprovedArray(new String[]{});
        item2.setBoxCount(1);

        Registry savedRegistry = registryRepo.saveAndFlush(r);
        item.setRegistry(savedRegistry);
        item2.setRegistry(savedRegistry);
        registryItemRepo.saveAndFlush(item);
        registryItemRepo.saveAndFlush(item2);

        mockMvc.perform(get(String.format("/resupplies/registry/search/%d?trackCode=2&boxId=222333",
                savedRegistry.getId())))
                .andExpect(status().isOk());

        List<RegistryItem> items = registryItemRepo.findAllByRegistryId(savedRegistry.getId());
        Assertions.assertEquals(2, items.size());
        RegistryItem secondItemAfterSave = items.stream().filter(it -> "2".equals(it.getOrderId())).findAny().get();
        Assertions.assertArrayEquals(new String[]{ "222333" }, secondItemAfterSave.getBoxesApprovedArray());
    }

    @Test
    public void approveByBoxIdSameAsOrderIdTest() throws Exception {
        Registry r = new Registry();
        r.setType(RegistryType.UNPAID);
        RegistryItem item = new RegistryItem();
        item.setApproved(false);
        item.setOrderId("123");
        item.setTrackCode("1234");
        item.setBoxesArray(new String[]{});
        item.setSourceFulfillmentId(2L);
        item.setBoxesApprovedArray(new String[]{"222333"});
        item.setBoxCount(2);

        Registry savedRegistry = registryRepo.saveAndFlush(r);
        item.setRegistry(savedRegistry);
        registryItemRepo.saveAndFlush(item);

        mockMvc.perform(get(String.format("/resupplies/registry/search/%d?trackCode=123&boxId=123",
                savedRegistry.getId())))
                .andExpect(status().isOk());

        List<RegistryItem> items = registryItemRepo.findAllByRegistryId(savedRegistry.getId());
        Assertions.assertEquals(1, items.size());
        RegistryItem savedItem = items.get(0);
        Assertions.assertArrayEquals(new String[]{ "222333" }, savedItem.getBoxesApprovedArray());
    }
}
