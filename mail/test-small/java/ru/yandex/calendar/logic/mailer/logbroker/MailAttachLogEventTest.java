package ru.yandex.calendar.logic.mailer.logbroker;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MailAttachLogEventTest {
    @Test
    public void emptyMid() {
        val line = "tskv\ttskv_format=mail-mxbackcorp-attach-log\tuid=1120000000045634\tsuid=1120000000173635\tmid=\t" +
                "fidType=\tfidSpecType=0\thid=1.2\tfileType=image/png\tname=2019-07-15_16-13-52.png\tsize=183378\t" +
                "stid=320.mail:0.E1336921:1678060284203940068131679467494\tunixtime=1563197341\tsessionId=kN9Ly4a1C1-hdnKxMaB";
        val optEvent = MailAttachLogEvent.parse(line);
        assertThat(optEvent).isEmpty();
    }

    @Test
    public void filledMid() {
        val line = "tskv\ttskv_format=mail-mxbackcorp-attach-log\tuid=1120000000045634\tsuid=1120000000173635\tmid=123\t" +
                "fidType=\tfidSpecType=0\thid=1.2\tfileType=image/png\tname=2019-07-15_16-13-52.png\tsize=183378\t" +
                "stid=320.mail:0.E1336921:1678060284203940068131679467494\tunixtime=1563197341\tsessionId=kN9Ly4a1C1-hdnKxMaB";
        val optEvent = MailAttachLogEvent.parse(line);
        assertThat(optEvent).isPresent();
        val event = optEvent.get();
        assertThat(event).isInstanceOf(MailAttachLogEvent.class);
        assertThat(event.mid).isEqualTo(123);
    }
}
