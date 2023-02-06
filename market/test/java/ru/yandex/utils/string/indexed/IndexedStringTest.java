package ru.yandex.utils.string.indexed;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.utils.string.DeNullingProcessor;
import ru.yandex.utils.string.DefaultStringProcessor;

import static org.junit.Assert.assertEquals;

/**
 * todo описать предназначение.
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class IndexedStringTest {
    @Test
    public void findTokenByIndex() {
        IndexedStringFactory factory = new IndexedStringFactory();
        factory.setStringProcessor(new DefaultStringProcessor());
        String[] examples = new String[]{
            "a aa aab",
            " a aa aab ",
            "Матрас (матрац) ортопедический.:Magic Dream:Multipoket (S2000):\" V.I.P.\" S2000 | матрас ортопедический, матрац ортопедический\t" +
                "|ЕстьЛиДоставка: true|Описание: Ортопедический матрас \"1001 ночь\" Классический ортопедический матрас средней жесткости. Максимально позволяет снять нагрузку с позвоночника, за счет использования в своей конструкции пружинного блока Дуэт (пружина в пружине). Идеально подходит для супружеских пар с разницей в весе более 30 кг, а так же для людей с весом более 110кг. Соствляющие матраса: - Латекс натуральный - Койра латексированная - Независимый пружинный блок Дуэт (пружина в пружинке) - Койра латексированная - " +
                "Латекс натуральный -...",
            "-",
            "--",
            " - ",
            " -- ",
            "--------^^",
        };
        for (String example : examples) {
            IndexedString is = factory.createIndexedString(example);
            checkEveryToken(is);
            checkTokensOrder(is);
        }
    }

    @Test
    public void superscriptAcceptedAsWord() {
        IndexedStringFactory indexedStringFactory = new IndexedStringFactory();
        indexedStringFactory.setSteam(true);
        indexedStringFactory.setStringProcessor(new DeNullingProcessor());

        IndexedString indexedString = indexedStringFactory.createIndexedString("Объем: 20 см³");

        Assert.assertTrue(indexedString.getWords().indexOf("³") != -1);
    }

    private void checkEveryToken(IndexedString is) {
        for (IndexedString.IndexedToken token : is.getTokens()) {
            checkTokenPosition(is, token, token.start);
            checkTokenPosition(is, token, (token.start + token.end) / 2);
            checkTokenPosition(is, token, token.end - 1);
        }
    }

    private void checkTokenPosition(IndexedString is, IndexedString.IndexedToken token, int index) {
        int foundTokenIndex = is.findTokenByIndex(index);
        String tokenString = is.showToken(foundTokenIndex);
        String srcTokenString = is.getText().substring(token.start, token.end);
        assertEquals("Token strings are not equal", tokenString, srcTokenString);
    }

    private void checkTokensOrder(IndexedString is) {
        int prevTokenNumber = -1;
        for (int index = 0; index < is.getText().length(); index++) {
            int tokenNumber = is.findTokenByIndex(index);
            if (prevTokenNumber > tokenNumber) {
                throw new IllegalStateException("Prev token got a number higher then current!");
            }
            prevTokenNumber = tokenNumber;
        }
    }
}
