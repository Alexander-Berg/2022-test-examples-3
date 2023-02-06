package ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.addons;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.ModelEditor;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.Event2;

import java.util.List;

/**
 * @author s-ermakov
 */
public class SimpleEvent2Addon extends BaseTestAddon {
    public SimpleEvent2Addon(List<Integer> listToWriteCallsTo) {
        super(listToWriteCallsTo);
    }

    @Override
    public void init(ModelEditor modelEditor) {
        EditorEventBus bus = modelEditor.getEventBus();
        bus.subscribe(Event2.class, event -> {
            writeEventCall(event);
        });
    }
}
