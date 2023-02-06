package ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.ModelEditor;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.addons.Event1FireEvent2Addon;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.addons.Event1FireEvent3SyncAddon;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.addons.SimpleEvent1Addon;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.addons.SimpleEvent2Addon;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.addons.SwitchableAddon;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.Event1;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.Event2;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.Event3;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.rpc.TestRpc;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ViewFactoryStub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class EditorEventBusTest {

    private TestRpc rpc;
    private ViewFactoryStub viewFactory;

    @Before
    public void setUp() throws Exception {
        rpc = new TestRpc();
        viewFactory = new ViewFactoryStub();
    }

    @Test
    public void testSimpleEventAction() {
        List<Integer> eventList = new ArrayList<>();
        SimpleEvent1Addon addon = new SimpleEvent1Addon(eventList);
        ModelEditor modelEditor = ModelEditor.customCreate(rpc, viewFactory, Collections.singletonList(addon));
        EditorEventBus bus = modelEditor.getEventBus();

        bus.fireEvent(new Event1());
        bus.fireEvent(new Event2());
        bus.fireEvent(new Event1());
        bus.fireEvent(new Event3());

        assertEqualEvents(eventList, 1, 1);
    }

    @Test
    public void testTransitiveEventCall() {
        List<Integer> eventList = new ArrayList<>();

        // order is important
        Event1FireEvent2Addon addon1 = new Event1FireEvent2Addon(eventList);
        SimpleEvent1Addon addon2 = new SimpleEvent1Addon(eventList);
        SimpleEvent2Addon addon3 = new SimpleEvent2Addon(eventList);
        ModelEditor modelEditor = ModelEditor.customCreate(rpc, viewFactory, Arrays.asList(addon1, addon2, addon3));
        EditorEventBus bus = modelEditor.getEventBus();

        bus.fireEvent(new Event1());

        assertEqualEvents(eventList, 1, 1, 2);
    }

    @Test
    public void testTransitiveSyncEventCall() {
        List<Integer> eventList = new ArrayList<>();

        // order is important
        Event1FireEvent3SyncAddon addon1 = new Event1FireEvent3SyncAddon(eventList);
        SimpleEvent1Addon addon2 = new SimpleEvent1Addon(eventList);
        SimpleEvent2Addon addon3 = new SimpleEvent2Addon(eventList);
        ModelEditor modelEditor = ModelEditor.customCreate(rpc, viewFactory, Arrays.asList(addon1, addon2, addon3));
        EditorEventBus bus = modelEditor.getEventBus();

        bus.fireEvent(new Event1());

        assertEqualEvents(eventList, 1, 2, 1);
    }

    @Test
    public void testSwitchOffEvents() {
        List<Integer> eventList = new ArrayList<>();

        SwitchableAddon addon = new SwitchableAddon(eventList);
        ModelEditor modelEditor = ModelEditor.customCreate(rpc, viewFactory, Collections.singletonList(addon));
        EditorEventBus bus = modelEditor.getEventBus();

        // проверяем, что по дефолту события будут работать
        bus.fireEvent(new Event3());
        assertEqualEvents(eventList, 3);
        eventList.clear();

        // отключаем чтение 3 события
        bus.fireEvent(new Event1());
        bus.fireEvent(new Event3());
        assertEqualEvents(eventList, 1);
        eventList.clear();

        // включаем чтение 3 события
        bus.fireEvent(new Event2());
        bus.fireEvent(new Event3());
        assertEqualEvents(eventList, 2, 3);
        eventList.clear();

        // если еще раз включить, то ничего не поменяется
        bus.fireEvent(new Event2());
        bus.fireEvent(new Event3());
        assertEqualEvents(eventList, 2, 3);
        eventList.clear();

        // если выключить и включить, то ничего не поменяется
        bus.fireEvent(new Event1());
        bus.fireEvent(new Event2());
        bus.fireEvent(new Event3());
        assertEqualEvents(eventList, 1, 2, 3);
        eventList.clear();
    }

    private static void assertEqualEvents(List<Integer> actual, Integer... expected) {
        List<Integer> expectedList = Arrays.asList(expected);
        Assert.assertEquals(expectedList, actual);
    }
}
