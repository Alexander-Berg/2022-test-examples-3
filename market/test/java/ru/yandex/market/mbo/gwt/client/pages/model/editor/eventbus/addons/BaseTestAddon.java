package ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.addons;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.ModelEditor;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.BaseTestEvent;

import java.util.List;

/**
 * @author s-ermakov
 */
public abstract class BaseTestAddon implements ModelEditor.ModelEditorAddon {
    protected final List<Integer> listToWriteCallsTo;

    protected BaseTestAddon(List<Integer> listToWriteCallsTo) {
        this.listToWriteCallsTo = listToWriteCallsTo;
    }

    public List<Integer> getListToWriteCallsTo() {
        return listToWriteCallsTo;
    }

    protected void writeEventCall(BaseTestEvent testEvent) {
        listToWriteCallsTo.add(testEvent.getNumber());
    }
}
