package ru.yandex.market.checkout.referee.impl;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.referee.EmptyTest;
import ru.yandex.market.checkout.referee.entity.Lexic;
import ru.yandex.market.checkout.referee.jpa.LexicDao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by antipov93@yndx-team.ru
 */
public class LexicCheckServiceTest extends EmptyTest {

    @Autowired
    private LexicCheckService lexicCheckService;

    @Autowired
    private LexicDao lexicDao;

    @Test
    public void testLexic() {
        lexicDao.save(new Lexic("\\bми"));
        assertTrue(lexicCheckService.init());

        assertFalse(lexicCheckService.checkLexic("Чтоб вы сдохли"));
        assertFalse(lexicCheckService.checkLexic("ми-ми-ми"));
        assertTrue(lexicCheckService.checkLexic("бе-бе-бе"));
    }

    @Test
    public void testRegexp() {
        assertTrue(lexicCheckService.init());

        String regex = "\\b[кш]ц?(абвг|адеф)\\.{0,2}";
        Pattern pattern = Pattern.compile(regex);

        // (абвг|адеф)
        assertTrue(pattern.matcher("кабвг").find());
        assertTrue(pattern.matcher("кадеф").find());
        assertFalse(pattern.matcher("каxxx").find());

        // \b
        assertTrue(pattern.matcher("xxx кабвг").find());
        assertFalse(pattern.matcher("xxxкабвг").find());

        // [кш]
        assertTrue(pattern.matcher("шабвг").find());
        assertFalse(pattern.matcher("xабвг").find());

        // ц?
        assertTrue(pattern.matcher("кцабвг").find());

        // \.{0,2}
        assertTrue(pattern.matcher("кабвг.").find());
        assertTrue(pattern.matcher("кабвг..").find());
        assertTrue(pattern.matcher("кабвг..asdf").find());
        assertTrue(pattern.matcher("кабвг...").find());
    }

    @Test
    public void testRegexpFromDb() {
        // жоп[ыауо]й?
        assertFalse(lexicCheckService.checkLexic("жопа"));

        // \bгнид[аы]
        assertFalse(lexicCheckService.checkLexic("гнида"));
        assertFalse(lexicCheckService.checkLexic(" гнида"));
        assertTrue(lexicCheckService.checkLexic("xгнида"));

        // (ублюдок|ублюдочный)
        assertFalse(lexicCheckService.checkLexic("ублюдок"));
    }
}
