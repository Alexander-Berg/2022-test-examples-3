package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.RenameModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Date;

/**
 * @author gilmulla
 */
public class TestModelRenaming extends AbstractTest {

    private static final String OLD_NAME = "Old Name";
    private static final String NEW_NAME = "New Name";
    private static final String ALIAS_1 = "alias_1";
    private static final String ALIAS_2 = "alias_2";
    private static final Long MODEL_ID = 1L;
    private static final Long CATEGORY_ID = 2L;

    @Test
    public void testRenaming() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
            .startParameters()
            .startParameter()
            .xsl(XslNames.NAME).type(Param.Type.STRING).name("Имя")
            .endParameter()
            .startParameter()
            .xsl(XslNames.ALIASES).type(Param.Type.STRING).name("Алиасы")
            .endParameter()
            .startParameter()
            .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
            .hidden(true)
            .option(1, "Vendor1")
            .endParameter()
            .endParameters()
            .startModel()
            .id(MODEL_ID).category(CATEGORY_ID).source(Source.GURU).currentType(Source.GURU)
            .param(XslNames.VENDOR).setOption(1)
            .param(XslNames.NAME).setString(OLD_NAME)
            .param(XslNames.ALIASES)
            .setWords(
                new Word(Word.DEFAULT_LANG_ID, ALIAS_1),
                new Word(Word.DEFAULT_LANG_ID, ALIAS_2))
            .endModel()
            .startVendor()
            .source("http://source1", "ru", new Date())
            .endVendor()
            .tovarCategory(1, CATEGORY_ID.intValue());

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(new PlaceShowEvent(
            EditorUrlStub.of("modelEditor", "entity-id=1")));
        rpc.setSaveModel(MODEL_ID, null);

        Assert.assertEquals(OLD_NAME, data.getModel().getTitle());
        Assert.assertEquals("Модель: " + OLD_NAME, view.getWindowTitle());
        bus.fireEvent(new RenameModelRequest(NEW_NAME));
        Assert.assertEquals(NEW_NAME, data.getModel().getTitle());
        Assert.assertEquals("Модель: " + NEW_NAME, view.getWindowTitle());
    }

    //TODO - fix test, it fails after moving to arcadia and didn't start in github because of bad regexp:
    //https://a.yandex-team.ru/arc_vcs/market/mbo/mbo-catalog/build.gradle?rev=r9351105#L178,
    @Test
    @Ignore
    public void testAliases() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
            .startParameters()
            .startParameter()
            .xsl(XslNames.NAME).type(Param.Type.STRING).name("Имя")
            .endParameter()
            .startParameter()
            .xsl(XslNames.ALIASES).type(Param.Type.STRING).name("Алиасы")
            .endParameter()
            .startParameter()
            .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
            .hidden(true)
            .option(1, "Vendor1")
            .endParameter()
            .endParameters()
            .startModel()
            .id(MODEL_ID).category(CATEGORY_ID).source(Source.GURU).currentType(Source.GURU)
            .param(XslNames.VENDOR).setOption(1)
            .param(XslNames.NAME).setString(OLD_NAME)
            .param(XslNames.ALIASES)
            .setWords(
                new Word(Word.DEFAULT_LANG_ID, ALIAS_1),
                new Word(Word.DEFAULT_LANG_ID, ALIAS_2))
            .endModel()
            .startVendor()
            .source("http://source1", "ru", new Date())
            .endVendor()
            .tovarCategory(1, CATEGORY_ID.intValue());

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(new PlaceShowEvent(
            EditorUrlStub.of("modelEditor", "entity-id=1")));
        rpc.setSaveModel(MODEL_ID, null);

        Assert.assertEquals(OLD_NAME, data.getModel().getTitle());
        Assert.assertEquals("Модель: " + OLD_NAME, view.getWindowTitle());
        //  model's name won't change because aliases changed
        data.getModel().getParameterValues(XslNames.ALIASES).clearValues();
        bus.fireEvent(new RenameModelRequest(NEW_NAME));
        Assert.assertEquals(OLD_NAME, data.getModel().getTitle());
        Assert.assertEquals("Модель: " + OLD_NAME, view.getWindowTitle());
    }
}
