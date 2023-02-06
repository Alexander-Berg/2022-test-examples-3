package ru.yandex.market.tpl.core.domain.shift;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.routing.AdditionalRoutingParamDto;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftManagerValidatorTest {

    private static final LocalDate NOW = LocalDate.now();
    private static final long SORTING_CENTER_ID = 1L;
    private static final Set<Long> USER_IDS = Set.of(1L, 2L);
    private static final AdditionalRoutingParamDto ADDITIONAL_ROUTING_PARAMETER = new AdditionalRoutingParamDto(
            SORTING_CENTER_ID,
            USER_IDS,
            NOW
    );
    @InjectMocks
    private ShiftManagerValidator shiftManagerValidator;
    @Mock
    private ShiftRepository shiftRepository;

    @DisplayName("Валидация и получения смены")
    @Test
    public void validateAndFindShiftTest() {
        when(shiftRepository.findByShiftDateAndSortingCenterId(eq(NOW), anyLong()))
                .thenReturn(Optional.of(new Shift()));

        Shift shift = shiftManagerValidator.validateAndFindShift(ADDITIONAL_ROUTING_PARAMETER);

        assertNotNull(shift);
    }

    @DisplayName("Исключение при поиске смены, когда смена не найдена")
    @Test
    public void validateAndFindShiftExceptionTest() {
        when(shiftRepository.findByShiftDateAndSortingCenterId(eq(NOW), anyLong()))
                .thenReturn(Optional.empty());

        TplInvalidParameterException assertThrows = assertThrows(
                TplInvalidParameterException.class,
                () -> shiftManagerValidator.validateAndFindShift(ADDITIONAL_ROUTING_PARAMETER)
        );

        String message = assertThrows.getMessage();
        String exceptedMessage = String.format(
                "Не найдено смены для СЦ %d на дату %s",
                ADDITIONAL_ROUTING_PARAMETER.getSortingCenterId(),
                ADDITIONAL_ROUTING_PARAMETER.getDate()
        );
        assertEquals(message, exceptedMessage);
    }
}
