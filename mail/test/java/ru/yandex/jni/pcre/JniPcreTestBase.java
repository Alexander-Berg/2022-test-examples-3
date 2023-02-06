package ru.yandex.jni.pcre;

import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

@Ignore
public class JniPcreTestBase extends TestBase {
    private final boolean useHyperscan;

    public JniPcreTestBase(final boolean useHyperscan) {
        super(false, 0L);
        this.useHyperscan = useHyperscan;
    }

    @Test
    public void test() throws Exception {
        try (JniPcre pcre = new JniPcre(useHyperscan)) {
            logger.info("JniPcre created");
            int first = pcre.addRegex(".*if.*");
            int second = pcre.addRegex(".*l[Ii][Ff]eless.*");
            logger.info(
                "First regex id: " + first
                + ", second regex id: " + second);
            Assert.assertNotEquals(first, second);
            Assert.assertEquals(first, pcre.findMatch("life"));
            Assert.assertEquals(-1, pcre.findMatch("abracadabra"));
            Assert.assertEquals(
                second,
                pcre.findMatch("This world is lIfeless"));
        }
    }

    @Test
    public void testBadRegex() throws Exception {
        try (JniPcre pcre = new JniPcre(useHyperscan)) {
            try {
                pcre.addRegex("[");
                Assert.fail();
            } catch (JniPcreException e) {
                logger.log(Level.INFO, "Regex add failed", e);
            }
        }
    }

    @Test
    public void testCyrillic() throws Exception {
        try (JniPcre pcre = new JniPcre(useHyperscan)) {
            int first = pcre.addRegex("[мМ][иИ][рР]");
            int second = pcre.addRegex("[пП]ривет");
            int third = pcre.addRegex("Превед.*Земля");
            Assert.assertEquals(first, pcre.findMatch("ПрИвЕт МиР"));
            Assert.assertEquals(second, pcre.findMatch("Приветики"));
            Assert.assertEquals(
                third,
                pcre.findMatch("Превед, кагдила\nОстановись, Земля, я сойду"));
        }
    }

    @Test
    public void testComplexExpressions() throws Exception {
        try (JniPcre pcre = new JniPcre(useHyperscan)) {
            int first = pcre.addRegex("[Ee]-?[Mm]ail");
            int second = pcre.addRegex("миру (мир|война)");
            Assert.assertEquals(first, pcre.findMatch("Bonus email"));
            Assert.assertEquals(first, pcre.findMatch("Bonus E-Mail"));
            Assert.assertEquals(-1, pcre.findMatch("Bonus emial"));
            Assert.assertEquals(second, pcre.findMatch("миру мир"));
            Assert.assertEquals(second, pcre.findMatch("миру война"));
            Assert.assertEquals(-1, pcre.findMatch("миру beer"));
        }
    }

    @Test
    public void testDistance() throws Exception {
        try (JniPcre pcre = new JniPcre(useHyperscan)) {
            int first = pcre.addRegex("a.{0,10}b");
            Assert.assertEquals(first, pcre.findMatch("ab"));
            Assert.assertEquals(first, pcre.findMatch("a111111b"));
            Assert.assertEquals(first, pcre.findMatch("a1111111111b"));
            Assert.assertEquals(-1, pcre.findMatch("a11111111111b"));
        }
    }

    @Test
    public void testExact() throws Exception {
        try (JniPcre pcre = new JniPcre(useHyperscan)) {
            int first = pcre.addRegex("a.{3}b");
            Assert.assertEquals(-1, pcre.findMatch("ab"));
            Assert.assertEquals(-1, pcre.findMatch("a11b"));
            Assert.assertEquals(first, pcre.findMatch("a111b"));
            Assert.assertEquals(-1, pcre.findMatch("a1111b"));
        }
    }
}

