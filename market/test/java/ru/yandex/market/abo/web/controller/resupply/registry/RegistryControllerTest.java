package ru.yandex.market.abo.web.controller.resupply.registry;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.abo.AbstractControllerTest;
import ru.yandex.market.abo.api.entity.resupply.registry.RegistryType;
import ru.yandex.market.abo.core.resupply.entity.Warehouse;
import ru.yandex.market.abo.core.resupply.registry.Registry;
import ru.yandex.market.abo.core.resupply.registry.RegistryItemRepo;
import ru.yandex.market.abo.core.resupply.registry.RegistryRepo;
import ru.yandex.market.abo.cpa.lms.model.DeliveryService;
import ru.yandex.market.abo.cpa.lms.repo.DeliveryServiceRepo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RegistryControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistryItemRepo registryItemRepo;

    @Autowired
    private RegistryRepo registryRepo;

    @Autowired
    private DeliveryServiceRepo deliveryServiceRepo;

    @AfterEach
    public void deleteRegistryAndItems() {
        registryItemRepo.deleteAll();
        registryRepo.deleteAll();
    }

    @Test
    public void uploadRegistryTest() throws Exception {
        var deliveryService = new DeliveryService();
        deliveryService.setReadableName("Name");
        deliveryService.setJurName("Name");
        deliveryService = deliveryServiceRepo.save(deliveryService);
        MockMultipartFile file = getFile("correct-registry.txt");
        mockMvc.perform(multipart("/resupplies/registry")
                        .file(file)
                        .flashAttr("registryForm", getRegistryForm(deliveryService.getId()))
                        .with(csrf())
                )
                .andExpect(status().isOk());

        List<Registry> registries = registryRepo.findAll();
        Assertions.assertEquals(1, registries.size());
        Registry registry = registries.get(0);
        Assertions.assertEquals("main-mock-mvc-user", registry.getUserName());
    }

    @NotNull
    private RegistryForm getRegistryForm(long deliveryServiceId) {
        var form = new RegistryForm();
        form.setDate(LocalDate.now());
        form.setType(RegistryType.REFUND);
        form.setName("Name");
        form.setDeliveryServiceId(deliveryServiceId);
        form.setWarehouse(Warehouse.SOFINO);
        return form;
    }

    @Test
    public void getRegistryTest() throws Exception {
        mockMvc.perform(get("/resupplies/registry"))
                .andExpect(status().isOk());

        List<Registry> registries = registryRepo.findAll();
        Assertions.assertEquals(0, registries.size());
    }

    private MockMultipartFile getFile(String fileName) throws IOException {
        InputStream is = getClass().getResourceAsStream("/resupply/registry/" + fileName);
        return new MockMultipartFile("file", is);
    }
}
