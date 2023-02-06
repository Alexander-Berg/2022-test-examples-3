package ru.yandex.market.ir.nirvana.modelpublisher;

import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;

/**
 * @author inenakhov
 */
public class UtilTest {
    private static final String dummyFilePath = "dummy.tsv";
    private static final String dummyBrokenFilePath = "dummy_broken.tsv";
    private static final String emptyFilePath = "empty.tsv";

    @Test(expected = RuntimeException.class)
    public void readModelsBrokenFile() throws Exception {
        URL resource = Resources.getResource(dummyBrokenFilePath);

        Util.readModels(resource.getPath());
    }

    @Test
    public void readModelsEmptyFile() throws Exception {
        URL resource = Resources.getResource(emptyFilePath);

        Assert.assertTrue(Util.readModels(resource.getPath()).isEmpty());
    }

    @Test
    public void readModels() throws Exception {
        URL resource = Resources.getResource(dummyFilePath);

        List<Model> models = Util.readModels(resource.getPath());

        Assert.assertEquals(3, models.size());
        Model firstModel = models.get(0);
        Assert.assertEquals(1, firstModel.getCategoryId());
        Assert.assertEquals(2, firstModel.getId());

        Model secondModel = models.get(1);
        Assert.assertEquals(1, secondModel.getCategoryId());
        Assert.assertEquals(3, secondModel.getId());

        Model thirdModel = models.get(2);
        Assert.assertEquals(2, thirdModel.getCategoryId());
        Assert.assertEquals(1, thirdModel.getId());
    }

    @Test
    public void parseRow() throws Exception {
        String dummyRow = "1\t2";

        Model model = Util.parseRow(dummyRow);
        Assert.assertEquals(1, model.getCategoryId());
        Assert.assertEquals(2, model.getId());
    }
}