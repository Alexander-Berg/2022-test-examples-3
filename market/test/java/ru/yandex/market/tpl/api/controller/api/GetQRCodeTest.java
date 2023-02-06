package ru.yandex.market.tpl.api.controller.api;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.util.security.QRCodeUtil;
import ru.yandex.market.tpl.common.util.security.QrCode;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GetQRCodeTest {

    private final String password = "password";

    private final String salt = "123";

    @Test
    void getQRCode() {
        LocalDate date = LocalDate.now();
        Long uid = 318536736L;
        Long rand = 535135235L;

        var qrCode = QrCode.of(rand, date, uid);
        String cipherText = QRCodeUtil.encryptQrCode(qrCode, password, salt);
        assertThat(cipherText).isNotNull();
        assertThat(Base64.isBase64(cipherText)).isTrue();

        var decryptQrCode = QRCodeUtil.decryptQrCode(cipherText, password, salt);
        assertThat(decryptQrCode.format()).isEqualTo(qrCode.format());
    }
}
