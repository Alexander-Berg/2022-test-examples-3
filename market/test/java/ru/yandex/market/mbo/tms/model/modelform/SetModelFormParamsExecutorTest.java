package ru.yandex.market.mbo.tms.model.modelform;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelFormBuilder;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelFormTab;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

@SuppressWarnings("checkstyle:MagicNumber")
public class SetModelFormParamsExecutorTest {

    private ModelFormDataProviderStub mdmDataProvider;
    private SetModelFormParamsExecutor executor;

    @Before
    public void setup() {
        mdmDataProvider = new ModelFormDataProviderStub();
        executor = new SetModelFormParamsExecutor(mdmDataProvider);
    }

    @Test
    public void testCategoriesWithoutFormIgnored() {
        // Categories with mdm parameters:
        mdmDataProvider.setGuruLeafCategories(1, 2, 3);
        mdmDataProvider.setMdmParameterXslNames(1, "ololo_capacity");
        mdmDataProvider.setMdmParameterXslNames(2, "meow_size");
        mdmDataProvider.setMdmParameterXslNames(3, "batman_velocity");
        // ...but no actual model forms.
        executor.doRealJob(null);

        // No forms were edited/added.
        assertFalse(mdmDataProvider.getModelEditorForm(1).isPresent());
        assertFalse(mdmDataProvider.getModelEditorForm(2).isPresent());
        assertFalse(mdmDataProvider.getModelEditorForm(3).isPresent());
    }

    @Test
    public void testCategoriesWithoutParamsIgnored() {
        ModelForm form1 = new ModelFormBuilder()
            .startTab().name("Элементарные частицы")
                .startBlock().name("Кварки")
                .properties("is_up", "is_charm", "is_beauty")
                .endBlock()
            .endTab()
            .getModelForm();

        ModelForm form2 = new ModelFormBuilder()
            .startTab().name("Жевотнъе")
                .startBlock().name("Жевотнъе")
                .properties("peos", "mooha", "kooritsa")
                .endBlock()
            .endTab()
            .getModelForm();

        // Categories with model forms:
        mdmDataProvider.setGuruLeafCategories(1, 2);
        mdmDataProvider.setModelEditorForm(1, form1.copy());
        mdmDataProvider.setModelEditorForm(2, form2.copy());
        // ...but no mdm params.
        executor.doRealJob(null);

        // No forms were edited.
        assertEquals(form1, mdmDataProvider.getModelEditorForm(1).get());
        assertEquals(form2, mdmDataProvider.getModelEditorForm(2).get());
    }

    @Test
    public void testFormsWithoutMdmTabFilledIn() {
        ModelForm form1 = new ModelFormBuilder()
            .startTab().name("Элементарные частицы")
                .startBlock().name("Кварки")
                .properties("is_up", "is_charm", "is_beauty")
                .endBlock()
            .endTab()
            .startTab().name("Ололоментарные частицы")
                .startBlock().name("Коворкинги")
                .properties("is_smoothie", "apple_used", "hipster_factor")
                .endBlock()
            .endTab()
            .getModelForm();

        ModelForm form2 = new ModelFormBuilder()
            .startTab().name("Жевотнъе")
                .startBlock().name("Жевотнъе")
                .properties("peos", "mooha", "kooritsa")
                .endBlock()
            .endTab()
            .getModelForm();

        // Categories with model forms:
        mdmDataProvider.setGuruLeafCategories(1, 2);
        mdmDataProvider.setModelEditorForm(1, form1.copy());
        mdmDataProvider.setModelEditorForm(2, form2.copy());
        // ...and with mdm parameters:
        mdmDataProvider.setMdmParameterXslNames(1, "ololo_capacity");
        mdmDataProvider.setMdmParameterXslNames(2, "meow_size");

        executor.doRealJob(null); // Forms should change;
        assertNotEquals(form1, mdmDataProvider.getModelEditorForm(1).get());
        assertNotEquals(form2, mdmDataProvider.getModelEditorForm(2).get());

        // let's make sure they changed appropriately.
        ModelFormTab mdmTab1 = new ModelFormTab(XslNames.MDM_TAB_TITLE);
        mdmTab1.addBlock(XslNames.MDM_TAB_TITLE, Collections.singletonList("ololo_capacity"));
        form1.addTab(mdmTab1);

        ModelFormTab mdmTab2 = new ModelFormTab(XslNames.MDM_TAB_TITLE);
        mdmTab2.addBlock(XslNames.MDM_TAB_TITLE, Collections.singletonList("meow_size"));
        form2.addTab(mdmTab2);

        assertEquals(form1, mdmDataProvider.getModelEditorForm(1).get());
        assertEquals(form2, mdmDataProvider.getModelEditorForm(2).get());
    }

    @Test
    public void testFormsWithMdmTabUpdated() {
        ModelForm form1 = new ModelFormBuilder()
            .startTab().name("Элементарные частицы")
                .startBlock().name("Кварки")
                .properties("is_up", "is_charm", "is_beauty")
                .endBlock()
            .endTab()
            .startTab().name(XslNames.MDM_TAB_TITLE)
                .startBlock().name(XslNames.MDM_TAB_TITLE)
                .properties("ololo_capacity", "batman")
                .endBlock()
            .endTab()
            .getModelForm();

        ModelForm form2 = new ModelFormBuilder()
            .startTab().name(XslNames.MDM_TAB_TITLE)
                .startBlock().name(XslNames.MDM_TAB_TITLE)
                .properties("oof ouch owie", "bone hurting")
                .endBlock()
            .endTab()
            .startTab().name("Жевотнъе")
                .startBlock().name("Жевотнъе")
                .properties("peos", "mooha", "kooritsa")
                .endBlock()
            .endTab()
            .getModelForm();

        // Categories with model forms:
        mdmDataProvider.setGuruLeafCategories(1, 2);
        mdmDataProvider.setModelEditorForm(1, form1.copy());
        mdmDataProvider.setModelEditorForm(2, form2.copy());
        // ...and with mdm parameters:
        mdmDataProvider.setMdmParameterXslNames(1, "ololo_capacity");
        mdmDataProvider.setMdmParameterXslNames(2, "meow_size");

        executor.doRealJob(null); // Forms should change;
        assertNotEquals(form1, mdmDataProvider.getModelEditorForm(1).get());
        assertNotEquals(form2, mdmDataProvider.getModelEditorForm(2).get());

        // let's make sure they changed appropriately.
        form1.getTab(XslNames.MDM_TAB_TITLE).get()
            .getBlock(XslNames.MDM_TAB_TITLE).get()
            .setProperties(Collections.singletonList("ololo_capacity"));

        form2.getTab(XslNames.MDM_TAB_TITLE).get()
            .getBlock(XslNames.MDM_TAB_TITLE).get()
            .setProperties(Collections.singletonList("meow_size"));

        assertEquals(form1, mdmDataProvider.getModelEditorForm(1).get());
        assertEquals(form2, mdmDataProvider.getModelEditorForm(2).get());
    }
}
