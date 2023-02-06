package ru.yandex.market.tpl.common.util.security;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourierQRCodeUtilTest {

    @Test
    void encryptQrCode() {
        var courierUid = 23L;
        var shipmentDate = LocalDate.parse("2021-10-25");
        var randomNumber = ThreadLocalRandom.current().nextLong();

        QrCode qrCode = QrCode.of(randomNumber, shipmentDate, courierUid);
        var cipherText = QRCodeUtil.encryptQrCode(qrCode, "password", "123");

        assertThat(Base64.isBase64(cipherText)).isTrue();
        assertThat(cipherText).isNotEqualTo(qrCode.format());
    }

    @Test
    void decryptQrCode() {
        var courierUid = 24L;
        var shiftDate = LocalDate.parse("2021-10-25");
        var randomNumber = ThreadLocalRandom.current().nextLong();

        QrCode qrCode = QrCode.of(randomNumber, shiftDate, courierUid);
        var cipherText = QRCodeUtil.encryptQrCode(qrCode, "password", "123");
        var decryptQrCode = QRCodeUtil.decryptQrCode(cipherText, "password", "123");

        assertThat(Base64.isBase64(cipherText)).isTrue();
        assertThat(decryptQrCode.format()).isEqualTo(qrCode.format());
        assertThat(decryptQrCode.getUid()).isEqualTo(courierUid);
        assertThat(decryptQrCode.getShiftTime()).isEqualTo(shiftDate.toString());
        assertThat(decryptQrCode.getRandomNumber()).isEqualTo(randomNumber);
    }
}
