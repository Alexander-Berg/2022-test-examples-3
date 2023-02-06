package ru.yandex.market.ir.matcher2.matcher.matcher.books.patterns;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.junit.Test;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Infix;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.tokenizers.InOfferEntry;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.tokenizers.Tokenization;
import ru.yandex.market.ir.matcher2.matcher.books.patterns.BooksCategoryPattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * todo описать предназначение.
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class BooksCategoryPatternTest {

    @Test
    public void testGetIsbnPositions() {
        checkPositions("");
        checkPositions("|ISBN:");
        checkPositions("|ISBN: ");
        checkPositions("|ISBN:  ");
        checkPositions("|ISBN: 5-85976-234-8", 978585976234L);
        checkPositions(" |ISBN: 5-85976-234-8 ", 978585976234L);
        checkPositions(" |ISBN: 5-85976-234-8 ", 978585976234L);
        checkPositions(
            "|Автор: Сагман С.|Издательство: ДМК|Серия: Самоучитель|Год издания: 2002|" +
                "ISBN: 5-94074-130-4|Описание: Настоящая книга предназначена для пользователей, " +
                "осваивающих программы, которые входят в состав пакета Microsoft Office...", 978594074130L
        );
        checkPositions(
            "|Автор: Брайан Хохгуртль|Издательство: КУДИЦ-Образ|Год издания: 2004|ISBN: 5-9579-0015-X, 1-58450-262-2|" +
                "Описание: Превалирующим трендом в развитии современной индустрии информационных технологий является " +
                "интеграция информационно-вычислительных систем с использованием Интернет-технологий...",
            978595790015L, 978158450262L
        );
    }


    private void checkPositions(String rawDescription, long... isbns) {
        Long2ObjectMap<Infix> positions = BooksCategoryPattern.getIsbnPositions(
            rawDescription,
            new InOfferEntry(Tokenization.FieldWithPriority.DESCRIPTION, 1, rawDescription.length())
        );
        LongSet isbnsSet = new LongArraySet();
        for (long isbn : isbns) {
            isbnsSet.add(isbn);
        }
        assertEquals(isbnsSet.size(), positions.size());
        for (long isbn : isbnsSet) {
            assertNotNull(positions.get(isbn));
        }
    }
}
