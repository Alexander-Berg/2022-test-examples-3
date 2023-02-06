package ru.yandex.market.tpl.core.service.usershift;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class ElectronicQueueServiceTest extends TplAbstractTest {

    private static final Long SC_ID = 686786321L;

    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final ElectronicQueueService electronicQueueService;
    private final ShiftRepository shiftRepository;

    private Shift shift1;
    private Shift shift2;

    @BeforeEach
    void init() {
        shift1 = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        shift2 = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), SC_ID);
    }

    @Test
    void checkQrCodeMatching() {
        var shift1QrCode = electronicQueueService.generateQrCodeContentForElectronicQueue(
                shift1.getSortingCenter().getId(), shift1.getShiftDate());
        var shift2QrCode = electronicQueueService.generateQrCodeContentForElectronicQueue(
                shift2.getSortingCenter().getId(), shift2.getShiftDate()
        );

        assertThat(electronicQueueService.checkQrCodeMatching(shift1.getId(), shift1QrCode)).isTrue();
        assertThat(electronicQueueService.checkQrCodeMatching(shift1.getId(), shift2QrCode)).isFalse();
        assertThat(electronicQueueService.checkQrCodeMatching(shift1.getId(), "not_matching")).isFalse();
    }

    @Test
    void convertCodeToStringIsCorrect() {
        assertThat(electronicQueueService.convertCodeToString(123456)).isEqualTo("123456");
        assertThat(electronicQueueService.convertCodeToString(-123456)).isEqualTo("123456");
        assertThat(electronicQueueService.convertCodeToString(1234567)).isEqualTo("123456");
        assertThat(electronicQueueService.convertCodeToString(-1234567)).isEqualTo("123456");
        assertThat(electronicQueueService.convertCodeToString(6)).isEqualTo("000006");
        assertThat(electronicQueueService.convertCodeToString(-6)).isEqualTo("000006");
    }

    @Test
    void getQrCodeShiftNotExist() {
        var date = LocalDate.now(clock).plusDays(1);
        var qrCode = electronicQueueService.generateShiftAndQrCode(2L, date);
        assertThat(qrCode).isNotNull();
        assertThat(shiftRepository.findByShiftDateAndSortingCenterId(date, 2L))
                .isNotEmpty();
    }

}
