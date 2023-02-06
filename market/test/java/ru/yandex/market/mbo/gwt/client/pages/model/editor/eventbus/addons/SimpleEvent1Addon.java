package ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.addons;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.ModelEditor;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.Event1;

import java.util.List;

/**
 * @author s-ermakov
 */
public class SimpleEvent1Addon extends BaseTestAddon {
    public SimpleEvent1Addon(List<Integer> listToWriteCallsTo) {
        super(listToWriteCallsTo);
    }

    @Override
    public void init(ModelEditor modelEditor) {
        EditorEventBus bus = modelEditor.getEventBus();
        bus.subscribe(Event1.class, event -> {
            writeEventCall(event);
        });
    }
}
