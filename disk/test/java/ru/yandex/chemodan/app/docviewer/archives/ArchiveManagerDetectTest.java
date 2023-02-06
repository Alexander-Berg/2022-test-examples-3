package ru.yandex.chemodan.app.docviewer.archives;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.commune.archive.ArchiveManager;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;

@RunWith(Parameterized.class)
public class ArchiveManagerDetectTest {

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<>();

        for (URL resource : new URL[] {
                TestResources.Apache_Poi_Examples_Documents,
                TestResources.Apache_Poi_Examples_Spreadsheets,
                TestResources.Microsoft_Word_12_001p,
                TestResources.OpenOffice_Calc_3_0_001p,
                TestResources.OpenOffice_Impress_3_3_001p,
                TestResources.OpenOffice_Writer_3_2_001p,
                TestResources.ZIP,
                TestResources.RAR,
                TestResources.TAR_BZ2,
                TestResources.TAR_GZ,
        })
        {
            data.add(new Object[] { resource, Boolean.TRUE });
        }

        for (URL resource : new URL[] {
                TestResources.Adobe_Acrobat_1_3_001p,
                TestResources.Adobe_Photoshop_CS2,
                TestResources.Adobe_Photoshop_CS5,
                TestResources.HTML,
                TestResources.JPEG,
                TestResources.Microsoft_EMF,
                TestResources.Microsoft_Excel_97_001p,
                TestResources.Microsoft_PowerPoint_97_001p,
                TestResources.Microsoft_RTF,
                TestResources.Microsoft_WMF_delta,
                TestResources.Microsoft_Word_95_001p,
                TestResources.Microsoft_Word_97_001p,
                TestResources.Nikon_D300,
                TestResources.SVG,
                TestResources.TIFF,
        })
        {
            data.add(new Object[] { resource, Boolean.FALSE });
        }

        return data;
    }

    private ArchiveManager archiveManager = new ArchiveManager();

    private final boolean expectedResult;

    private final URL url;

    public ArchiveManagerDetectTest(URL url, boolean expectedResult) {
        this.url = url;
        this.expectedResult = expectedResult;
    }

    @Test
    public void isArchive() {
        Assert.equals(expectedResult, archiveManager.isArchive(new UrlInputStreamSource(url)));
    }

    @Test
    public void list() {
        if (expectedResult) {
            Assert.isTrue(archiveManager.listArchive(new UrlInputStreamSource(url)).getEntries().isNotEmpty());
        }
    }
}
