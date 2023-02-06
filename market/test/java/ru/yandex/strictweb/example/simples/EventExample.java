package ru.yandex.strictweb.example.simples;

import ru.yandex.strictweb.scriptjava.base.CommonElements;
import ru.yandex.strictweb.scriptjava.base.DOMEventCallback;
import ru.yandex.strictweb.scriptjava.base.Node;
import ru.yandex.strictweb.scriptjava.base.NodeBuilder;
import ru.yandex.strictweb.scriptjava.base.custom.NoChildNodeBuilder;

public class EventExample extends CommonElements implements DOMEventCallback {
    static {
        document.body.appendChild(EL("h2").text(ajaxName(EventExample.class) + " example").node);
        document.body.appendChild(new EventExample().draw().node);
    }

    private NoChildNodeBuilder input;

    private NodeBuilder draw() {
        return $DIV()
                .add(input = $INPUT().onChange(this))
                .BR()
                .add($BTN("Push me", new DOMEventCallback() {
                    public boolean delegate(Node n) {
                        window.alert("btn.onClick: " + input.valueAsStr());
                        return false;
                    }
                }))
                ;
    }

    // input onChange
    public boolean delegate(Node n) {
        window.alert("input.onChange: " + n.value);
        return true; // we need it
    }
}
