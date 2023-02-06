package ru.yandex.market.checkout.checkouter.balance;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.balance.service.BalanceUtils;

/**
 * Created by disproper on 17/06/30.
 */
public class BalanceUtilsTest {

    @Test
    public void testCutToCharsLong() {
        String title = "Материнская плата ASUS PRIME H270-PLUS, LGA1151, H270, 4*DDR4, D-Sub+DVI+HDMI, CrossFireX, " +
                "SATA3 + RAID, Audio, Gb LAN, USB 3.0*8, USB 2.0*6, COM*1 header (w/o cable), ATX";
        String cutTitle = BalanceUtils.cutToChars(title);
        Assertions.assertEquals(cutTitle, "Материнская плата ASUS PRIME H270-PLUS, LGA1151, H270, 4*DDR4, " +
                "D-Sub+DVI+HDMI, CrossFireX, SATA3 + RAID, Audio, Gb LAN, USB 3...");
    }

    @Test
    public void testCutToCharsShort() {
        String title = "Материнская плата ASUS PRIME";
        String cutTitle = BalanceUtils.cutToChars(title);
        Assertions.assertEquals(cutTitle, "Материнская плата ASUS PRIME");
    }

    @Test
    public void testCutToBytes() {
        String title = "Материнская плата ASUS PRIME H270-PLUS, LGA1151, H270, 4*DDR4, D-Sub+DVI+HDMI, CrossFireX, " +
                "SATA3 + RAID, Audio, Gb LAN, USB 3.0*8, USB 2.0*6, COM*1 header (w/o cable), ATX";
        String cutTitle = BalanceUtils.cutToBytes(title);
        Assertions.assertEquals(cutTitle, "Материнская плата ASUS PRIME H270-PLUS, LGA1151, H270, 4*DDR4, " +
                "D-Sub+DVI+HDMI, CrossFireX, SATA3 + RAID, Audi...");
    }

    @Test
    public void testCutToBytesShort() {
        String title = "Материнская плата ASUS PRIME";
        String cutTitle = BalanceUtils.cutToChars(title);
        Assertions.assertEquals(cutTitle, "Материнская плата ASUS PRIME");
    }
}
