package ru.yandex.chemodan.uploader.preview;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.image.ImageFormat;
import ru.yandex.commune.image.RotateAngle;
import ru.yandex.misc.image.Dimension;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class ConvertReportParserTest {
    private ConvertReportParser parser = new ConvertReportParser();

    @Test
    public void parseReport() throws Exception {
        String report =
                "orig-format:JPEG\n" +
                "orig-size:792x1188\n" +
                "orientation:6\n" +
                "tmpprev1.jpg:792x1188\n";

        PreviewInfo info = new PreviewInfo(ImageFormat.JPEG,
                Option.of(new Dimension(792, 1188)), new Dimension(792, 1188), Option.of(RotateAngle.D90));
        Assert.equals(parser.parseReport(report, "tmpprev1.jpg"), info);
    }

    @Test
    public void parseReportWithEmptyOrientation() throws Exception {
        String report =
                "orig-format:JPEG\n" +
                "orig-size:600x800\n" +
                "orientation:\n" +
                "tmpprev1.jpg:300x400\n";

        PreviewInfo info = new PreviewInfo(ImageFormat.JPEG,
                Option.of(new Dimension(600, 800)), new Dimension(300, 400), Option.empty());
        Assert.equals(parser.parseReport(report, "tmpprev1.jpg"), info);
    }

}
