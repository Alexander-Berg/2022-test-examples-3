package ru.yandex.market.ir.matcher2.matcher.matcher;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.Test;

import ru.yandex.market.ir.matcher2.matcher.DescriptionCleaner;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class DescriptionCleanerTest {
    public void testGetStickPositions() {
        assertEquals(DescriptionCleaner.getStickPositions(null), new IntArrayList());
        assertEquals(DescriptionCleaner.getStickPositions(""), new IntArrayList());
        assertEquals(DescriptionCleaner.getStickPositions(" | | "), new IntArrayList(new int[]{1, 3}));
        assertEquals(DescriptionCleaner.getStickPositions("| |"), new IntArrayList(new int[]{0, 2}));
        assertEquals(DescriptionCleaner.getStickPositions(" | || | "), new IntArrayList(new int[]{1, 6}));
        assertEquals(DescriptionCleaner.getStickPositions("| || |"), new IntArrayList(new int[]{0, 5}));
        assertEquals(DescriptionCleaner.getStickPositions(" | ||| | "), new IntArrayList(new int[]{1, 5, 7}));
        assertEquals(DescriptionCleaner.getStickPositions("| ||| |"), new IntArrayList(new int[]{0, 4, 6}));
        assertEquals(DescriptionCleaner.getStickPositions(" | |||| | "), new IntArrayList(new int[]{1, 8}));
        assertEquals(DescriptionCleaner.getStickPositions("| |||| |"), new IntArrayList(new int[]{0, 7}));
    }

    @Test
    public void testCleanDescription() {
        assertEquals(DescriptionCleaner.cleanDescription(null), "");
        assertEquals(DescriptionCleaner.cleanDescription(""), "");
        assertEquals(DescriptionCleaner.cleanDescription(
                "|Описание: aaa || bbb"),
            "|Описание: aaa || bbb");
        assertEquals(DescriptionCleaner.cleanDescription(
                "|Описание: aaa|Комментарий: bbb"),
            "|Описание: aaa");
        assertEquals(DescriptionCleaner.cleanDescription(
                "|Описание: aaa|Комментарий: bbb|Код производителя: ccc"),
            "|Описание: aaa                 |Код производителя: ccc");
        assertEquals(DescriptionCleaner.cleanDescription(
                "|Описание: aaa|Комментарий: bbb|Код производителя: ccc|Комментарий-2: ddd"),
            "|Описание: aaa                 |Код производителя: ccc");
        assertEquals(DescriptionCleaner.cleanDescription(
                "|Комментарий: bbb|Описание: aaa"),
            "                 |Описание: aaa");
        assertEquals(DescriptionCleaner.cleanDescription(
                "Описание: aaa|Комментарий: bbb"),
            "");
        assertEquals(DescriptionCleaner.cleanDescription(
                "Комментарий: bbb|Описание: aaa"),
            "                |Описание: aaa");
        assertEquals(DescriptionCleaner.cleanDescription(
                "|Описание: aaa|||Комментарий: bbb"),
            "|Описание: aaa||");
        assertEquals(DescriptionCleaner.cleanDescription(
                "|Комментарий: bbb|||Описание: aaa"),
            "                   |Описание: aaa");
        assertEquals(DescriptionCleaner.cleanDescription(
                "|Комментарий: bbb|||Описание: aaa|"),
            "                   |Описание: aaa");
        assertEquals(DescriptionCleaner.cleanDescription(
                "|Комментарий: bbb|||Описание: aaa||"),
            "                   |Описание: aaa||");
        assertEquals(DescriptionCleaner.cleanDescription(
                "|Комментарий: bbb|||Описание: aaa|||"),
            "                   |Описание: aaa||");
        assertEquals(DescriptionCleaner.cleanDescription(
                "|ЕстьЛиДоставка: true|Описание: Тип наушников: проводные || Частотный диапазон: 6-23500 Гц || Длина провода: 1.2 m || Чувствительность: 102 Дб || Сопротивление: 16 Om"),
            "                     |Описание: Тип наушников: проводные || Частотный диапазон: 6-23500 Гц || Длина провода: 1.2 m || Чувствительность: 102 Дб || Сопротивление: 16 Om"
        );
    }
}
