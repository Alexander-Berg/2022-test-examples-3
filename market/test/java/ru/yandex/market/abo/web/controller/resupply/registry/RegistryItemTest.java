package ru.yandex.market.abo.web.controller.resupply.registry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.resupply.registry.RegistryType;
import ru.yandex.market.abo.core.resupply.registry.LrmService;
import ru.yandex.market.abo.core.resupply.registry.Registry;
import ru.yandex.market.abo.core.resupply.registry.RegistryItem;
import ru.yandex.market.abo.core.resupply.registry.RegistryItemRepo;
import ru.yandex.market.abo.core.resupply.registry.RegistryRepo;
import ru.yandex.market.abo.core.resupply.registry.RegistryRequestService;
import ru.yandex.market.abo.core.resupply.registry.RegistrySendingService;
import ru.yandex.market.abo.core.resupply.registry.RegistryValidationService;
import ru.yandex.market.abo.cpa.lms.DeliveryServiceManager;
import ru.yandex.market.abo.util.db.toggle.DbToggleService;
import ru.yandex.market.abo.util.db.toggle.ToggleService;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.logistics.lrm.client.model.ReturnBox;
import ru.yandex.market.logistics.lrm.client.model.SearchReturn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ContextConfiguration("classpath:registry-request-service-test-mocks.xml")
public class RegistryItemTest extends EmptyTest {

    private RegistryRequestService registryRequestService;
    private RegistryItemRepo registryItemRepo;
    private LrmService lrmService;

    private static final String REGISTRY =
            "11,track1\n" +
                    "22,track2\n" +
                    "22,track5\n" +
                    "11,track3\n" +
                    "44,track4\n" +
                    "44,track6";

    @BeforeEach
    void init() {
        registryItemRepo = Mockito.mock(RegistryItemRepo.class);
        lrmService = Mockito.mock(LrmService.class);
        registryRequestService = new RegistryRequestService(
                Mockito.mock(FulfillmentWorkflowClientApi.class),
                Mockito.mock(RegistryValidationService.class),
                Mockito.mock(RegistrySendingService.class),
                registryItemRepo,
                Mockito.mock(RegistryRepo.class),
                Mockito.mock(DeliveryServiceManager.class),
                Mockito.mock(ToggleService.class),
                Mockito.mock(Environment.class),
                lrmService,
                Mockito.mock(DbToggleService.class)
        );
    }

    @Test
    void uploadUnpaidRegistry() {
        Registry registry = getRegistry();
        Collection<RegistryItem> expectedItems = prepareItemList(registry).stream()
                .collect(Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(RegistryItem::getOrderId))));

        MultipartFile file = new MockMultipartFile("_", REGISTRY.getBytes());
        registryRequestService.upload(registry, file, "testUserName");

        Mockito.verify(registryItemRepo).saveAll(expectedItems);
    }

    @Test
    void enrichUnpaidRegistry() {
        Registry registry = getRegistryWithItems();
        registry.getItems().get(0).setOrderId(null);
        assertThat(registry.getItems().get(0).getOrderId()).isEqualTo(null);

        SearchReturn searchReturn1 = new SearchReturn();
        searchReturn1.setOrderExternalId("11");
        searchReturn1.setBoxes(List.of(getBox("track1"), getBox("track3")));

        when(lrmService.searchByBoxes(any())).thenReturn(List.of(searchReturn1));

        registryRequestService.enrichOrderIds(registry.getItems());

        assertThat(registry.getItems().get(0).getOrderId()).isEqualTo("11");
    }

    private ReturnBox getBox(String boxId) {
        ReturnBox box3 = new ReturnBox();
        box3.setExternalId(boxId);
        return box3;
    }

    private Registry getRegistry() {
        Registry registry = new Registry();
        registry.setName("TestRegistry");
        registry.setType(RegistryType.UNPAID);
        registry.setDate(LocalDate.now());
        return registry;
    }

    private Registry getRegistryWithItems() {
        Registry registry = new Registry();
        registry.setName("TestRegistry");
        registry.setType(RegistryType.UNPAID);
        registry.setDate(LocalDate.now());

        registry.setItems(new ArrayList<>(prepareItemList(registry)));

        return registry;
    }

    private Collection<RegistryItem> prepareItemList(Registry registry) {

        RegistryItem item = new RegistryItem();
        item.setOrderId("11");
        item.setTrackCode("11");
        item.setRegistry(registry);
        item.setBoxesArray(new String[]{"track1", "track3"});
        item.setUpdatedAt(registry.getCreationTime());

        RegistryItem item2 = new RegistryItem();
        item2.setOrderId("22");
        item2.setTrackCode("22");
        item2.setRegistry(registry);
        item2.setBoxesArray(new String[]{"track2", "track5"});
        item2.setUpdatedAt(registry.getCreationTime());

        RegistryItem item4 = new RegistryItem();
        item4.setOrderId("44");
        item4.setTrackCode("44");
        item4.setBoxesArray(new String[]{"track4", "track6"});
        item4.setRegistry(registry);
        item4.setUpdatedAt(registry.getCreationTime());

        return List.of(item, item2, item4);
    }
}
