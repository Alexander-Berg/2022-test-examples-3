package ru.yandex.common.mining.listparser;

import junit.framework.TestCase;
import org.junit.Test;
import static ru.yandex.common.mining.listparser.ListParser.extract;

/**
 * @author Dmitry Kachmar dkachmar@yandex-team.ru
 *         Date: 11.10.2007 22:18:24
 */
public class ListParserTest extends TestCase {
    @Test
    public void testListParser() {
        assertEquals("[TrackID, стабилизатор изображения и видеоизображения, " +
                     "BestPic - 9 снимков за сек. одним нажатием клавиши]",
                extract("TrackID; стабилизатор изображения и видеоизображения; " +
                        "BestPic - 9 снимков за сек. одним нажатием клавиши").toString());
        assertEquals("[GSM, EDGE]", extract("GSM/EDGE").toString());
        assertEquals("[Nothing]", extract("Nothing").toString());
        assertEquals("[DK-87, DK-89, HM-78, HM-96]",
                extract("DK-87/89/HM-78/96").toString());
        assertEquals("[Z-810, Z-820, Z-830, Z-840]",
                extract("Z-810/820/830/840").toString());
        /*
        assertEquals("[Defender DCR mini, Defender DCR maxi, Defender DCR size 25, Defender DCR medium]",
                extract("Defender DCR mini, maxi, size 25, medium").toString());
        */
        assertEquals("[FO-145, FO-245, UX-105, UX-175]", extract("FO-145/245/UX-105/175").toString());
        /*
        assertEquals("[Defender CAS-221, Defender CAS-421, Defender CAS-611]",
                extract("Defender CAS-221, CAS-421, CAS-611").toString());
        assertEquals("[Defender UHD251, Defender UHD351, Defender UHD451, Defender UHD551, Defender UHD651, Defender UHD751]",
                extract("Defender UHD251, UHD351, UHD451, UHD551, UHD651, UHD751").toString());
        */
        assertEquals("[FO-455, UX-256]",
                extract("FO-455/UX-256").toString());
        assertEquals("[SF-2014, SF-2114]",
                extract("SF-2014/2114").toString());
        /*
        assertEquals("[Defender C-20, Defender C-22CC, Defender C-10, " +
                     "Defender C-16, Defender C-22SL, Defender MINI STEGO, " +
                     "Defender MINI C-5, Defender MINI C-007, Defender MINI C-270A, " +
                     "Defender MINI C-270CC]",
                extract("Defender C-20, C-22CC, C-10, C-16, C-22SL, MINI STEGO/ C-5, C-007, C-270A, C-270CC").toString());
        assertEquals("[GSM (1800), GSM (2000), GSM (3000), EDGE (1800), EDGE (2000), EDGE (3000)]",
                extract("GSM/EDGE (1800/2000/3000)").toString());
        */
    }
}
