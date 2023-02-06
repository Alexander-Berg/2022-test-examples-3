package ru.yandex.market.tpl.core.external.kiosk;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.util.JacksonUtil;
import ru.yandex.market.tpl.core.external.kiosk.dto.KioskProgramWrapper;
import ru.yandex.market.tpl.core.external.kiosk.dto.KioskSession;
import ru.yandex.market.tpl.core.external.kiosk.dto.KioskSessionWrapper;

import static org.assertj.core.api.Assertions.assertThat;

public class KioskObjectsRequestTest {

    @Test
    void testSessionDeserialization() throws Exception {
        KioskSessionWrapper kioskSessionWrapper = JacksonUtil.fromString(
                IOUtils.toString(
                        this.getClass().getResourceAsStream("/kiosk/session_response.json"),
                        StandardCharsets.UTF_8
                ),
                KioskSessionWrapper.class
        );
        assertThat(kioskSessionWrapper).isNotNull();
        assertThat(kioskSessionWrapper.getSession()).isNotNull();
        KioskSession kioskSession = kioskSessionWrapper.getSession();
        assertThat(kioskSession.getSessionExams()).hasSize(1);
        KioskSession.KioskSessionExam exam = kioskSession.getSessionExams().get(0);
        assertThat(exam.getScore())
                .isEqualTo(100);
        assertThat(exam.getMaxScore())
                .isEqualTo(100);
    }

    @Test
    void testProgramsDeserialization() throws Exception {
        KioskProgramWrapper kioskSessionWrapper = JacksonUtil.fromString(
                IOUtils.toString(
                        this.getClass().getResourceAsStream("/kiosk/program_response.json"),
                        StandardCharsets.UTF_8
                ),
                KioskProgramWrapper.class
        );
        assertThat(kioskSessionWrapper).isNotNull();
        assertThat(kioskSessionWrapper.getPrograms()).isNotNull();
        assertThat(kioskSessionWrapper.getPrograms()).isNotEmpty();
        assertThat(kioskSessionWrapper.getPrograms().get(0).getUuid()).isEqualTo("1111");
    }
}
