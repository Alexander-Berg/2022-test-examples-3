package ru.yandex.market.sc.internal.controller;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.inbound.repository.BoundRegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRegistryOrderStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortableRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPartner;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.internal.test.ScTestUtils.fileContent;

@ScIntControllerTest
public class FFApiControllerV2PutInboundRegistryTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    RegistryRepository registryRepository;
    @Autowired
    RegistrySortableRepository registrySortableRepository;
    @Autowired
    BoundRegistryRepository boundRegistryRepository;

    private SortingCenter sortingCenter;
    private SortingCenterPartner sortingCenterPartner;

    @BeforeEach
    void init() {
        sortingCenterPartner = testFactory.storedSortingCenterPartner(1000, "sortingCenter-token");
        sortingCenter = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(100L)
                        .partnerName("Новый СЦ")
                        .sortingCenterPartnerId(sortingCenterPartner.getId())
                        .token(sortingCenterPartner.getToken())
                        .yandexId("6667778881")
                        .build());
    }

    @Test
    void testInboundRegistryRequestSpecialCase() {
        testInboundRegistry("ff_putInbound_registry_special_case.xml", false, false);
    }

    @Test
    void testInboundRegistryRequestNoItems() {
        testInboundRegistry("ff_putInbound_registry_no_items.xml", false, false);
    }

    @Test
    void testInboundRegistryRequestNoBoxes() {
        testInboundRegistry("ff_putInbound_registry_no_boxes.xml", false, true);
    }

    @Test
    void testInboundRegistryRequestNoNothing() {
        testInboundRegistry("ff_putInbound_registry_no_nothing.xml", true, true);
    }


    void testInboundRegistry(String fileName, boolean noNothing, boolean emptyRelations) {
        String inboundExternalId = "my_inbound_id";
        String body = String.format(fileContent("ff_putInbound.xml"), sortingCenter.getToken(), inboundExternalId,
                sortingCenter.getId());
        ScTestUtils.ffApiV2SuccessfulCall(mockMvc, body);
        var inbound = testFactory.getInbound(inboundExternalId);
        Long inboundId = inbound.getId();

        body = String.format(fileContent(fileName), sortingCenter.getToken(), inbound.getExternalId());
        ScTestUtils.ffApiV2SuccessfulCall(mockMvc, body);
        var registryList = testFactory.getRegistryByInboundId(inbound.getId());
        assertThat(registryList).hasSize(1);
        var registry = registryList.get(0);
        assertThat(registry.getExternalId()).isEqualTo("registry_external_id");
        assertThat(registry.getInbound().getId()).isEqualTo(inboundId);
        assertThat(registry.getType()).isEqualTo(RegistryType.PLANNED);
        List<RegistryOrder> orders = testFactory.getRegistryOrdersByRegistryExternalId(registry.getId());
        if (noNothing) {
            assertThat(orders).hasSize(0);
        } else {
            assertThat(orders).hasSize(4);
            assertThat(StreamEx.of(orders).filter(item -> item.getExternalId().equals("multiPlace_external_order_id"))
                    .toList()).hasSize(2);
            assertThat(StreamEx.of(orders).filter(item -> item.getStatus().equals(InboundRegistryOrderStatus.CREATED))
                    .toList()).hasSize(4);
            assertThat(StreamEx.of(orders).filter(item -> item.getPlaceId().equals("place_external_id_1"))
                    .toList()).hasSize(1);
            assertThat(StreamEx.of(orders).filter(item -> item.getPlaceId().equals("place_external_id_2"))
                    .toList()).hasSize(1);

            var multiPlaceOrderPlace1 = StreamEx.of(orders)
                    .findFirst(item -> item.getPlaceId().equals("place_external_id_1")).get();
            var multiPlaceOrderPlace2 = StreamEx.of(orders)
                    .findFirst(item -> item.getPlaceId().equals("place_external_id_2")).get();
            assertThat(multiPlaceOrderPlace1.getExternalId()).isEqualTo("multiPlace_external_order_id");
            assertThat(multiPlaceOrderPlace2.getExternalId()).isEqualTo("multiPlace_external_order_id");

            assertThat(testFactory
                    .getRegistryById(multiPlaceOrderPlace1.getRegistryId()).getExternalId())
                    .isEqualTo("registry_external_id");

            assertThat(testFactory
                    .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                    .getInbound().getId()).isEqualTo(inboundId);
            assertThat(testFactory
                    .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                    .getInbound().getId()).isEqualTo(inboundId);
            if (emptyRelations) {
                assertThat(multiPlaceOrderPlace1.getPalletId()).isNull();
                assertThat(multiPlaceOrderPlace2.getPalletId()).isNull();
            } else {
                assertThat(multiPlaceOrderPlace1.getPalletId()).isEqualTo("first_pallet_id");
                assertThat(multiPlaceOrderPlace2.getPalletId()).isEqualTo("first_pallet_id");
            }

            var regularOrder1 = StreamEx.of(orders)
                    .findFirst(item -> item.getPlaceId().equals("regular_order_external_id")).get();
            assertThat(regularOrder1.getPlaceId()).isEqualTo("regular_order_external_id");
            assertThat(testFactory
                    .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                    .getInbound().getId()).isEqualTo(inboundId);
            if (emptyRelations) {
                assertThat(regularOrder1.getPalletId()).isNull();
            } else {
                assertThat(regularOrder1.getPalletId()).isEqualTo("first_pallet_id");
            }
            assertThat(testFactory
                    .getRegistryById(regularOrder1.getRegistryId()).getExternalId())
                    .isEqualTo("registry_external_id");

            var regularOrder2 = StreamEx.of(orders)
                    .findFirst(item -> item.getPlaceId().equals("regular_order_external_id_2")).get();
            assertThat(regularOrder2.getPlaceId()).isEqualTo("regular_order_external_id_2");
            assertThat(testFactory
                    .getRegistryById(multiPlaceOrderPlace1.getRegistryId())
                    .getInbound().getId()).isEqualTo(inboundId);
            assertThat(testFactory
                    .getRegistryById(regularOrder2.getRegistryId()).getExternalId())
                    .isEqualTo("registry_external_id");
            if (emptyRelations) {
                assertThat(regularOrder1.getPalletId()).isNull();
            } else {
                assertThat(regularOrder2.getPalletId()).isEqualTo("second_pallet_id");
            }
        }
    }
}
