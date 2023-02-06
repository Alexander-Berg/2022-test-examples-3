package ru.yandex.market.sc.internal.controller.external;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.internal.test.ScTestUtils.fileContent;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LogisticsSortingCenterControllerTest {

    private final long DELIVERY_SERVICE_UID_START = 1100000000000000L;

    private final MockMvc mockMvc;
    private final TestFactory testFactory;

    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.IS_DROPOFF, true);
    }

    @Test
    void createDropOff() throws Exception {
        testFactory.storedCourier(DELIVERY_SERVICE_UID_START + 258317);

        mockMvc.perform(post("/v1/logistics/sorting-centers/drop-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(fileContent("external/create_drop_off.json"), "2895849", "45678")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(fileContent("external/create_drop_off.json"), "2895849", "45678")));

        mockMvc.perform(post("/v1/logistics/sorting-centers/drop-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(fileContent("external/create_drop_off.json"), "2895850", "45679")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(fileContent("external/create_drop_off.json"), "2895850", "45679")));
    }

    @Test
    void createDropOffAndCheckCreatedCell() throws Exception {
        testFactory.storedCourier(DELIVERY_SERVICE_UID_START + 258317);

        mockMvc.perform(post("/v1/logistics/sorting-centers/drop-off")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(fileContent("external/create_drop_off.json"), "2895849", "45678")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(fileContent("external/create_drop_off.json"), "2895849", "45678")));

        List<Cell> allCells = testFactory.findAllCells();
        assertThat(allCells.size()).isEqualTo(1);

        Cell cell = allCells.get(0);
        assertThat(cell.getScNumber()).isEqualTo("O");
        assertThat(cell.getStatus()).isEqualTo(CellStatus.ACTIVE);
        assertThat(cell.getType()).isEqualTo(CellType.COURIER);
        assertThat(cell.getCourierId()).isEqualTo(DELIVERY_SERVICE_UID_START + 258317);
    }

    @Test
    void createDropOffIdempotentByCampaignId() throws Exception {
        testFactory.storedCourier(DELIVERY_SERVICE_UID_START + 258317);

        mockMvc.perform(post("/v1/logistics/sorting-centers/drop-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(fileContent("external/create_drop_off.json"), "2895849", "45678")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(fileContent("external/create_drop_off.json"), "2895849", "45678")));

        mockMvc.perform(post("/v1/logistics/sorting-centers/drop-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(fileContent("external/create_drop_off.json"), "2895849", "45678")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(fileContent("external/create_drop_off.json"), "2895849", "45678")));
    }

    @Test
    void badRequestForTheSameDeliveryServiceId() throws Exception {
        testFactory.storedCourier(DELIVERY_SERVICE_UID_START + 258317);

        mockMvc.perform(post("/v1/logistics/sorting-centers/drop-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(fileContent("external/create_drop_off.json"), "2895849", "45678")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(fileContent("external/create_drop_off.json"), "2895849", "45678")));

        mockMvc.perform(post("/v1/logistics/sorting-centers/drop-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(fileContent("external/create_drop_off.json"), "2895850", "45678")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findInventoryItemByPlaceBarcodeThenSuccess() throws Exception {
        String place1 = "p1";
        String place2 = "p2";
        var order = testFactory.createForToday(order(sortingCenter).places(place1, place2).build())
                .accept().sort().ship().get();

        String expected = String.format(
                fileContent("external/find_inventory_item_by_place_barcode.json"),
                order.getExternalId(), place1, place2
        );
        mockMvc.perform(
                get("/v1/logistics/sorting-centers/" + sortingCenter.getId() + "/find-inventory-item-by-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("barcodes", place1))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected));
    }

    @Test
    void findInventoryItemByExternalIdBarcodeThenSuccess() throws Exception {
        String place1 = "p1";
        String place2 = "p2";
        var order = testFactory.createForToday(order(sortingCenter).places(place1, place2).build())
                .accept().sort().ship().get();

        String expected = String.format(
                fileContent("external/find_inventory_item_by_place_barcode.json"),
                order.getExternalId(), place1, place2
        );
        mockMvc.perform(
                get("/v1/logistics/sorting-centers/" + sortingCenter.getId() + "/find-inventory-item-by-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("barcodes", order.getExternalId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected));
    }

    @Test
    void whenNotFoundInventoryItem() throws Exception {
        String place1 = "p1";
        mockMvc.perform(
                get("/v1/logistics/sorting-centers/" + sortingCenter.getId() + "/find-inventory-item-by-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("barcodes", place1))
                .andExpect(status().isNotFound());
    }

}
