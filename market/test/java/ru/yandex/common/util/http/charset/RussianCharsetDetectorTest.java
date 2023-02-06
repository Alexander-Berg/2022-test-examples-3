package ru.yandex.common.util.http.charset;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.junit.Test;

import ru.yandex.common.util.CanonicalCharset;
import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.text.Charsets;

import static org.junit.Assert.assertEquals;

/**
 * Created on 31.05.2007 18:10:09
 *
 * @author Eugene Kirpichov jkff@yandex-team.ru
 */
public class RussianCharsetDetectorTest {
    private final CharsetDetector detector = new RussianCharsetDetector();

    @Test
    public void testFixCp1251ToKoi8OnActualHtml() throws Exception {
        byte[] page = IOUtils.readInputStreamToBytes(
                getClass().getResourceAsStream("/mega-zephal-news-cp1251.html")
        );
        assertEquals(Charset.forName("koi8-r"),
                detector.detectActualCharset(page, Charset.forName("windows-1251")));
    }

    @Test
    public void testRolsenCorrect1251() throws Exception {
        byte[] page = IOUtils.readInputStreamToBytes(
                getClass().getResourceAsStream("/rolsen-digislim-cp1251-correct.html")
        );
        assertEquals(Charset.forName("windows-1251"),
                detector.detectActualCharset(page, Charset.forName("windows-1251")));
    }

    @Test
    public void testZyxel() throws Exception {
        byte[] page = IOUtils.readInputStreamToBytes(
                getClass().getResourceAsStream("/zyxel.html")
        );
        assertEquals(Charsets.UTF_8,
                detector.detectActualCharset(page, Charset.forName("windows-1251")));
    }

    @Test
    public void testJabra() throws Exception {
        byte[] page = IOUtils.readInputStreamToBytes(
                getClass().getResourceAsStream("/jabra.html")
        );
        assertEquals(Charsets.UTF_8,
                detector.detectActualCharset(page, Charset.forName("windows-1251")));
    }

    @Test
    public void testOnAllCombinations() throws Exception {
        // One sentence
        testAllCombinationsOn(
                "2 февраля 2007 года, при проведении работ по переводу " +
                        "сайта из кодировки windows-1251 в кодировку UTF-8, на " +
                        "сервере произошёл сбой, в результате которого была утрачена " +
                        "база данных, начиная с 31 января 2007 года. Часть " +
                        "потерянной информации удалось восстановить из кеша поисковиков, " +
                        "остальная же, к сожалению, утрачена безвозвратно. ");
        // Several sentences, mixed language
        String war = "-- Еh bien, mon prince. G?nes et Lucques ne sont plus que des " +
                "apanages, des поместья, de la famille Buonaparte. Non, je vous " +
                "pr?viens, que si vous ne me dites pas, que nous avons la guerre, " +
                "si vous vous permettez encore de pallier toutes les infamies, " +
                "toutes les atrocit?s de cet Antichrist (ma parole, j'y crois) -- " +
                "je ne vous connais plus, vous n'?tes plus mon ami, vous n'?tes " +
                "plus мой верный раб, comme vous dites. 1 Ну, здравствуйте, " +
                "здравствуйте. Je vois que je vous fais peur, 2 садитесь и рассказывайте. \n" +
                "\n" +
                "Так говорила в июле 1805 года известная Анна Павловна Шерер, фрейлина и " +
                "приближенная императрицы Марии Феодоровны, встречая важного и чиновного " +
                "князя Василия, первого приехавшего на ее вечер. Анна Павловна кашляла " +
                "несколько дней, у нее был грипп, как она говорила (грипп был тогда новое " +
                "слово, употреблявшееся только редкими). В записочках, разосланных утром с " +
                "красным лакеем, было написано без различия во всех: \n" +
                "\n" +
                "\"Si vous n'avez rien de mieux ? faire, M. le comte (или mon prince), et si " +
                "la perspective de passer la soir?e chez une pauvre malade ne vous effraye " +
                "pas trop, je serai charm?e de vous voir chez moi entre 7 et 10 heures. " +
                "Annette Scherer\".3 \n" +
                "\n" +
                "-- Dieu, quelle virulente sortie 4 -- отвечал, нисколько не смутясь такою " +
                "встречей, вошедший князь, в придворном, шитом мундире, в чулках, башмаках, " +
                "при звездах, с светлым выражением плоского лица. Он говорил на том изысканном " +
                "французском языке, на котором не только говорили, но и думали наши деды, и с " +
                "теми тихими, покровительственными интонациями, которые свойственны состаревшемуся " +
                "в свете и при дворе значительному человеку. Он подошел к Анне Павловне, " +
                "поцеловал ее руку, подставив ей свою надушенную и сияющую лысину, и " +
                "покойно уселся на диване.";
        testAllCombinationsOn(war);

        // Blondie style
        testAllCombinationsOn(war.toUpperCase());
        // And vice versa
        testAllCombinationsOn(war.toLowerCase());
    }

    @Test
    public void testOnAvtomarketXML() throws Exception {
        byte[] page = IOUtils.readInputStreamToBytes(
                getClass().getResourceAsStream("/avtomarket.xml")
        );
        assertEquals(Charset.forName("windows-1251"),
                detector.detectActualCharset(page, Charset.forName("koi8-r")));
    }

    @Test
    public void testOnMailRu() throws Exception {
        byte[] page = IOUtils.readInputStreamToBytes(
                getClass().getResourceAsStream("/carsmailru.html")
        );
        assertEquals(Charset.forName("windows-1251"), detector.detectActualCharset(page, Charset.forName("koi8-r")));
    }


    @Test
    public void testOnYandexRu() throws Exception {
        byte[] page = IOUtils.readInputStreamToBytes(
                getClass().getResourceAsStream("/yandexru.html")
        );
        assertEquals(Charset.forName("windows-1251"), detector.detectActualCharset(page, Charsets.UTF_8));
    }

    @Test
    public void testOnAvtoMarket() throws Exception {
        byte[] page = IOUtils.readInputStreamToBytes(
                getClass().getResourceAsStream("/avtomarket.html")
        );
        assertEquals(Charset.forName("windows-1251"), detector.detectActualCharset(page, Charset.forName("koi8-r")));
    }

    @Test
    public void testOnSay7Info() throws Exception {
        byte[] page = IOUtils.readInputStreamToBytes(
                getClass().getResourceAsStream("/say7info.html")
        );
        assertEquals(Charset.forName("windows-1251"), detector.detectActualCharset(page, Charset.forName("koi8-r")));
    }

    private void testAllCombinationsOn(String correct) throws UnsupportedEncodingException {
        final Charset[] charsets = new Charset[]{
                CanonicalCharset.forName("koi8-r"),
                CanonicalCharset.forName("utf-8"),
                CanonicalCharset.forName("windows-1251")
        };

        for (Charset expected : charsets) {
            for (Charset to : charsets) {
                Charset actual = detector.detectActualCharset(correct.getBytes(expected.name()), to);

                assertEquals("From: " + expected.name() + ", To: " + to.name(),
                        expected, actual);
            }
        }
    }
}


