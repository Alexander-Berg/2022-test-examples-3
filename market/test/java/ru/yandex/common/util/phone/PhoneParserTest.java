package ru.yandex.common.util.phone;

import org.junit.Test;
import ru.yandex.common.util.functional.Function;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.common.util.collections.CollectionFactory.newList;

/**
 * Author: Olga Bolshakova (obolshakova@yandex-team.ru)
 * Date: 22.04.2008
 */
public class PhoneParserTest {

    private static final String[] TEST_1 = new String[]{
            "89118428460, тел. 7534835\n89118428460, тел. 7534835",
            "(909) 594-7794\n(909) 594-7794",
            "4\n4\n715-93-18\n8-903-097-87-71\n+\n4\n4",
            "89049704418\n89049704418",
            "89213460506, с 12, по 22",
            "496-68-31, с 11, по 20, тел. 89013146831",
            "+9(812) 715-9005",
            "+375296678473",
            "926 (561-3231)\n\t\t\t\t\n-- \n(\n) --\n926 (561-3231)",
            "89207611273.89105523752",
            "(812)9429575\n8129429575\n8(812)9429576",
            "Андрей 911-188-5608",
            "+7(921)3080098)",
            "8(921)314-(2391)",
            "8063 717 10 55 Звонить до 24:00",
//            "(+380)+ 38 (044) 496 00 06 , (+380)8 (044) 228 00 28",
            "+ 38 (044) 496 00 06 , (+380)8 (044) 228 00 28",
            "(+7921) 851 21 05",
            "(947) 2796",
            "79034182810; 7-918-747-1082",
            "8-908-271-63-83",
            "89241323460, ag081017",
            "+7(909) 923-9860, ((00:00 - 00:00))",
            "+7 /909/ 923-9860"
    };

    private static final String[] RIGHT_RESULT_1 = new String[]{
            "[(911)8428460, (?)7534835]",
            "[(909)5947794]",
            "[(903)0978771, (?)7159318]",
            "[(904)9704418]",
            "[(921)3460506]",
            "[(901)3146831, (?)4966831]",
            "[(812)7159005]",
            "[296678473]",
            "[(926)5613231]",
            "[(910)5523752, (920)7611273]",
            "[(812)9429575, (812)9429576, 8129429575]",
            "[(911)1885608]",
            "[(921)3080098]",
            "[(921)3142391]",
            "[0637171055]",
            "[(044)4960006, (044)82280028]",
            "[(921)8512105]",
            "[(?)9472796]",
            "[(903)4182810, (918)7471082]",
            "[(908)2716383]",
            "[(924)1323460]",
            "[(909)9239860]",
            "[(909)9239860]"
    };

    private static final String[] TEST_2 = new String[]{
            "4449217",
            "+7(495)4449217",
            "(916) 683-9665",
            "80504971883",
            "8-921-319-55-55",
            "+8(915) 130-6731",
            "(8352) 721-621",
            "+49(231) 725-1616",
            "(495) 234-234-2",
            "8 901-513 20 40",
            "945-23-12"
    };

    private static final String[] RIGHT_RESULT_2 = new String[]{
            "[(?)4449217]",
            "[(495)4449217]",
            "[(916)6839665]",
            "[0504971883]",
            "[(921)3195555]",
            "[(915)1306731]",
            "[(8352)721621]",
            "[(231)7251616]",
            "[(495)2342342]",
            "[(901)5132040]",
            "[(?)9452312]"
    };

    @Test
    public void testFirstSuit() {
        for (int i = 0; i < TEST_1.length; i++) {
            assertEquals(RIGHT_RESULT_1[i], sort(PhoneParser.parse(TEST_1[i])).toString());
        }
    }

    @Test
    public void testSecondSuit() {
        for (int i = 0; i < TEST_2.length; i++) {
            assertEquals(RIGHT_RESULT_2[i], sort(PhoneParser.parse(TEST_2[i])).toString());
        }
    }

    @Test
    public void testFromJira() {
        final Set<PhoneNumber> res1 = PhoneParser.parse("+7(917)0845");
        final Set<PhoneNumber> res2 = PhoneParser.parse("+7(812)9170845");
        res1.retainAll(res2);

        assertEquals(0, res1.size());

        final Set<PhoneNumber> s1 = PhoneParser.parse("(495)9170845");
        final Set<PhoneNumber> s2 = PhoneParser.parse("(812)9170845");
        s1.retainAll(s2);

        assertEquals(0, s1.size());
    }

    @Test
    public void testNumericalNoise() {
        final Set<PhoneNumber> set = PhoneParser.parse(
                "(495) 221-4272 c 10:00 до 20:00\n" +
                        "(495) 221-4222 c 10:00 до 22:00\n" +
                        "(495) 778-5587 c 10:00 до 21:00"
        );
        assertEquals("[(495)2214222, (495)2214272, (495)7785587]", sort(set).toString());
    }

    @Test
    public void testEquals() {
        final Set<PhoneNumber> phones1 = PhoneParser.parse("(909)903-53-19\n");
        final Set<PhoneNumber> phones2 = PhoneParser.parse("(909)903-53-19\n");

        phones1.retainAll(phones2);

        assertEquals(1, phones1.size());
    }

    @Test
    public void testNotEquals() {
        final Set<PhoneNumber> phones2 = PhoneParser.parse("(044)2283005/n");
        final Set<PhoneNumber> phones1 = PhoneParser.parse("2283005/n");

        phones1.retainAll(phones2);

        assertEquals(0, phones1.size());
    }

/*
    @Test
    public void testMissedRegion() {
        Set<PhoneNumber> phones = PhoneParser.parse("+375 29 1699148\n88127777777", "1");
        for (PhoneNumber phone : phones) {
            assertEquals("1", phone.getRegion());
        }
    }
*/

    @Test
    public void testDetectedRegion() {
        Set<PhoneNumber> phones = PhoneParser.parseWithDefaultRegion("+375 29 1699148\n+ 38 (044) 496 00 06 , +380 (44) 228 00 28,", "1");
        final List<PhoneNumber> numberList = sortWithCC(phones);
        Function<PhoneNumber, String> regionF = new Function<PhoneNumber, String>() {
            @Override
            public String apply(final PhoneNumber arg) {
                return arg.getRegion();
            }
        };
        assertEquals(regionF.map(numberList), list("375", "38", "380"));
    }

    @Test
    public void testNotRemovedLeading7() {
        final Set<PhoneNumber> set = PhoneParser.parse("7(911)9126236");

        assertEquals("[(911)9126236]", sort(set).toString());
    }

    @Test
    public void testNotRemovedPrefix() {
        final Set<PhoneNumber> set = PhoneParser.parse("791 3739");

        assertEquals("[(?)7913739]", sort(set).toString());
    }

    @Test
    public void testDoublePlus() {
        String rawPhones = "++79504496002\n++7(963)6009373\n++7 916 6912043\n++7(342)243-38-48";
        final Set<PhoneNumber> set = PhoneParser.parse(rawPhones);

        assertEquals("[(342)2433848, (916)6912043, (950)4496002, (963)6009373]", sort(set).toString());
    }

    @Test
    public void testCuttingTimeWithoutMinutes() {
        String rawPhones = "912-29-40 с 10 до 21\n+7(812)912-29-40 с 10  до 21";
        final Set<PhoneNumber> set = PhoneParser.parse(rawPhones);

        assertEquals("[(812)9122940, (?)9122940]", sort(set).toString());
    }

    @Test
    public void testParseTheLongestPossibleCode() {
        String rawPhone = "(55555)12345";
        Set<PhoneNumber> phone = PhoneParser.parse(rawPhone);
        assertEquals("[(55555)12345]", phone.toString());
    }

    @Test
    public void testNotParseTooLongCode() {
        String rawPhone = "(666666)1234";
        Set<PhoneNumber> phone = PhoneParser.parse(rawPhone);
        assertEquals("[6666661234]", phone.toString());
    }

    @Test
    public void testParseTheShortestPossibleNumber() {
        String rawPhone = "1-23-45";
        Set<PhoneNumber> phone = PhoneParser.parse(rawPhone);
        assertEquals("[(?)12345]", phone.toString());
    }

    @Test
    public void testNotParseTooShortNumber() {
        String rawPhone = "1234";
        Set<PhoneNumber> phone = PhoneParser.parse(rawPhone);
        assertTrue(phone.isEmpty());
    }

    @Test
    public void testNestedParentheses() throws Exception {
        String rawPhone = "(8(048)) 7430704";
        Set<PhoneNumber> phones = PhoneParser.parse(rawPhone);
        assertEquals(1, phones.size());
        assertEquals("(048)7430704", phones.iterator().next().toString());
    }

    @Test
    public void testIncorrectParentheses() throws Exception {
        Set<PhoneNumber> phones = PhoneParser.parse("(8(048) 7430704");
        assertEquals(1, phones.size());
        assertEquals("(048)7430704", phones.iterator().next().toString());

        phones = PhoneParser.parse("8(048)) 7430704");
        assertEquals(1, phones.size());
        assertEquals("(048)7430704", phones.iterator().next().toString());

        phones = PhoneParser.parse("8(048) 7430704)");
        assertEquals(1, phones.size());
        assertEquals("(048)7430704", phones.iterator().next().toString());
    }

    // utility methods

    private static List<PhoneNumber> sort(Set<PhoneNumber> phonesSet) {
        List<PhoneNumber> phones = newList(phonesSet);
        Collections.sort(phones, new Comparator<PhoneNumber>() {
            public int compare(PhoneNumber a, PhoneNumber b) {
                return a.toString().compareTo(b.toString());
            }
        });
        return phones;
    }

    private static List<PhoneNumber> sortWithCC(Set<PhoneNumber> phonesSet) {
        List<PhoneNumber> phones = newList(phonesSet);
        Collections.sort(phones, new Comparator<PhoneNumber>() {
            public int compare(PhoneNumber a, PhoneNumber b) {
                return a.toStringWithCountryCode().compareTo(b.toStringWithCountryCode());
            }
        });
        return phones;
    }
}
