package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.RuleInfoWidget;

/**
 * @author gilmulla
 *
 */
public class RuleInfoDlgStub extends EditorWidgetStub implements RuleInfoWidget {

    private String message;
    private List<Block> blocks = new ArrayList<>();
    private boolean errorStyleEnabled;

    @Override
    public List<Block> getBlocks() {
        return this.blocks;
    }

    @Override
    public void addBlock(Block block) {
        this.blocks.add(block);
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean isErrorStyleEnabled() {
        return this.errorStyleEnabled;
    }

    @Override
    public void setErrorStyleEnabled(boolean enabled) {
        this.errorStyleEnabled = enabled;
    }

}
