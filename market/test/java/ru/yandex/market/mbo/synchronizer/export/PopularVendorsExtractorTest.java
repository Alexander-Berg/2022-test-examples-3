package ru.yandex.market.mbo.synchronizer.export;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.PopularVendors;

import java.io.StringWriter;
import java.io.Writer;

/**
 * @author dmserebr
 * @date 29.05.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class PopularVendorsExtractorTest {
    private PopularVendorsServiceMock popularVendorsService;
    private PopularVendorsExtractor popularVendorsExtractor;

    @Before
    public void before() {
        popularVendorsService = new PopularVendorsServiceMock();
        popularVendorsExtractor = new PopularVendorsExtractor();
        popularVendorsExtractor.setPopularVendorsService(popularVendorsService);
    }

    @Test
    public void extractEmptyCategory() throws Exception {
        Writer writer = new StringWriter();
        popularVendorsExtractor.writePopularVendors(writer);

        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<popularVendors/>\n";
        Assert.assertEquals(expectedXml, writer.toString());
    }

    @Test
    public void extractOnePopularVendorForOneCategory() throws Exception {
        PopularVendors vendors = new PopularVendorsBuilder()
            .categoryId(90401L)
            .startList()
                .vendor(10L)
                .vendor(11L)
                .region(10000L)
            .endList()
            .build();
        popularVendorsService.save(vendors);

        Writer writer = new StringWriter();
        popularVendorsExtractor.writePopularVendors(writer);

        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<popularVendors>\n" +
            " <category hid=\"90401\">\n" +
            "  <list>\n" +
            "   <vendors>10,11</vendors>\n" +
            "   <regions>10000</regions>\n" +
            "  </list>\n" +
            " </category>\n" +
            "</popularVendors>\n";
        Assert.assertEquals(expectedXml, writer.toString());
    }

    @Test
    public void extractSeveralPopularVendorsForOneCategory() throws Exception {
        PopularVendors vendors = new PopularVendorsBuilder()
            .categoryId(90401L)
            .startList()
                .vendor(10L)
                .vendor(11L)
                .region(100L)
            .endList()
            .startList()
                .vendor(10L)
                .vendor(12L)
                .region(101L)
            .endList()
            .startList()
                .vendor(12L)
                .vendor(13L)
                .region(10000L)
                .blue(true)
            .endList()
            .build();
        popularVendorsService.save(vendors);

        Writer writer = new StringWriter();
        popularVendorsExtractor.writePopularVendors(writer);

        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<popularVendors>\n" +
            " <category hid=\"90401\">\n" +
            "  <list>\n" +
            "   <vendors>10,11</vendors>\n" +
            "   <regions>100</regions>\n" +
            "  </list>\n" +
            "  <list>\n" +
            "   <vendors>10,12</vendors>\n" +
            "   <regions>101</regions>\n" +
            "  </list>\n" +
            "  <list-blue>\n" +
            "   <vendors>12,13</vendors>\n" +
            "   <regions>10000</regions>\n" +
            "  </list-blue>\n" +
            " </category>\n" +
            "</popularVendors>\n";
        Assert.assertEquals(expectedXml, writer.toString());
    }

    @Test
    public void extractSeveralCategories() throws Exception {
        PopularVendors vendors1 = new PopularVendorsBuilder()
            .categoryId(12345L)
            .startList()
                .vendor(10L)
                .vendor(11L)
                .region(100L)
            .endList()
            .build();
        PopularVendors vendors2 = new PopularVendorsBuilder()
            .categoryId(23456L)
            .startList()
                .vendor(20L)
                .vendor(11L)
                .region(200L)
            .endList()
            .startList()
                .vendor(12L)
                .vendor(13L)
                .vendor(20L)
                .region(100L)
                .region(101L)
                .blue(true)
            .endList()
            .build();
        PopularVendors vendors3 = new PopularVendorsBuilder()
            .categoryId(34567L)
            .startList()
                .vendor(12L)
                .region(100L)
                .blue(true)
            .endList()
            .build();
        popularVendorsService.save(vendors1);
        popularVendorsService.save(vendors2);
        popularVendorsService.save(vendors3);

        Assert.assertEquals(1, popularVendorsService.getCount(10L));
        Assert.assertEquals(2, popularVendorsService.getCount(11L));
        Assert.assertEquals(2, popularVendorsService.getCount(12L));
        Assert.assertEquals(0, popularVendorsService.getCount(34L));
        Assert.assertEquals(3, popularVendorsService.getCount(null));

        Writer writer = new StringWriter();
        popularVendorsExtractor.writePopularVendors(writer);

        String expectedXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<popularVendors>\n" +
            " <category hid=\"23456\">\n" +
            "  <list>\n" +
            "   <vendors>20,11</vendors>\n" +
            "   <regions>200</regions>\n" +
            "  </list>\n" +
            "  <list-blue>\n" +
            "   <vendors>12,13,20</vendors>\n" +
            "   <regions>100,101</regions>\n" +
            "  </list-blue>\n" +
            " </category>\n" +
            " <category hid=\"34567\">\n" +
            "  <list-blue>\n" +
            "   <vendors>12</vendors>\n" +
            "   <regions>100</regions>\n" +
            "  </list-blue>\n" +
            " </category>\n" +
            " <category hid=\"12345\">\n" +
            "  <list>\n" +
            "   <vendors>10,11</vendors>\n" +
            "   <regions>100</regions>\n" +
            "  </list>\n" +
            " </category>\n" +
            "</popularVendors>\n";
        Assert.assertEquals(expectedXml, writer.toString());
    }
}
