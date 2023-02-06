package ru.yandex.common.mining.language;

import org.junit.Test;
import org.xml.sax.SAXException;
import ru.yandex.common.mining.bd.miner.DomAnalysis;
import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.html.HtmlUtils;
import ru.yandex.common.util.http.charset.RussianCharsetDetector;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;
import static ru.yandex.common.util.collections.CollectionFactory.newList;
import static ru.yandex.common.util.collections.CollectionFactory.newUnorderedMap;

/**
 * Created on 19:17:14 19.01.2009
 *
 * @author jkff
 */
public class LanguageDetectorTest {
    @Test
    public void testOnSonyEricsson() throws Exception {
        String prefix = "/language-check/sony-ericsson/";

        assertTrue(LanguageDetectors.isRussian(readLimitedFile(prefix + "russian-k610i.html")));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(prefix + "russian-k610i.html")));

        assertFalse(LanguageDetectors.isRussian(readLimitedFile(prefix + "english-k610i.html")));
        assertTrue(LanguageDetectors.isEnglish(readLimitedFile(prefix + "english-k610i.html")));

        assertFalse(LanguageDetectors.isRussian(readLimitedFile(prefix + "french-k610i.html")));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(prefix + "french-k610i.html")));

        assertFalse(LanguageDetectors.isRussian(readLimitedFile(prefix + "israeli-k610i.html")));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(prefix + "israeli-k610i.html")));

        assertFalse(LanguageDetectors.isRussian(readLimitedFile(prefix + "norwegian-k610i.html")));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(prefix + "norwegian-k610i.html")));

        assertFalse(LanguageDetectors.isRussian(readLimitedFile(prefix + "spanish-k610i.html")));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(prefix + "spanish-k610i.html")));

        assertFalse(LanguageDetectors.isRussian(readLimitedFile(prefix + "polish-k610i.html")));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(prefix + "polish-k610i.html")));

        assertFalse(LanguageDetectors.isRussian(readLimitedFile(prefix + "portugalian-k610i.html")));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(prefix + "portugalian-k610i.html")));

        assertFalse(LanguageDetectors.isRussian(readLimitedFile(prefix + "spanish-k610i.html")));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(prefix + "spanish-k610i.html")));

        assertFalse(LanguageDetectors.isRussian(readLimitedFile(prefix + "taiwanian-k610i.html")));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(prefix + "taiwanian-k610i.html")));

        assertFalse(LanguageDetectors.isRussian(readLimitedFile(prefix + "netherlands-k610i.html")));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(prefix + "netherlands-k610i.html")));

        assertFalse(LanguageDetectors.isRussian(readLimitedFile(prefix + "bulgarian-k610i.html")));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(prefix + "bulgarian-k610i.html")));
    }

    @Test
    public void testOnGeneralBG() throws Exception {
        String f = "/language-check/generalbg-arha24l.html";
        assertFalse(LanguageDetectors.isRussian(readLimitedFile(f)));
        assertFalse(LanguageDetectors.isEnglish(readLimitedFile(f)));
        assertFalse(LanguageDetectors.isRussianOrEnglish(readLimitedFile(f)));
    }

    @Test
    public void testOnBigSample() throws Exception {
        /*
        Файл sandbox/language-check/big-sample.zip недоступен.

        Map<String,Boolean> file_actual = newUnorderedMap();
        Map<String,Boolean> file_guessed = newUnorderedMap();
        Map<String,String> file_url = newUnorderedMap();


        ZipFile f = new ZipFile(new File("sandbox/language-check/big-sample.zip"));
        Enumeration<? extends ZipEntry> entries = f.entries();
        while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            InputStream stream = f.getInputStream(entry);
            if(name.endsWith(".url")) {
                file_url.put(name.replace(".url",""), IOUtils.readInputStream(stream));
            } else if(name.endsWith(".ans")) {
                file_actual.put(name.replace(".ans",""), IOUtils.readInputStream(stream).contains("1"));
            } else if(name.endsWith(".data")) {
                file_guessed.put(name.replace(".data",""), check(IOUtils.readInputStreamToBytes(stream, 204800)));
            }
        }

        int[][] actual_guessed_count = new int[2][2];

        for(String file : file_url.keySet()) {
            String url = file_url.get(file);
            boolean actual = file_actual.get(file);
            boolean guessed = file_guessed.get(file);
            actual_guessed_count[actual?1:0][guessed?1:0]++;

            if(actual != guessed) {
                byte[] bytes = IOUtils.readInputStreamToBytes(f.getInputStream(f.getEntry(file + ".data")), 204800);
                System.out.println(url + "Actual:"+actual+", Guessed:"+guessed+" ["+file+"] ("+bytes.length+" bytes)\n");
                check(bytes);
            }
        }

        System.out.println("Actual / Guessed:");
        System.out.println("0 / 0 : " + actual_guessed_count[0][0]);
        System.out.println("0 / 1 : " + actual_guessed_count[0][1]);
        System.out.println("1 / 0 : " + actual_guessed_count[1][0]);
        System.out.println("1 / 1 : " + actual_guessed_count[1][1]);
         */
    }

    @Test
    public void testOnSample() throws Exception {
        /*
        Map<String,Boolean> url_isRight = newUnorderedMap();
        Map<String,String> url_file = newUnorderedMap();
        Map<String,String> url_num = newUnorderedMap();
        FilenameFilter URL_EXT = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".url");
            }
        };

        for(File urlFile : new File(getClass().getResource("/language-check/actually-right").toURI())
            .listFiles(URL_EXT)) {
            String url = IOUtils.readWholeFile(urlFile.getAbsolutePath());
            url_isRight.put(url, true);
            url_file.put(url, urlFile.getAbsolutePath().replace(".url",".data"));
            url_num.put(url, urlFile.getAbsolutePath());
        }
        for(File urlFile : new File(getClass().getResource("/language-check/actually-wrong").toURI())
            .listFiles(URL_EXT)) {
            String url = IOUtils.readWholeFile(urlFile.getAbsolutePath());
            url_isRight.put(url, false);
            url_file.put(url, urlFile.getAbsolutePath().replace(".url",".data"));
            url_num.put(url, urlFile.getAbsolutePath());
        }

        int[][] actual_guessed_count = new int[2][2];

        for(String url : url_file.keySet()) {
            String urlFile = url_file.get(url);
            byte[] content = readLimitedBytes(urlFile);

            boolean actual = url_isRight.get(url);
            boolean guessed = check(content);

            actual_guessed_count[actual?1:0][guessed?1:0]++;

            if(actual != guessed) {
                System.out.println(url + "Actual:"+actual+", Guessed:"+guessed+" ["+url_num.get(url)+"]\n");
                check(content);
            }
        }

        System.out.println("Actual / Guessed:");
        System.out.println("0 / 0 : " + actual_guessed_count[0][0]);
        System.out.println("0 / 1 : " + actual_guessed_count[0][1]);
        System.out.println("1 / 0 : " + actual_guessed_count[1][0]);
        System.out.println("1 / 1 : " + actual_guessed_count[1][1]);
        */
    }

    @Test
    public void testOnRussian() throws Exception {
        /*
        List<String> failedFiles = newList();
        for(File f : new File(getClass().getResource("/language-check/in-russian/").toURI()).listFiles()) {
            if(f.isFile())
                if(!check(readLimitedBytes(f.getAbsolutePath())))
                    failedFiles.add(f.getName());
        }
        assertEquals(new ArrayList<String>(), failedFiles);
        */
    }

    private String readLimitedFile(String filename) throws Exception {
        return DomAnalysis.getScriptlessTextContent(HtmlUtils.parse(RussianCharsetDetector.parse(
            readLimitedBytes(filename)
        )).getDocumentElement());
    }

    private byte[] readLimitedBytes(String filename) throws IOException {
        return IOUtils.readInputStreamToBytes(getClass().getResourceAsStream(filename), 204800);
    }

    @Test
    public void testOnRussianSony() throws Exception {
        assertTrue(LanguageDetectors.isRussian(readLimitedFile("/sony/KDL-26S3000.htm")));
    }

    private static boolean check(byte[] content) {
        String s;
        try {
            s = DomAnalysis.getScriptlessTextContent(
                    HtmlUtils.parse(RussianCharsetDetector.parse(content)));
        } catch (SAXException e) {
            return false;
        }
        return LanguageDetectors.isRussianOrEnglish(s);
    }
}
