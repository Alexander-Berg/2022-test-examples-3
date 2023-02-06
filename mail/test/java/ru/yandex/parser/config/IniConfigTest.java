package ru.yandex.parser.config;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.IntInterval;
import ru.yandex.collection.IntIntervalSet;
import ru.yandex.collection.IntSet;
import ru.yandex.parser.string.IntSetParser;
import ru.yandex.parser.string.IntegerParser;
import ru.yandex.parser.string.NonNegativeValidator;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.filesystem.CloseableDeleter;

public class IniConfigTest extends TestBase {
    private static final String UNUSED = "unused";
    private static final String GLOBAL = "global";
    private static final String LOCAL = "local";
    private static final String SUB = "sub";
    private static final String SUB2 = "sub2";
    private static final String SUBGLOBAL = "sub.global";
    private static final String SUBLOCAL = "sub.local";
    private static final String ONE_HUNDRED = "100";
    private static final int FIVE = 5;

    public IniConfigTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws ConfigException, IOException {
        IniConfig ini = new IniConfig(
            new StringReader("unused = 0\nglobal = 1\nlocal = 2\n"
                + "[sub]\nlocal=3\nglobal=5\n[sub.sub]\nlocal = 4\n"
                + "global = 6\n[sub.sub.sub2]\nlocal = 7\n"
                + "[sub.sub3]\nglobal = 8\nlocal = 9"));
        IniConfig sub = ini.section(SUB);
        Assert.assertEquals(SUB + '.', sub.prefix());
        Assert.assertEquals("1", ini.getString(GLOBAL, "globaldef"));
        Assert.assertEquals("2", ini.getString(LOCAL));
        Assert.assertEquals("3", ini.getString(SUBLOCAL));
        Assert.assertEquals(FIVE, ini.getInt(SUBGLOBAL));
        Assert.assertEquals("6", sub.getString(SUBGLOBAL));
        sub = ini.section(SUB);
        Assert.assertEquals("" + '6', sub.getString(SUBGLOBAL));
        Assert.assertEquals("" + '5', sub.getString(GLOBAL));
        Assert.assertEquals("7", ini.getString("sub.sub.sub2.local"));
        sub = sub.section(SUB);
        Assert.assertEquals(SUB + '.' + SUB + '.', sub.prefix());
        Assert.assertEquals("4", sub.getString(LOCAL));
        Assert.assertEquals("" + '6', sub.getString(GLOBAL));
        Assert.assertEquals("" + '7', sub.getString("sub2.local"));
        Assert.assertEquals(
            new HashSet<String>(Arrays.asList(SUB)),
            ini.sections().keySet());
        Assert.assertEquals(
            new HashSet<String>(Arrays.asList(SUB2)),
            sub.sections().keySet());
        Assert.assertEquals(
            new HashSet<String>(Arrays.asList(SUB, "sub3")),
            ini.section(SUB).sections().keySet());
        Assert.assertEquals(
            new HashSet<String>(Arrays.asList(UNUSED, LOCAL, GLOBAL)),
            ini.keys());
        Set<String> expected = new HashSet<String>(
            Arrays.asList(UNUSED, "sub.sub3.local", "sub.sub3.global"));
        Assert.assertEquals(expected, new HashSet<>(ini.unusedKeys()));
    }

    @Test
    public void testNotSet() throws ConfigException, IOException {
        IniConfig ini = new IniConfig(new StringReader("root = on"));
        try {
            ini.getString("404");
            Assert.fail();
        } catch (ParameterNotSetException e) {
            Assert.assertEquals("Parameter 404 is not set", e.getMessage());
        }
    }

    @Test
    public void testBoolean() throws ConfigException, IOException {
        IniConfig ini = new IniConfig(
            new StringReader("[sub]\ntrue = on\nfalse = no\nbad = wut?"));
        IniConfig section = ini.section(SUB);
        Assert.assertTrue(section.getBoolean(Boolean.TRUE.toString()));
        Assert.assertTrue(section.getBoolean(Boolean.TRUE.toString(), false));
        Assert.assertTrue(section.getBoolean(Boolean.TRUE.toString(), true));
        Assert.assertTrue(section.getBoolean("default1", true));
        Assert.assertFalse(section.getBoolean("false"));
        Assert.assertFalse(section.getBoolean("default2", false));
        try {
            section.getBoolean("bad");
            Assert.fail();
        } catch (ConfigException e) {
            Assert.assertEquals(
                "Failed to parse parameter sub.bad with value 'wut?'",
                e.getMessage());
        }
    }

    @Test
    public void testSubsection() throws ConfigException, IOException {
        IniConfig config = new IniConfig(
            new StringReader(
                "[top.child]\n"
                + "child1 = ch1\n"
                + "[top]\n"
                + "top1 = toptop"));
        Assert.assertEquals("toptop", config.getString("top.top1"));
        Assert.assertEquals("ch1", config.getString("top.child.child1"));
    }

    private enum TestEnum {
        V1,
        V2,
        V3
    }

    @Test
    public void testEnum() throws ConfigException, IOException {
        IniConfig ini = new IniConfig(
            new StringReader("[sub2]\nfirst = v1\nsecond = V2\nb = ?"));
        IniConfig section = ini.section(SUB2);
        Assert.assertEquals(
            TestEnum.V1,
            section.getEnum(TestEnum.class, "first"));
        Assert.assertEquals(
            TestEnum.V2,
            section.getEnum(TestEnum.class, "second", TestEnum.V3));
        Assert.assertEquals(
            TestEnum.V3,
            section.getEnum(TestEnum.class, "third", TestEnum.V3));
        try {
            section.getEnum(TestEnum.class, "b");
            Assert.fail();
        } catch (ConfigException e) {
            Assert.assertEquals(
                "Failed to parse parameter sub2.b with value '?'",
                e.getMessage());
            Assert.assertNotNull(e.getCause());
        }
    }

    private List<Integer> toList(final IntSet set) {
        List<Integer> list = new ArrayList<>();
        for (Integer value: set) {
            list.add(value);
        }
        return list;
    }

    // CSOFF: MagicNumber
    @Test
    public void testIntSet() throws ConfigException, IOException {
        IniConfig ini = new IniConfig(new StringReader(
            "set1 = 1,2,3\n"
            + "set2 = 4-7,3,1,1,1,9-12,13\n"
            + "set3 = 2\n"
            + "set4 = 1-3, 7-9, 4 - 6"));

        IntSet set = ini.get("set1", IntSetParser.INSTANCE);
        Assert.assertEquals(Arrays.asList(1, 2, 3), toList(set));
        Assert.assertEquals("1-3", set.toString());
        Assert.assertEquals("{1, 2, 3}", set.toBitSet().toString());
        Assert.assertEquals(set, IntIntervalSet.create(set.toBitSet()));
        YandexAssert.assertInstanceOf(IntInterval.class, set);
        Assert.assertFalse(set.contains(0));
        Assert.assertTrue(set.contains(1));
        Assert.assertTrue(set.contains(2));
        Assert.assertTrue(set.contains(3));
        Assert.assertFalse(set.contains(4));

        set = ini.get("set2", IntSetParser.INSTANCE);
        Assert.assertEquals(
            Arrays.asList(1, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13),
            toList(set));
        Assert.assertEquals("1-1,3-7,9-13", set.toString());
        Assert.assertEquals(
            "{1, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13}",
            set.toBitSet().toString());
        Assert.assertEquals(set, IntIntervalSet.create(set.toBitSet()));
        Assert.assertFalse(set.contains(0));
        Assert.assertTrue(set.contains(1));
        Assert.assertFalse(set.contains(2));
        Assert.assertTrue(set.contains(3));
        Assert.assertFalse(set.contains(8));
        Assert.assertTrue(set.contains(13));
        Assert.assertFalse(set.contains(14));

        set = ini.get("set3", IntSetParser.INSTANCE);
        Assert.assertEquals(Arrays.asList(2), toList(set));
        Assert.assertEquals("2-2", set.toString());
        Assert.assertEquals("{2}", set.toBitSet().toString());
        Assert.assertEquals(set, IntIntervalSet.create(set.toBitSet()));
        YandexAssert.assertInstanceOf(IntInterval.class, set);
        Assert.assertFalse(set.contains(0));
        Assert.assertFalse(set.contains(1));
        Assert.assertTrue(set.contains(2));
        Assert.assertFalse(set.contains(3));

        set = ini.get("set4", IntSetParser.INSTANCE);
        Assert.assertEquals(
            Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
            toList(set));
        Assert.assertEquals("1-9", set.toString());
        Assert.assertEquals(
            "{1, 2, 3, 4, 5, 6, 7, 8, 9}",
            set.toBitSet().toString());
        Assert.assertEquals(set, IntIntervalSet.create(set.toBitSet()));
        YandexAssert.assertInstanceOf(IntInterval.class, set);
        Assert.assertFalse(set.contains(0));
        Assert.assertTrue(set.contains(1));
        Assert.assertTrue(set.contains(5));
        Assert.assertFalse(set.contains(10));
    }

    @Test
    public void testIntegerDurationParser()
        throws ConfigException, IOException
    {
        IniConfig ini = new IniConfig(new StringReader(
            "dur1 = 1\n"
            + "dur2 = 2 ms\n"
            + "dur3 = 3.3s\n"
            + "dur4 = 4.5 m\n"
            + "dur5 = 0.5h\n"
            + "dur6 = 0.25d\n"
            + "dur7 = 1w\n"
            + "dur8 = 15000 µs\n"
            + "dur9 = 25000000 ns\n"
            + "dur10 = 4w"));
        Assert.assertEquals(1, ini.getIntegerDuration("dur1"));
        Assert.assertEquals(2, ini.getIntegerDuration("dur2"));
        Assert.assertEquals(3300, ini.getIntegerDuration("dur3"));
        Assert.assertEquals(270000, ini.getIntegerDuration("dur4"));
        Assert.assertEquals(1800000, ini.getIntegerDuration("dur5"));
        Assert.assertEquals(21600000, ini.getIntegerDuration("dur6"));
        Assert.assertEquals(604800000, ini.getIntegerDuration("dur7"));
        Assert.assertEquals(15, ini.getIntegerDuration("dur8"));
        Assert.assertEquals(25, ini.getIntegerDuration("dur9"));
        String dur10 = "dur10";
        try {
            ini.getIntegerDuration(dur10);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(2419200000L, ini.getLongDuration(dur10));
        }
    }
    // CSON: MagicNumber

    @Test
    public void testSystemProperty() throws ConfigException, IOException {
        System.setProperty("PROP", ONE_HUNDRED);
        System.setProperty("CPU_CORES", "16");
        IniConfig ini =
            new IniConfig(
                new StringReader(
                    "prop1 = $(PROP)\n"
                    + "prop2 = $($(PROP) + 1)\n"
                    + "prop3 = $($(PROP) - 1)\n"
                    + "prop4 = $($(PROP) * 2)\n"
                    + "prop5 = $($(PROP) % 2)\n"
                    + "prop6 = $($(PROP) / 2)\n"
                    + "prop7 = $(PROP)/string-concat\n"
                    + "prop8 = $($(prop6) + 1)\n"
                    + "CPU_CORES_75 = $($(CPU_CORES) * 75)\n"
                    + "prop9 = $($(CPU_CORES_75) / 100)\n"
                    + "[section]\n"
                    + "prop1 = $($(prop4) + 2)\n"
                    + "prop2 = $($(prop1) + 1)\n"
                    + "[section2]\n"
                    + "prop1 = $($(section.prop1) + 3)\n"
                    + "prop2 = $($(prop1) + 2)"));
        Assert.assertEquals(ONE_HUNDRED, ini.getString("prop1"));
        Assert.assertEquals("101", ini.getString("prop2"));
        Assert.assertEquals("99", ini.getString("prop3"));
        Assert.assertEquals("200", ini.getString("prop4"));
        Assert.assertEquals("0", ini.getString("prop5"));
        Assert.assertEquals("50", ini.getString("prop6"));
        Assert.assertEquals("100/string-concat", ini.getString("prop7"));
        Assert.assertEquals("51", ini.getString("prop8"));
        Assert.assertEquals("203", ini.getString("section.prop2"));
        Assert.assertEquals("205", ini.getString("section2.prop1"));
        Assert.assertEquals("207", ini.getString("section2.prop2"));
        Assert.assertEquals("12", ini.getString("prop9"));
        YandexAssert.assertEmpty(ini.unusedKeys());
    }

    // CSOFF: MagicNumber
    @Test
    public void testOverrides() throws Exception {
        IniConfig config = new IniConfig(
            Paths.get(IniConfigTest.class.getResource("config.ini").toURI()));
        IniConfig root = config.section("root");
        Assert.assertEquals(1, root.getInt("subroot"));
        Assert.assertEquals(7, root.getInt("subroot2"));
        Assert.assertEquals(3, config.getInt("section.value"));
        Assert.assertEquals(8, config.getInt("section.another"));
        Assert.assertEquals(5, config.getInt("section2.value"));
        Assert.assertEquals(6, config.getInt("section2.another"));
    }
    // CSON: MagicNumber

    @Test
    public void testSimpleInclude() throws Exception {
        try (CloseableDeleter deleter =
                new CloseableDeleter(
                    Files.createTempDirectory(testName.getMethodName())))
        {
            String subdirName = "subdir";
            System.setProperty("SUB_DIRECTORY", subdirName);
            Path subdir = deleter.path().resolve(subdirName);
            Files.createDirectory(subdir);
            Path subconfig = subdir.resolve("subconfig.ini");
            Files.write(
                subconfig,
                Arrays.asList(
                    "key1 = 555",
                    "subkey1 = 330",
                    "[sub]",
                    "subkey1 = 440",
                    "subkey2 = $(dirname)"),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
            Path configPath = deleter.path().resolve("mainconfig.ini");
            Files.write(
                configPath,
                Arrays.asList(
                    "key1 = will be overriden",
                    "key1_1 = 444",
                    "$(include $(SUB_DIRECTORY)/*.ini)",
                    "key2 = $($(subkey1) + 1)",
                    "key3 = $($(sub.subkey1) + 1)",
                    "[section]",
                    "key1 = 337",
                    "key2 = $($(sub.subkey1) + 2)",
                    "$(include " + subconfig.toAbsolutePath() + ')',
                    "key3 = $($(subkey1) + 5)"),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
            IniConfig config = new IniConfig(configPath);
            Assert.assertEquals("555", config.getString("key1"));
            Assert.assertEquals("444", config.getString("key1_1"));
            Assert.assertEquals("331", config.getString("key2"));
            Assert.assertEquals("335", config.getString("key3"));
            Assert.assertEquals("337", config.getString("section.key1"));
            Assert.assertEquals("442", config.getString("section.key2"));
            Assert.assertNull(config.getString("section.key3", null));
            Assert.assertEquals("440", config.getString("sub.subkey1"));
            YandexAssert.assertEndsWith(
                '/' + subdirName,
                config.getString("sub.subkey2"));
        }
    }

    @Test
    public void testTransitiveGlobInclude() throws Exception {
        try (CloseableDeleter deleter =
                new CloseableDeleter(
                    Files.createTempDirectory(testName.getMethodName())))
        {
            Path subdir = deleter.path().resolve("conf.d");
            Files.createDirectory(subdir);
            Path subsubdir = subdir.resolve("subconf.d");
            Files.createDirectory(subsubdir);
            Files.write(
                subdir.resolve("first.conf"),
                Arrays.asList(
                    "first1 = value1",
                    "first2 = value2",
                    "first3 = value3",
                    "$(include subconf.d/*.ini)",
                    "[sub.$(filename).section]",
                    "file = $(filename).file"),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
            Files.write(
                subdir.resolve("second.conf"),
                Arrays.asList(
                    "second1 = value2_1",
                    "second2 = value2_2",
                    "first2 = overridden1_2",
                    "third2 = overridden3_2"),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
            Files.write(
                subsubdir.resolve("third.ini"),
                Arrays.asList(
                    "third1 = value3_1",
                    "third2 = value3_2",
                    "second1 = not_overridden_value2_1",
                    "first3 = overridden1_3",
                    "[sect]",
                    "sectvalue = value_sect",
                    "$(include forth.txt)",
                    "[sub.$(filename).section2]",
                    "file2 = $(filename).file"),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
            Files.write(
                subsubdir.resolve("forth.txt"),
                Arrays.asList(
                    "forth1 = value4_1",
                    "forth2 = $(sect.sectvalue)"),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
            Path configPath = deleter.path().resolve("main.conf");
            Files.write(
                configPath,
                Arrays.asList("$(include **/*.conf)"),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
            IniConfig config = new IniConfig(configPath);
            Assert.assertEquals("value1", config.getString("first1"));
            Assert.assertEquals("overridden1_2", config.getString("first2"));
            Assert.assertEquals("overridden1_3", config.getString("first3"));
            Assert.assertEquals("value2_1", config.getString("second1"));
            Assert.assertEquals("value2_2", config.getString("second2"));
            Assert.assertEquals("value3_1", config.getString("third1"));
            Assert.assertEquals("overridden3_2", config.getString("third2"));
            Assert.assertEquals(
                "first.file",
                config.getString("sub.first.section.file"));
            Assert.assertEquals(
                "third.file",
                config.getString("sub.third.section2.file2"));
            Assert.assertEquals("value4_1", config.getString("forth1"));
            Assert.assertEquals("value_sect", config.getString("forth2"));
        }
    }

    @Test
    public void testIncludeSubsection() throws Exception {
        try (CloseableDeleter deleter =
                new CloseableDeleter(
                    Files.createTempDirectory(testName.getMethodName())))
        {
            String subdirName = "subdir";
            System.setProperty("SUB_DIRECTORY", subdirName);
            Path subdir = deleter.path().resolve(subdirName);
            Files.createDirectory(subdir);
            Path subconfig = subdir.resolve("subconfig.ini");
            Files.write(
                subconfig,
                Arrays.asList(
                    "key1 = 555",
                    "subkey1 = 330",
                    "[sub]",
                    "subkey1 = 44"),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
            Path configPath = deleter.path().resolve("mainconfig.ini");
            Files.write(
                configPath,
                Arrays.asList(
                    "key1 = value1",
                    "[section]",
                    "key1 = 337",
                    "$(include_subsection " + subconfig.toAbsolutePath() + ')',
                    "key3 = $($(subkey1) + 5)"),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
            IniConfig config = new IniConfig(configPath);
            Assert.assertEquals("value1", config.getString("key1"));
            Assert.assertEquals("555", config.getString("section.key1"));
            Assert.assertEquals("330", config.getString("section.subkey1"));
            Assert.assertEquals("335", config.getString("section.key3"));
            Assert.assertEquals("44", config.getString("section.sub.subkey1"));
        }
    }

    @Test
    public void testValidator() throws Exception {
        String name = "valid";
        IniConfig config = new IniConfig(new StringReader("valid = -1"));
        int value = config.get(name, IntegerParser.INSTANCE);
        Assert.assertEquals(-1, value);
        try {
            value = config.get(
                name,
                IntegerParser.INSTANCE.andThen(
                    NonNegativeValidator.instance()));
            Assert.fail("Unexpected value: " + value);
        } catch (ConfigException e) {
        }
    }

    @Test
    public void testEscaping() throws Exception {
        IniConfig ini =
            new IniConfig(
                new StringReader(
                    "[www\\.yandex\\.ru]\n"
                    + "dot\\\\.key = world\n"
                    + "dot\\\\.key2 = hello"));
        Assert.assertNull(ini.sectionOrNull("www"));
        IniConfig section = ini.section("www\\.yandex\\.ru");
        String key = "dot.key";
        String key2 = "dot.key2";
        Assert.assertEquals(
            new HashSet<>(Arrays.asList(key, key2)),
            section.keys());
        Assert.assertEquals(
            new HashSet<>(section.unusedKeys()),
            section.keys());
        Assert.assertEquals("world", section.getString("dot\\.key"));
        Assert.assertEquals(
            Collections.singleton(key2),
            new HashSet<>(section.unusedKeys()));
        Assert.assertEquals(
            "hello",
            ini.section("www.yandex.ru").getString(key2));
        Assert.assertEquals(Collections.emptyList(), section.unusedKeys());
    }

    @Test
    public void testMinusSubstitution() throws Exception {
        IniConfig ini =
            new IniConfig(
                new StringReader(
                    "[my-section]\n"
                    + "my-key = he-hey\n"
                    + "[selection]\n"
                    + "sel-key = $(my-section.my-key)\n"
                    + "[another-section]\n"
                    + "another-key = $(selection.sel-key)"));
        Assert.assertEquals(
            "he-hey",
            ini.section("another-section").getString("another-key"));
    }

    @Test
    public void testUnusedSection() throws Exception {
        IniConfig ini =
            new IniConfig(
                new StringReader("[section]\nsubsection = lost\n[section2]"));
        Assert.assertNull(ini.getString("section.subsection.value", null));
        Assert.assertEquals(
            new HashSet<>(Arrays.asList("section.subsection", "section2.[]")),
            new HashSet<>(ini.unusedKeys()));
    }

    @Test
    public void testMultilineParser() throws Exception {
        IniConfig ini =
            new IniConfig(
                new StringReader(
                    "[root]\n"
                    + "key = \\\n"
                    + "\tbegin\\n\\\n"
                    + "\t\tmiddle\\n\\\n"
                    + "\tend\n"
                    + "key2 = value\n"));
        Assert.assertEquals("begin\nmiddle\nend", ini.getString("root.key"));
        Assert.assertEquals("value", ini.getString("root.key2"));
    }
}

