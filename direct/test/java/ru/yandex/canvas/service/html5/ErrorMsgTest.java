package ru.yandex.canvas.service.html5;

import java.util.List;
import java.util.Locale;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.exceptions.SourceValidationError;
import ru.yandex.canvas.service.TankerKeySet;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
public class ErrorMsgTest {

    public void bakeException(List<String> codes) {
        throw new SourceValidationError(codes);
    }

    @Test
    public void messageRuFormat() {

        Locale.setDefault(new Locale("ru", "RU"));
        SourceValidationError got = null;

        try {
            bakeException(ImmutableList.of(TankerKeySet.HTML5.key("no_html_file_found"),
                    TankerKeySet.HTML5.formattedKey("too_much_files_inside_zip", 12)));
        } catch (SourceValidationError e) {
            got = e;
        }

        assertEquals("Exception contains expected message",
                "{\"messages\":[\"В баннере нет HTML-файла\",\"В баннере больше 12 файлов\"]}",
                got.getMessage());

    }

    @Test
    public void messageTrFormat() {

        Locale.setDefault(new Locale("tr", "TK"));
        SourceValidationError got = null;

        try {
            bakeException(ImmutableList.of(TankerKeySet.HTML5.key("no_html_file_found"),
                    TankerKeySet.HTML5.formattedKey("too_much_files_inside_zip", 12)));
        } catch (SourceValidationError e) {
            got = e;
        }

        assertEquals("Exception contains expected message",
                "{\"messages\":[\"Banner\\u0027da HTML dosyası yok\",\"Banner\\u0027daki dosya 12 adedin üstünde\"]}",
                got.getMessage());

    }

}
