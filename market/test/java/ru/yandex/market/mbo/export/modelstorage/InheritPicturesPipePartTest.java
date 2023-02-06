package ru.yandex.market.mbo.export.modelstorage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.export.modelstorage.pipe.InheritPicturesPipePart;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipeContext;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipePart;
import ru.yandex.market.mbo.export.modelstorage.pipe.Pipe;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InheritPicturesPipePartTest {

    private ModelPipePart pipePart;
    private Long modelIdCounter = 0L;
    private ModelPipeContext context;
    private DummyPipePart dummyPipePart;

    @Before
    public void init() {
        dummyPipePart = new DummyPipePart();
        pipePart = Pipe.simple(new InheritPicturesPipePart(), dummyPipePart);
    }

    @Test
    public void testIfAllEmpty() throws Exception {
        List<ModelStorage.Model> modifications = new ArrayList<>();

        ModelStorage.Model model = model();

        modifications.add(model());
        modifications.add(model());

        pipePart.acceptModelsGroup(context(model, modifications));

        Assert.assertEquals(0, dummyPipePart.getModifications().get(0).getPictures().size());
        Assert.assertEquals(0, dummyPipePart.getModifications().get(1).getPictures().size());
    }

    @Test
    public void testIfModifPicturesEmpty() throws Exception {
        List<ModelStorage.Model> modifications = new ArrayList<>();

        ModelStorage.Model model = model(picture("xsl1", "url1"), picture("xsl2", "url2"));

        modifications.add(model());
        modifications.add(model());

        pipePart.acceptModelsGroup(context(model, modifications));

        Assert.assertEquals(2, dummyPipePart.getModifications().get(0).getPictures().size());
        Assert.assertEquals(2, dummyPipePart.getModifications().get(1).getPictures().size());
    }

    @Test
    public void testIfModifPicturesNotEmpty() throws Exception {
        List<ModelStorage.Model> modifications = new ArrayList<>();

        ModelStorage.Model model = model(picture("xsl1", "url1"), picture("xsl2", "url2"));

        modifications.add(model(picture("xsl1", "url11")));
        modifications.add(model(picture("xsl2", "url22")));

        pipePart.acceptModelsGroup(context(model, modifications));

        List<Picture> mod1PicturesResult = dummyPipePart.getModifications().get(0).getPictures();
        List<Picture> mod2PicturesResult = dummyPipePart.getModifications().get(1).getPictures();

        Assert.assertEquals("url11", mod1PicturesResult.get(0).getUrl());
        Assert.assertEquals("url2", mod1PicturesResult.get(1).getUrl());
        Assert.assertEquals("url1", mod2PicturesResult.get(0).getUrl());
        Assert.assertEquals("url22", mod2PicturesResult.get(1).getUrl());
    }

    @Test
    public void testIfModelPicturesEmpty() throws Exception {
        List<ModelStorage.Model> modifications = new ArrayList<>();

        ModelStorage.Model model = model();

        modifications.add(model(picture("xsl1", "url1"), picture("xsl11", "url11")));
        modifications.add(model(picture("xsl2", "url22")));

        pipePart.acceptModelsGroup(context(model, modifications));

        Assert.assertEquals(2, dummyPipePart.getModifications().get(0).getPictures().size());
        Assert.assertEquals(1, dummyPipePart.getModifications().get(1).getPictures().size());
    }

    private ModelPipeContext context(ModelStorage.Model model, List<ModelStorage.Model> modifications) {
        this.context = new ModelPipeContext(model, modifications, Collections.emptyList());
        return this.context;
    }

    private ModelStorage.Model model(ModelStorage.Picture... pictures) {
        modelIdCounter++;
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
        builder.addAllPictures(Arrays.asList(pictures)).setId(modelIdCounter);
        return builder.build();
    }

    private ModelStorage.Picture picture(String xslName, String url) {
        ModelStorage.Picture.Builder builder = ModelStorage.Picture.newBuilder();
        builder.setXslName(xslName);
        builder.setUrl(url);
        return builder.build();
    }

}
