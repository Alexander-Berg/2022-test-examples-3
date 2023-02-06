package ru.yandex.ir.parser.clutcher;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import ru.yandex.utils.IoUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * todo описать предназначение.
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class XmlDataLoaderTest {
    private static final Logger log = LoggerFactory.getLogger(XmlDataLoaderTest.class);

    @Test
    public void testXmlDataParsing() {
//        log.info("XmlDataLoaderTest.testXmlDataParsing");
//        try {
//            String dumpFile = "./src/test/resources/cat-example.xml";
//            checkDumpFile(dumpFile);
//            for (File dump : new File("/home/shurk/projects/clutcher/xml-dumps/20120829/").listFiles(new FilenameFilter() {
//                @Override
//                public boolean accept(File dir, String name) {
//                    return name.startsWith("clutcher_category_");
//                }
//            })) {
//                checkDumpFile(dump.getAbsolutePath());
//            }
//        } catch (Exception e) {
//            log.error(e, e);
//            fail(e.getMessage());
//        }
    }

    private void checkDumpFile(String dumpFile) throws IOException, ParserConfigurationException, SAXException {
        final BufferedInputStream stream = new BufferedInputStream(IoUtils.openInputStream(dumpFile), 0x80000);
        ArrayList<Category> categories = new ArrayList<>();
        XmlDataLoader.loadCategoryObjectsFromStream(stream, categories);
        assertEquals(1, categories.size());
    }
}
