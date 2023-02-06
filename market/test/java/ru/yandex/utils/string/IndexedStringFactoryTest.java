package ru.yandex.utils.string;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.utils.string.indexed.IndexedString;
import ru.yandex.utils.string.indexed.IndexedStringFactory;
import ru.yandex.utils.string.indexed.Position;
import ru.yandex.utils.string.indexed.String4Search;

import java.lang.reflect.Field;
import java.util.SortedMap;

import static org.junit.Assert.assertEquals;

public class IndexedStringFactoryTest {

    private static IndexedStringFactory factory;

    @BeforeClass
    public static void setUp() {
        factory = new IndexedStringFactory();
        factory.setStringProcessor(new DefaultStringProcessor());
    }

    @Test
    public void testPlusMinusExtraction() throws Exception {
        IndexedString dvdPlus = factory.createIndexedString("DVD+RW");
        IndexedString dvdMinus = factory.createIndexedString("DVD-RW");
        System.out.print("");
    }

    @Test
    public void testNumbersExtraction() throws Exception {
        checkNumbers(new double[]{1, 2, 3}, "1 2 3 ");
        checkNumbers(new double[]{1, 2, 3}, "1, 2, 3, ");
        checkNumbers(new double[]{1, 2, 3}, "1,, 2,, 3,, ");
        checkNumbers(new double[]{1, 2, 3}, "1. 2. 3. ");
        checkNumbers(new double[]{1, 2, 3}, "1.. 2.. 3.. ");

        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.1 2.2 3.3 ");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "01.1 02.2 03.3 ");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.10 2.20 3.30 ");

        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.1;2.2;3.3;");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "01.1;02.2;03.3;");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.10;2.20;3.30;");

        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1,1 2,2 3,3 ");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "01,1 02,2 03,3 ");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1,10 2,20 3,30 ");

        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1,1;2,2;3,3;");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "01,1;02,2;03,3;");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1,10;2,20;3,30;");

        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.1, 2.2, 3.3, ");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "01.1, 02.2, 03.3, ");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.10, 2.20, 3.30, ");

        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1,1. 2,2. 3,3. ");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "01,1. 02,2. 03,3. ");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1,10. 2,20. 3,30. ");

//        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.1,2.2,3.3,");
//        checkNumbers(new double[]{1.1, 2.2, 3.3}, "01.1,02.2,03.3,");
//        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.10,2.20,3.30,");

        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.1. 2.2. 3.3. ");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.10. 2.20. 3.30. ");

        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.2.3. 1.1. 2.2. 3.3. ");
        checkNumbers(new double[]{1.1, 2.2, 3.3}, "1.2.3.. 1.1.. 2.2.. 3.3.. ");

        checkNumbers(new double[]{100, 1}, "100 001");
//        checkNumbers(new double[]{100000}, "100 000");
//        checkNumbers(new double[]{100, 1000}, "100 001 000");
//        checkNumbers(new double[]{100000, 1}, "100 000 001");
//        checkNumbers(new double[]{100000000}, "100 000 000");
//        checkNumbers(new double[]{100000, 200000}, "100 000, 200 000");
//        checkNumbers(new double[]{100000000, 200000000}, "100 000 000, 200 000 000");
    }

    @Test
    public void testFractions() throws Exception {
        checkNumbers(new double[]{5.1, 5.0, 0.1, 1.0, 10.0, 2.33, 2, 0.33, 33, 100}, "Первая сложная дробь 5 1/10 Вторая сложная дробь 2 33/100");
        checkNumbers(new double[]{0.5, 1.0, 2.2, 2.0, 0.2, 1.0, 5.1, 5.0, 0.1, 1.0, 10.0}, "Несколько простых дробей 1/2 1/5 1/10.");
        checkNumbers(new double[]{0.5, 1.0, 2.1, 0.2, 5.1, 0.1, 10}, "Несколько простых дробей 1/2,1/5,1/10 с разделителем.");
        checkNumbers(new double[]{0.25, 0.5, 0.75, 0.5, 1, 2, 7.25, 7}, "Вот они спец символы в дробях ¼ ½ ¾ и еще дроби 1/2 и  еще дроби 7 ¼");
        checkNumbers(new double[]{2.25, 2.0, 3.75, 3.0, 4.5, 4.0, 0.5, 1.0, 2.0}, "Все мы любим вот такие дроби 2 ¼ 3 ¾ 4 1/2 не так ли ?");
        checkNumbers(new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9.75, 9}, "Ну вот кто такое в реально жизни 1/2/3/4/5/6/7/8/9¾ будет использовать ?");
        checkNumbers(new double[]{90, 60, 90, 100.5}, "Девушка мечты 90/60/90 с уровнем интеллекта 100.5.");
        checkNumbers(new double[]{10.25, 10.0, 0.5, 0.75, 0.5, 1, 2}, "И вот такой ад 10 ¼ ½ ¾ тоже работает1/2");
        checkNumbers(new double[]{}, "А вот это не дробь ¼½¾");
        checkNumbers(new double[]{1.0, 1.75, 1.0}, "Ухх жесть 1¼½1¾ то какая");
        checkNumbers(new double[]{1.0000000000000001E80}, "100000000000000000000000000000000000000000000000000000000000000000000000000000000");
        checkNumbers(new double[]{5.0000000000000007E79, 1.0000000000000001E80, 2}, "100000000000000000000000000000000000000000000000000000000000000000000000000000000/2");
    }

    private void checkNumbers(double[] doubles, String sourceString) throws NoSuchFieldException, IllegalAccessException {
        SortedMap<Position, ? extends Number> numbers = getNumbers(sourceString);
        assertEquals(doubles.length, numbers.size());
        int i = 0;
        for (Position position : numbers.keySet()) {
            Number number = ((SortedMap<Position, ? extends Number>) numbers).get(position);
            assertEquals(number.doubleValue(), doubles[i++], number.doubleValue() / 1e6);
        }
    }

    @SuppressWarnings("unchecked")
    private SortedMap<Position, ? extends Number> getNumbers(String sourceString)
        throws NoSuchFieldException, IllegalAccessException {
        Field numbersField = IndexedString.class.getDeclaredField("numbers");
        numbersField.setAccessible(true);
        return (SortedMap<Position, ? extends Number>) numbersField.get(factory.createIndexedString(sourceString));
    }

    @Test
    public void testColon() throws Exception {
        IndexedString a = factory.createIndexedString("3:2");
        IndexedString b = factory.createIndexedString("3.2");
        System.out.print("");
    }

//    @Test
//    public void testSimple() throws Exception {
//        IndexedString a = factory.createIndexedString("tfdgfdg Eten+ (something)");
//        String4Search b = factory.createString4Search(" eten ", true);
//        assertEquals("Eten", a.show(b.findIn(a).get(0)));
//    }
//    @Test
//    public void testDefault() throws Exception {
//        IndexedString a = factory.createIndexedString("tfdgfdg Eten+ (something)");
//        String4Search b = factory.createString4Search("- eten +", true);
//        assertEquals("Eten+", a.show(b.findIn(a).get(0)));
//    }

    @Test
    public void testExtra() throws Exception {
        IndexedString a = factory.createIndexedString("tfdgfdg Eten + (something)");
        String4Search b = factory.createString4Search("+", true);
        assertEquals("+", a.show(b.findIn(a).get(0)));
    }

    private String turkeyI = "İ";

    @Test
    public void testLocale() throws Exception {
        IndexedString s1 = factory.createIndexedString("tr_TR", turkeyI);
        IndexedString s2 = factory.createIndexedString(turkeyI);
        Assert.assertEquals(1, s1.toString().length());
        Assert.assertEquals(2, s2.toString().length());

    }
}
