package ru.yandex.market.sc.internal.controller.manual;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.sc.ScCargoUnit;
import ru.yandex.market.logistics.les.sc.ScCargoUnitStatus;
import ru.yandex.market.logistics.les.sc.ScSegmentStatusesEvent;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.cargo.Cargo;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.place.model.CargoSegment;
import ru.yandex.market.sc.core.domain.place.model.SendStatusesSegmentDto;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sqs.SendCargoUpdateStatusHistoryToSqsService;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author mors741
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ScIntControllerTest
public class ManualCargoControllerTest {

    private static final long UID = 123L;

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final ObjectMapper objectMapper;
    @MockBean
    JmsTemplate jmsTemplate;
    @Autowired
    SqsQueueProperties sqsQueueProperties;
    SortingCenter sortingCenter;
    Warehouse warehouse;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        warehouse = testFactory.storedWarehouse();

        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);

        Mockito.when(sqsQueueProperties.getOutQueue()).thenReturn("sc_out");
        Mockito.clearInvocations(jmsTemplate);
    }

    @Test
    @SneakyThrows
    void createReturn() {
        var sc = testFactory.storedSortingCenter(84L);

        String segmentUid = mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/cargo/saveReturn")
                                .param("scId", sc.getId().toString())
                                .param("orderBarcode", "o1")
                                .param("placeBarcode", "p1")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Place place = testFactory.findPlace("o1", "p1", sc);
        assertThat(segmentUid)
                .contains(place.getSegmentUid()); // mockMvc возвращает значение в ковычках
    }

    @Test
    @SneakyThrows
    void updateReturn() {
        var sc = testFactory.storedSortingCenter(84L);
        Place place = testFactory.createOrderForToday(sc).acceptPlaces().getPlace();

        String segmentUid = mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/cargo/saveReturn")
                                .param("scId", sc.getId().toString())
                                .param("orderBarcode", place.getExternalId())
                                .param("placeBarcode", place.getMainPartnerCode())
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        place = testFactory.updated(place);
        assertThat(segmentUid)
                .contains(place.getSegmentUid()); // mockMvc возвращает значение в ковычках
        assertThat(place.getSortableStatus())
                .isEqualTo(SortableStatus.ACCEPTED_RETURN);
    }

    @Test
    void sendOrderFfStatusToLrm() throws Exception {
        var cargoList = new ArrayList<Cargo>();
        User user = testFactory.storedUser(sortingCenter, 9999);
        for (int i = 0; i < 10; i++) {
            var cargo = new Cargo("segment-uuid-" + i, "cargo-unit-id-" + i,
                    "P" + i, "wh-1", "o-barcode-" + i);
            cargoList.add(cargo);
            testFactory.createReturn(cargo, sortingCenter, user);
        }

        var cargoSegments = cargoList.stream()
                .map(Cargo::segmentUuid)
                .map(s -> new CargoSegment(s, null))
                .toList();
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/cargo/sendStatusesByOrder")
                                .content(objectMapper.writeValueAsString(new SendStatusesSegmentDto(
                                        cargoSegments)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Mockito.verify(jmsTemplate, Mockito.times(10)).convertAndSend(Mockito.any(String.class),
                Mockito.any(Event.class));
    }
}
