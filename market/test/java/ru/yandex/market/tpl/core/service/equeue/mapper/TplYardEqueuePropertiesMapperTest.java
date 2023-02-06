package ru.yandex.market.tpl.core.service.equeue.mapper;

import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.yard.client.dto.service.Param;
import ru.yandex.market.logistics.yard.client.dto.service.ServiceParamDto;
import ru.yandex.market.logistics.yard.client.dto.service.ServiceParamListDto;
import ru.yandex.market.tpl.core.external.TplExternalConstants;
import ru.yandex.market.tpl.core.service.equeue.model.EqueuePropertiesStateDto;

import static org.assertj.core.api.Assertions.assertThat;

class TplYardEqueuePropertiesMapperTest {

    private final TplYardEqueuePropertiesMapper mapper = new TplYardEqueuePropertiesMapper();

    @Test
    void map() {
        //given
        long expectedParkingCapacity = 10L;
        long expectedQtyFreePlacesToNextSlot = 2L;
        long expectedSlotDurationInMinutes = 5L;
        long expectedRatePassLatecomers = 9L;
        long expectedScId = 777L;

        //when
        ServiceParamListDto paramListDto = mapper.map(EqueuePropertiesStateDto.builder()
                .scId(expectedScId)
                .parkingCapacity(expectedParkingCapacity)
                .qtyFreePlacesToNextSlot(expectedQtyFreePlacesToNextSlot)
                .slotDurationInMinutes(expectedSlotDurationInMinutes)
                .ratePassLatecomers(expectedRatePassLatecomers)
                .build());

        //then
        assertThat(paramListDto).isNotNull();
        assertThat(paramListDto.getServiceParams()).hasSize(1);

        ServiceParamDto serviceParamDto = paramListDto.getServiceParams().get(0);

        assertThat(serviceParamDto.getCapacity()).isEqualTo(expectedParkingCapacity);
        assertThat(serviceParamDto.getServiceId()).isEqualTo(expectedScId);
        assertThat(serviceParamDto.getParams()).hasSize(2);

        Optional<Param> paramO = findParam(serviceParamDto, TplExternalConstants.Yard.ELEMENTS_IN_ALMOST_EMPTY_QUEUE);
        assertThat(paramO.isPresent()).isTrue();
        assertThat(paramO.get().getValue()).isEqualTo(String.valueOf(expectedParkingCapacity - expectedQtyFreePlacesToNextSlot));

        paramO = findParam(serviceParamDto, TplExternalConstants.Yard.MINUTES_BEFORE_SLOT_TO_ARRIVE_NOT_TOO_EARLY);
        assertThat(paramO.isPresent()).isTrue();
        assertThat(paramO.get().getValue()).isEqualTo(String.valueOf(expectedSlotDurationInMinutes));

        assertThat(serviceParamDto.getPriorityFunctionParams()).hasSize(1);
        assertThat(serviceParamDto.getPriorityFunctionParams().get(0).getKey()).isEqualTo(TplExternalConstants.Yard.SKIP_N_CLIENTS);
        assertThat(serviceParamDto.getPriorityFunctionParams().get(0).getValue()).isEqualTo(String.valueOf(expectedRatePassLatecomers));
    }

    private Optional<Param> findParam(ServiceParamDto serviceParamDto, String key) {
        return serviceParamDto.getParams()
                .stream()
                .filter(param -> Objects.equals(param.getKey(), key))
                .findFirst();
    }
}
