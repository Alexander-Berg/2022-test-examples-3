package ru.yandex.market.api.doc.dita;

import com.sun.javadoc.Tag;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.doc.tags.TextTagImpl;

/**
 * Created by vivg on 12.04.17.
 */
public class TextRendererTest {

    private TextRenderer TR = TextRenderer.INSTANCE;

    @Test
    public void test() {
        //Список предлогов
        Assert.assertEquals(
            "а&#160;в&#160;и&#160;к&#160;о&#160;с&#160;у&#160;is&#160;of&#160;во&#160;вы&#160;до&#160;ее&#160;её&#160;же&#160;за&#160;из&#160;их&#160;ко&#160;ли&#160;на&#160;не&#160;ни&#160;но&#160;об&#160;от&#160;по&#160;со&#160;то&#160;1",
            TR.apply(toTag("а в и к о с у is of во вы до ее её же за из их ко ли на не ни но об от по со то 1"))
        );
        // символы whitespaces
        Assert.assertEquals(
            "а&#160;в&#160;и&#160;к&#160;о&#160;с&#160;у&#160;is&#160;of&#160;во&#160;вы",
            TR.apply(toTag("а в    и\nк\tо\u000Bс\fу\ris \n of \t во \r\n вы"))
        );
        // Верхний регистр, начало предложения, запятая после предлога
        Assert.assertEquals(
            "В&#160;начале и&#160;в&#160;конце. А&#160;так же,&#160;и&#160;в&#160;середине.",
            TR.apply(toTag("В начале и в конце. А так же, и в середине."))
        );
        // Знаки пунктуации
        Assert.assertEquals(
            "Вместе с . Как же? Так, не ? То&#160;то&#160;же\n! Так {то } оно так. В&#160;начале или (и ) в&#160;конце. А&#160;так же,&#160;и&#160;в&#160;середине.",
            TR.apply(toTag("Вместе с . Как же? Так, не ? То то же\n! Так {то } оно так. В начале или (и ) в конце. А так же, и в середине."))
        );
        // Числа
        Assert.assertEquals(
            "1&#160;9&#160;11&#160;99&#160;9.9&#160;11.0 100 999 9999 999999",
            TR.apply(toTag("1   9\n11\t 99 9.9 11.0 100 999 9999 999999"))
        );
        // Длинное тире
        Assert.assertEquals(
            "&#160;— 1— >— 9— 99&#160;— 100&#160;— ",
            TR.apply(toTag(" — 1— >— 9— 99 — 100\n— "))
        );

        Assert.assertEquals(
            "Кол-во символов",
            TR.apply(toTag("Кол-во символов"))
        );
    }

    private Tag toTag(String value) {
        return new TextTagImpl(null, value);
    }
}
