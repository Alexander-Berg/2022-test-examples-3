package ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.addons;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.ModelEditor;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.Event1;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.Event2;

import java.util.List;

/**
 * @author s-ermakov
 */
public class Event1FireEvent2Addon extends BaseTestAddon {
    public Event1FireEvent2Addon(List<Integer> listToWriteCallsTo) {
        super(listToWriteCallsTo);
    }

    @Override
    public void init(ModelEditor modelEditor) {
        EditorEventBus bus = modelEditor.getEventBus();
        bus.subscribe(Event1.class, event -> {
            writeEventCall(event);
            bus.fireEvent(new Event2());
        });
    }
}
