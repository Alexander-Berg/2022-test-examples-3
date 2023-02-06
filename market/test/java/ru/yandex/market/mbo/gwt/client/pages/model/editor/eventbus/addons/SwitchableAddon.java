package ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.addons;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.ModelEditor;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.Event1;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.Event2;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.Event3;

import java.util.List;

/**
 * @author s-ermakov
 */
public class SwitchableAddon extends BaseTestAddon {

    public SwitchableAddon(List<Integer> listToWriteCallsTo) {
        super(listToWriteCallsTo);
    }

    @Override
    public void init(ModelEditor modelEditor) {
        EditorEventBus bus = modelEditor.getEventBus();
        bus.subscribe(Event1.class, event1 -> {
            writeEventCall(event1);
            bus.switchAddonHandlers(this, false);
        });
        bus.subscribe(Event2.class, event2 -> {
            writeEventCall(event2);
            bus.switchAddonHandlers(this, true);
        });
        bus.subscribeSwitchableConsumer(Event3.class, event3 -> {
            writeEventCall(event3);
        });
    }
}
