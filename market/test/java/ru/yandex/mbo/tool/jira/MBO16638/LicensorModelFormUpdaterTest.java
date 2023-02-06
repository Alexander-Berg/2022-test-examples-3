package ru.yandex.mbo.tool.jira.MBO16638;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelFormBuilder;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;
import ru.yandex.market.mbo.gwt.utils.XslNames;

/**
 * @author ayratgdl
 * @since 04.10.18
 */
public class LicensorModelFormUpdaterTest {
    @Test
    public void addLicensor() {
        ModelForm sourceForm = new ModelFormBuilder()
            .startTab("tab1")
            .endTab()
            .getModelForm();

        ModelForm updatedForm = new LicensorModelFormUpdater().update(sourceForm, true, false, false);
        Assert.assertNotEquals(updatedForm, sourceForm);

        ModelForm expectedTab = new ModelFormBuilder()
            .startTab("tab1")
                .startBlock("Лицензиар, тема, персонаж")
                    .property(XslNames.LICENSOR)
                .endBlock()
            .endTab()
            .getModelForm();
        Assert.assertEquals(expectedTab, updatedForm);
    }

    @Test
    public void addLicensorWhenFormContainsLicensorInLicensorBlock() {
        ModelForm sourceForm = new ModelFormBuilder()
            .startTab("tab1")
                .startBlock("Блок с лицензиаром")
                    .property(XslNames.LICENSOR)
                .endBlock()
            .endTab()
            .getModelForm();

        ModelForm updatedForm = new LicensorModelFormUpdater().update(sourceForm, true, false, false);
        Assert.assertEquals(updatedForm, sourceForm);
    }

    @Test
    public void addLicensorAndPersonageWhenFormContainsPersonageInLicensorBlock() {
        ModelForm sourceForm = new ModelFormBuilder()
            .startTab("tab1")
                .startBlock("Блок с лицензиаром")
                    .property(XslNames.PERSONAGE)
                .endBlock()
            .endTab()
            .getModelForm();

        ModelForm updatedForm = new LicensorModelFormUpdater().update(sourceForm, true, false, true);
        Assert.assertNotEquals(updatedForm, sourceForm);

        ModelForm expected = new ModelFormBuilder()
            .startTab("tab1")
                .startBlock("Блок с лицензиаром")
                    .property(XslNames.LICENSOR)
                    .property(XslNames.PERSONAGE)
                .endBlock()
            .endTab()
            .getModelForm();
        Assert.assertEquals(expected, updatedForm);
    }

    @Test
    public void addLicensorWhenFormContainsLicensorInOutsideLicensorBlock() {
        ModelForm sourceTab = new ModelFormBuilder()
            .startTab("tab1")
                .startBlock("Основные параметры")
                    .property("name")
                    .property(XslNames.LICENSOR)
                .endBlock()
            .endTab()
            .getModelForm();

        ModelForm updatedForm = new LicensorModelFormUpdater().update(sourceTab, true, false, false);
        Assert.assertNotEquals(updatedForm, sourceTab);

        ModelForm expectedTab = new ModelFormBuilder()
            .startTab("tab1")
                .startBlock("Основные параметры")
                    .property("name")
                .endBlock()
                .startBlock("Лицензиар, тема, персонаж")
                    .property(XslNames.LICENSOR)
                .endBlock()
            .endTab()
            .getModelForm();
        Assert.assertEquals(expectedTab, updatedForm);
    }

    @Test
    public void removeLicensorAndAddFranchiseWhenFormContainsLicensorInOutsideLicensorBlock() {
        ModelForm sourceTab = new ModelFormBuilder()
            .startTab("tab1")
                .startBlock("Основные параметры")
                    .property("name")
                    .property(XslNames.LICENSOR)
                .endBlock()
            .endTab()
            .getModelForm();

        ModelForm updatedForm = new LicensorModelFormUpdater().update(sourceTab, false, true, false);
        Assert.assertNotEquals(updatedForm, sourceTab);

        ModelForm expectedTab = new ModelFormBuilder()
            .startTab("tab1")
                .startBlock("Основные параметры")
                    .property("name")
                .endBlock()
                .startBlock("Лицензиар, тема, персонаж")
                    .property(XslNames.FRANCHISE)
                .endBlock()
            .endTab()
            .getModelForm();
        Assert.assertEquals(expectedTab, updatedForm);
    }
}
