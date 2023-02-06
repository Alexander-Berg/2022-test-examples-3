package ru.yandex.common.mining.aliaser.block;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import ru.yandex.common.mining.aliaser.PropertyContributor;
import ru.yandex.common.mining.bd.domex.DomExtractor;
import ru.yandex.common.mining.formalizer.*;
import ru.yandex.common.mining.property.Property;
import ru.yandex.common.util.IOUtils;
import static ru.yandex.common.util.collections.CollectionFactory.newList;
import ru.yandex.common.util.html.HtmlUtils;
import ru.yandex.common.util.http.Page;
import ru.yandex.common.util.http.charset.RussianCharsetDetector;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 23:23:06 03.01.2008
 *
 * @author jkff
 */
public class ParamsBlockFinderTest {
    TemplaterWordsDao pd;
    ParamsBlockFinder bf;

    DefaultKindDetector kd = new DefaultKindDetector();
    TemplaterWordsDao autoTWD;
    ParamsBlockFinder autoBF;
    DefaultKindDetector autoKD;
    ParamsBlockFinder addressBF;
    TemplaterWordsDao addressTWD;
    DefaultKindDetector addressKD ;

    {
        pd = new FileTemplaterWordsDao("params.txt", null);
        bf = new ParamsBlockFinder();
        kd.setWordsDao(pd);
        bf.setKindDetector(kd);

        autoTWD = new FileTemplaterWordsDao("au_param.txt", "au_values.txt");
        autoBF = new ParamsBlockFinder();
        autoKD = new AutoKindDetector();
        autoKD.setWordsDao(autoTWD);
        autoBF.setKindDetector(autoKD);

        addressTWD = new FileTemplaterWordsDao("adr_param.txt", "adr_values.txt");
        addressBF = new ParamsBlockFinder();
        addressKD = new AutoKindDetector();
        addressKD.setWordsDao(addressTWD);
        addressBF.setKindDetector(addressKD);
    }

    public static void main(String[] args) throws Exception {
        ParamsBlockFinderTest t = new ParamsBlockFinderTest();
        t.testOnLG();
        t.testOnPanasonic();
        t.testOnPhilips();
        t.testOnRolsen();
        t.testPropertyContributorOnAutochel();
        t.testPropertyContributorOnAutosurgut();
        t.testPropertyContributorOnCarsautoru();
        t.testPropertyContributorOnE1();
        t.testPropertyContributorOnPanasonic();
        t.testPropertyContributorOnPhilips();
        t.testPropertyContributorOnRolsen();
        t.testPropertyContributorOnSamsung();
        t.testPropertyContributorOnSony();
    }

    @Test
    public void testPropertyContributorOnAutochel() throws Exception {
        testAutoPropertiesContributorOn(parseDocs("autochel", "1.html", "2.html", "3.html"));
    }

    @Test
    public void testPropertyContributorOnBibika() throws Exception {
        testAutoPropertiesContributorOn(parseDocs("bibika", "2.htm", "1.htm"));
    }

    @Test
    public void testPropertyContributorOnE1() throws Exception {
        testAutoPropertiesContributorOn(parseDocs("e1", "290508.html"));
    }

    @Test
    public void testPropertyContributorOnCarsautoru() throws Exception {
        testAutoPropertiesContributorOn(parseDocs("carsautoru", "1.htm", "2.htm", "3.htm", "4.htm", "5.htm"));
    }

    @Test
    public void testPropertyContributorOnAutodrom() throws Exception {
        testAutoPropertiesContributorOn(parseDocs("autodrom", "1.htm"));
    }

    @Test
    public void testPropertyContributorOnAutosurgut() throws Exception {
        testAutoPropertiesContributorOn(parseDocs("autosurgut", "3177.html", "9241.html"));
    }

    @Test
    public void testPropertyContributorOnCarox() throws Exception {
        testAddressPropertiesContributorOn(parseDocs("carox", "carox-2t.html"));
    }

    @Test
    public void testPropertyContributorOnRinap() throws Exception {
        testAddressPropertiesContributorOn(parseDocs("rinap", "armand.html"));
    }
    
    @Test
    public void testPropertyContributorOnOtels() throws Exception {
        testAddressPropertiesContributorOn(parseDocs("otels", "1193.htm"));
    }

    @Test
    public void testPropertyContributorOn2rGrenada() throws Exception {
        testAddressPropertiesContributorOn(parseDocs("2r", "grenada.htm"));
    }

    private void testAutoPropertiesContributorOn(List<Document> docs) {
        testParamsBlockFinderOn(autoBF, docs);
    }

    private void testAddressPropertiesContributorOn(List<Document> docs) {
        testParamsBlockFinderOn(addressBF, docs);
    }

    private void testParamsBlockFinderOn(ParamsBlockFinder bf, List<Document> docs) {
        PropertyContributor x = bf.makePropertyContributor(docs);

        for (Document doc : docs) {
            List<Property> ps = newList();
            x.contribute(doc.getDocumentElement(), ps);
            for (Property p : ps) {
                System.out.println(p);
            }
            System.out.println("--------------");
        }
    }

    @Test
    public void testOnRolsen() throws Exception {
        List<Document> docs = parseDocs("rolsen", "rl-42d60d.html", "iis5.html", "rp50h20.html", "t2054ts.html", "rn5280.html");
        DomExtractor x = bf.makeParamsBlockExtractor(docs);

        for (Document doc : docs) {
            List<Element> els = x.extractElements(doc);
            assertSame(doc.getElementById("tech"), els.get(0));
            assertEquals(1, els.size());
        }
    }

    @Test
    public void testOnPhilips() throws Exception {
        List<Document> docs = parseDocs("philips", "42PFL9632D-10.htm", "52PFL7762D-12.htm");
        DomExtractor x = bf.makeParamsBlockExtractor(docs);

        for (Document doc : docs) {
            List<Element> els = x.extractElements(doc);
//            assertEquals(1, els.size());
//            assertEquals("clearfix", els.get(0).getAttribute("class"));
        }
    }

    @Test
    public void testOnDialogInvest() throws Exception {
        Document doc = parseDoc("dialoginvest", "w-2000-4.html");
        List<Property> props = bf.findProperties(doc);
        assertTrue(props.size() > 10);
    }


    @Test
    public void testOnDigitalExpress() throws Exception{
        testPropertyContributorOn(
            Arrays.asList(new Page(null,
                IOUtils.readInputStream(getClass().getResourceAsStream("/canon-digital-ixus-90.html"))).getDocument())
        );
    }

    @Test
    public void testOnPanasonic() throws Exception {
        List<Document> docs = parseDocs("panasonic", "DVD-S33EE-S.htm", "SC-PT250EE-S.htm");
        DomExtractor x = bf.makeParamsBlockExtractor(docs);

        for (Document doc : docs) {
            List<Element> els = x.extractElements(doc);
            assertEquals(1, els.size());
            assertTrue(els.get(0).getAttribute("class").equals("ttx") ||
                    ((Element) els.get(0).getParentNode()).getAttribute("class").equals("ttx"));
        }
    }

//    @Test
//    public void testFindLeafSignificantTexts() throws Exception {
//        Document doc = parseDocs("panasonic", "DVD-S33EE-S.htm").get(0);
//        Element tr = doc.getElementById("theOneIAmInterestedIn");
//
//        assertEquals(2, bf.getTextNodes(tr).size());
//    }

    @Test
    public void testOnLG() throws Exception {
        List<Document> docs = parseDocs("lg", "A12HL1.htm", "A18LH.htm");
        DomExtractor x = bf.makeParamsBlockExtractor(docs);

        for (Document doc : docs) {
            List<Element> els = x.extractElements(doc);
            assertEquals(1, els.size());
//            assertTrue(els.get(0).getTextContent().trim().startsWith("Холодопроизводительность"));
        }
    }

    @Test
    public void testPropertyContributorOnPhilips() throws Exception {
        testPropertyContributorOn(parseDocs("philips", "42PFL9632D-10.htm", "52PFL7762D-12.htm"));
    }


    @Test
    public void testPropertyContributorOnPanasonic() throws Exception {
        testPropertyContributorOn(parseDocs("panasonic", "DVD-S33EE-S.htm", "SC-PT250EE-S.htm"));
    }


    @Test
    public void testPropertyContributorOnSony() throws Exception {
        testPropertyContributorOn(parseDocs("sony", "KDL-26S3000.htm"));
    }

    @Test
    public void testPropertyContributorOnRolsen() throws Exception {
        testPropertyContributorOn(parseDocs("rolsen", "rl-42d60d.html", "iis5.html", "rn5280.html", "rp50h20.html", "t2054ts.html"));
    }

    @Test
    public void testPropertyContributorOnSamsung() throws Exception {
        testPropertyContributorOn(parseDocs("samsung", "DVD-P750.htm"));
    }

    private void testPropertyContributorOn(List<Document> docs) {
        PropertyContributor x = bf.makePropertyContributor(docs);

        for (Document doc : docs) {
            List<Property> ps = new ArrayList<Property>();
            x.contribute(doc.getDocumentElement(), ps);
            for (Property p : ps) {
                System.out.println(p);
            }
            System.out.println("--------------");
        }
    }


    @Test
    public void testLooksLikeValue() throws Exception {
        assertTrue(kd.looksLikeValue("183x396x267 мм"));
        assertTrue(kd.looksLikeValue("4 кг"));
        assertTrue(kd.looksLikeValue("1,25 кг"));
        assertTrue(kd.looksLikeValue("108 МГц/12 бит  "));
        assertTrue(kd.looksLikeValue("96 кГц / 24        бит  "));
        assertTrue(kd.looksLikeValue("250 Вт (100 кГц, 6Ом, 10% THD)  "));
        assertTrue(kd.looksLikeValue("125 Вт x 2 (1 кГц, 3 Ом, 10% THD)  "));
        assertTrue(kd.looksLikeValue("16-см, конусный"));
        assertTrue(kd.looksLikeValue("6,5-см, конусный x 2  "));
//        assertTrue(kd.looksLikeValue("PAL 625/50, PAL 525/60, NTSC ")) ;
    }


    @Test
    public void testLooksLikeParam() throws Exception {
        assertTrue(kd.looksLikeParam("Энергопотребление в режиме ожидания (Вт)"));
        assertTrue(kd.looksLikeParam("Экранное меню"));
        assertTrue(kd.looksLikeParam("Разъемы"));
    }

   @Test
    public void testSlipper() throws Exception{
       List<Document> docs = parseDocs("autosurgut", "3177.html");
//       List<Document> docs = parseDocs("bibika", "1.html");
//       List<Document> docs = parseDocs("carsautoru", "1.htm","2.htm","3.htm","4.htm","5.htm");
       SlidingPropertyFinder spf = new SlidingPropertyFinder(autoKD);

       for (Document doc : docs) {
           System.out.println("---NEW DOCUMENT---");
          spf.findProperties(doc);
       }
   }

    private List<Document> parseDocs(String prefix, String... names) throws Exception {
        List<Document> docs = new ArrayList<Document>();
        for (String f : names) {
            docs.add(parseDoc(prefix, f));
        }

        return docs;
    }

    private Document parseDoc(String prefix, String file) throws IOException, SAXException {
        byte[] bytes = IOUtils.readInputStreamToBytes(getClass().getResourceAsStream("/" + prefix + "/" + file));
        Charset cs = new RussianCharsetDetector().detectActualCharset(bytes, null);
        Document doc = HtmlUtils.parse(new String(bytes, cs.name()));
        return doc;
    }
}
