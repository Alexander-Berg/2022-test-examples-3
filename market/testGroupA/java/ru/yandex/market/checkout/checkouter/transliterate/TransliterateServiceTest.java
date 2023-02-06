package ru.yandex.market.checkout.checkouter.transliterate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Nicolai Iusiumbeli
 * date: 20.10.16
 */
public class TransliterateServiceTest {

    private static final int MAX_SIZE = Integer.MAX_VALUE;

    @Test
    public void transliterate() throws Exception {
        TransliterateService service = new TransliterateService();
        assertEquals("34Yuvyujfyv 44olbx", service.transliterate("34Ювюйфыв 44olbx", MAX_SIZE));
        assertEquals("Teatral'naya alleya", service.transliterate("Театральная аллея", MAX_SIZE));
        assertEquals("d2k3, d.26 vl.134", service.transliterate("д2к3, д.26 вл.134", MAX_SIZE));
        assertEquals("kv.112", service.transliterate("кв.112", MAX_SIZE));
        assertEquals("Konstantinov Evgenij Arsen'evich", service.transliterate(
                "Константинов Евгений Арсеньевич",
                MAX_SIZE));
        assertEquals("Yulij Tsezar'", service.transliterate("Юлий Цезарь", MAX_SIZE));
        assertEquals("Yulij Tsez", service.transliterate("Юлий Цезарь", 10));
    }

}
