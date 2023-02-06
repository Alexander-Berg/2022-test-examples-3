package ru.yandex.market.mbo.tms.model.modelform;

import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ModelFormDataProviderStub implements ModelFormDataProvider {

    private List<TovarCategory> categories = new ArrayList<>();
    private Map<Long, FormPiece> mdmCategoryParameters = new HashMap<>();
    private Map<Long, ModelForm> modelForms = new HashMap<>();

    public void setGuruLeafCategories(long... categoryIds) {
        Arrays.stream(categoryIds).forEach(categoryId ->
            categories.add(new TovarCategory("#" + categoryId, categoryId, 0)));
    }

    @Override
    public List<TovarCategory> getGuruLeafCategories() {
        return categories;
    }

    public void setMdmParameterXslNames(long categoryId, String... parameters) {
        mdmCategoryParameters.put(categoryId,
            new FormPiece(XslNames.MDM_TAB_TITLE, XslNames.MDM_TAB_TITLE, new ArrayList<>(Arrays.asList(parameters))));
    }

    @Override
    public List<FormPiece> getFormPieces(long categoryId) {
        if (mdmCategoryParameters.containsKey(categoryId)) {
            return Collections.singletonList(mdmCategoryParameters.get(categoryId));
        }
        return Collections.emptyList();
    }

    public void setModelEditorForm(long categoryId, ModelForm form) {
        modelForms.put(categoryId, form);
    }

    @Override
    public Optional<ModelForm> getModelEditorForm(long categoryId) {
        return Optional.ofNullable(modelForms.get(categoryId));
    }

    @Override
    public Optional<ModelForm> getContentLabForm(long categoryId) {
        return Optional.empty();
    }

    @Override
    public void saveModelEditorForm(long categoryId, ModelForm modelForm) {
        setModelEditorForm(categoryId, modelForm);
    }

    @Override
    public void saveContentLabForm(long categoryId, ModelForm modelForm) {

    }
}
