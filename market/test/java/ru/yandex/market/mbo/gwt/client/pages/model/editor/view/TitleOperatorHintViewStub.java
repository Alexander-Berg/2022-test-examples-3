package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.TitleOperatorHintView;

/**
 * @author V.Zaytsev
 * @since 17.08.2017
 */
public class TitleOperatorHintViewStub extends EditorWidgetStub implements TitleOperatorHintView {
    private String includedHint;
    private String excludedHint;
    private String modelNameComment;
    private String errorText;
    private boolean includedHintVisible;
    private boolean excludedHintVisible;
    private boolean modelNameCommentHintVisible;

    @Override
    public void setOperatorHints(String included, String excluded, String modelNameComment) {
        this.includedHint = included;
        this.excludedHint = excluded;
        this.modelNameComment = modelNameComment;
    }

    @Override
    public void setErrorText(String text) {
        this.errorText = text;
    }

    @Override
    public boolean isIncludedHintVisible() {
        return includedHintVisible;
    }

    @Override
    public void setIncludedHintVisible(boolean visible) {
        includedHintVisible = visible;
    }

    @Override
    public boolean isExcludedHintVisible() {
        return excludedHintVisible;
    }

    @Override
    public void setExcludedHintVisible(boolean visible) {
        excludedHintVisible = visible;
    }

    @Override
    public boolean isModelNameCommentHintVisible() {
        return modelNameCommentHintVisible;
    }

    @Override
    public void setModelNameCommentHintVisible(boolean visible) {
        modelNameCommentHintVisible = visible;
    }

    public String getErrorText() {
        return errorText;
    }

    public String getIncludedHint() {
        return includedHint;
    }

    public String getExcludedHint() {
        return excludedHint;
    }

    public String getModelNameComment() {
        return modelNameComment;
    }
}
