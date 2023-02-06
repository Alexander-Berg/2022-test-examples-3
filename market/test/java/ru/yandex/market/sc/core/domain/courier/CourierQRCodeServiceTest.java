package ru.yandex.market.sc.core.domain.courier;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.util.security.QRCodeUtil;
import ru.yandex.market.tpl.common.util.security.QrCode;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CourierQRCodeServiceTest {

    private final TestFactory testFactory;
    private final CourierQRCodeService courierQRCodeService;
    private final Clock clock;

    @Test
    void decryptCourierUid() {
        var courier = testFactory.storedCourier(23L);
        var shipmentDate = LocalDate.now(clock);
        var randomNumber = ThreadLocalRandom.current().nextLong();

        var qrCode = QrCode.of(randomNumber, shipmentDate, courier.getId());
        var cipherText = QRCodeUtil.encryptQrCode(qrCode, "none", "none");
        var courierUid = courierQRCodeService.decryptCourierUid(cipherText);

        assertThat(courierUid).isEqualTo(courier.getId());
    }
}
