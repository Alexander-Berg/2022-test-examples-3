package ru.yandex.market.sc.api.features;

import java.time.Clock;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.tpl.common.util.exception.TplErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SortableBarcodeFlowTest extends BaseApiControllerTest {

    @MockBean
    Clock clock;
    SortingCenter sortingCenter;
    Cell cell;

    private TestControllerCaller controllerCaller;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        cell = testFactory.storedActiveCell(sortingCenter);
        testFactory.storedUser(sortingCenter, UID, UserRole.STOCKMAN);
        testFactory.setupMockClock(clock);
        controllerCaller = TestControllerCaller.createCaller(mockMvc);
    }

    @Test
    @SneakyThrows
    @DisplayName("Принимаем коробку по баркоду плейса")
    void acceptPlaceByPlaceBarcodeTest() {
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").places("o1p1", "o1p2").build())
                .get();
        var requestDto = new AcceptOrderRequestDto("o1", "o1p1");
        var acceptResp = controllerCaller.acceptOrder(requestDto)
                .andExpect(status().is2xxSuccessful());
        var acceptDtoResp = readContentAsClass(acceptResp, ApiOrderDto.class);

        var requestDto2 = new AcceptOrderRequestDto("o1p1", null);
        var acceptResp2 = controllerCaller.acceptOrder(requestDto2)
                .andExpect(status().is2xxSuccessful());
        var acceptDtoResp2 = readContentAsClass(acceptResp2, ApiOrderDto.class);
        assertThat(acceptDtoResp).isEqualTo(acceptDtoResp2);
    }

    @Test
    @SneakyThrows
    @DisplayName("Принимаем коробку по неуникальному баркоду плейса")
    void acceptPlaceByPlaceBarcodeTest2() {
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").places("p1", "p2").build())
                .get();
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").places("p1", "o2p2").build())
                .get();
        var requestDto = new AcceptOrderRequestDto("o1", "p1");
        var acceptResp = controllerCaller.acceptOrder(requestDto)
                .andExpect(status().is2xxSuccessful());
        var acceptDtoResp = readContentAsClass(acceptResp, ApiOrderDto.class);

        var requestDto2 = new AcceptOrderRequestDto("p1", null);
        var acceptResp2 = controllerCaller.acceptOrder(requestDto2)
                .andExpect(status().is2xxSuccessful());
        var acceptDtoResp2 = readContentAsClass(acceptResp2, ApiOrderDto.class);
        assertThat(acceptDtoResp).isNotEqualTo(acceptDtoResp2);
    }

}
