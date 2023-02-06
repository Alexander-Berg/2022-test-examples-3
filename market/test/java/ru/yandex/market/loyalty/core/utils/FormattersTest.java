package ru.yandex.market.loyalty.core.utils;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.utils.Formatters.NBSP;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class FormattersTest {

    @Test
    public void shouldProcessNumbersWithLastPostposition() {
        assertEquals(
                "5" + NBSP + "000" + NBSP + '\u20BD',
                Formatters.makeNonBreakingSpaces("5 000 \u20BD", "test")
        );
    }

    @Test
    public void shouldProcessRealActionText() {
        assertEquals(
                "Повезло! Что" + NBSP + "бы вы" + NBSP + "ни" + NBSP + "купили, теперь это будет на" + NBSP +
                        '5' + NBSP + "000" + NBSP + "₽ дешевле. Использовать эту скидку " +
                        "отдельно или" + NBSP + "вместе с" + NBSP + "другими Беру Бонусами" + NBSP + "– решать вам.",
                Formatters.makeNonBreakingSpaces(
                        "Повезло! Что бы вы ни купили, теперь это будет на 5 000 ₽ дешевле. Использовать эту скидку " +
                                "отдельно или вместе с другими Беру Бонусами – решать вам.",
                        "test"
                )
        );
    }

    @Test
    public void shouldProcessRealActionText2() {
        assertEquals(
                "Отлично, теперь вы" + NBSP + "можете сэкономить 50" + NBSP + "₽ при" + NBSP + "заказе на" + NBSP +
                        "сумму от" + NBSP + '2' + NBSP + "999" + NBSP + "₽. Кстати, использовать эту скидку можно " +
                        "отдельно" +
                        " или" + NBSP + "вместе с" + NBSP + "другими Беру Бонусами.",
                Formatters.makeNonBreakingSpaces(
                        "Отлично, теперь вы можете сэкономить 50 ₽ при заказе на сумму от 2 999 ₽. Кстати, " +
                                "использовать" +
                                " эту скидку можно отдельно или вместе с другими Беру Бонусами.",
                        "test"
                )
        );
    }

    @Test
    public void shouldProcess$AsDigit() {
        assertEquals(
                "$1,000,000" + NBSP + "- это в" + NBSP + "долларах США. $" + NBSP + "1,000,000" + NBSP +
                        "- это тоже в" + NBSP + "долларах США.",
                Formatters.makeNonBreakingSpaces(
                        "$1,000,000 - это в долларах США. $ 1,000,000 - это тоже в долларах США.",
                        "test"
                )
        );
    }

    @Test
    public void shouldProcessPrepositionWithPostposition() {
        assertEquals(
                "С" + NBSP + "с.",
                Formatters.makeNonBreakingSpaces(
                        "С с.",
                        "test"
                )
        );
    }

    @Test
    public void shouldProcessAbbreviationWithVariousSpaces() {
        assertEquals(
                "т." + NBSP + "е.",
                Formatters.makeNonBreakingSpaces(
                        "т. \tе.",
                        "test"
                )
        );
    }

    @Test
    public void shouldProcessDefaultFixedTitle() {
        assertEquals(
                "Скидка 300" + NBSP + '\u20BD',
                Formatters.makeNonBreakingSpaces(
                        CoreCoinType.FIXED.getDefaultTitle(BigDecimal.valueOf(300)),
                        "test"
                )
        );
    }
}
