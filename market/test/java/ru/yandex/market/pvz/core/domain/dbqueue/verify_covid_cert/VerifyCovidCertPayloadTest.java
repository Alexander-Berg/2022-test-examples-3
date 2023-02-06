package ru.yandex.market.pvz.core.domain.dbqueue.verify_covid_cert;

import java.nio.file.Files;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.tpl.common.util.TplObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;

class VerifyCovidCertPayloadTest {

    @Test
    @SneakyThrows
    void shouldDeserialize() {
        VerifyCovidCertPayload payload = TplObjectMappers.TPL_DB_OBJECT_MAPPER.readValue(
                new String(Files.readAllBytes(
                        new ClassPathResource("vaccination/vaccination.json").getFile().toPath()
                )),
                VerifyCovidCertPayload.class);
        assertThat(payload).isNotNull();
    }
}
