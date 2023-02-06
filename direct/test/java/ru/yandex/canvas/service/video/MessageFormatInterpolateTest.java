package ru.yandex.canvas.service.video;

import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.service.TankerKeySet;

import static org.junit.Assert.assertEquals;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;

@RunWith(SpringJUnit4ClassRunner.class)
public class MessageFormatInterpolateTest {

    @Test
    public void checkInterpolateTest() {
        setLocale(Locale.forLanguageTag("ru"));

        String val = TankerKeySet.VIDEO_VALIDATION_MESSAGES.interpolate("incorrect_image_width", 12000, 1024, 2048);

        assertEquals(val, "Изображение должно быть шириной от 1024 до 2048 пикселей.");
    }

}
