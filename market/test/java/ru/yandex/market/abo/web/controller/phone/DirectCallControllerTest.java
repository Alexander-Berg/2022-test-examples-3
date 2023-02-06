package ru.yandex.market.abo.web.controller.phone;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author antipov93.
 * @date 25.01.18.
 */
public class DirectCallControllerTest {

    @Test
    public void testNormalizePhone() {
        String normalized = DirectCallController.normalizeAndValidate("8-(916)-034 69 71");
        Assertions.assertEquals("+79160346971", normalized);
    }

    @Test
    public void testInvalidPhone() throws Exception {
        assertThrows(RuntimeException.class, () ->
                DirectCallController.normalizeAndValidate("8916034697"));
    }
}
