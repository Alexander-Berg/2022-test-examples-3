package ru.yandex.strictweb.scriptjava.test;

import ru.yandex.strictweb.scriptjava.base.CommonElements;
import ru.yandex.strictweb.scriptjava.base.DOMEventCallback;
import ru.yandex.strictweb.scriptjava.base.Node;
import ru.yandex.strictweb.scriptjava.base.NodeBuilder;
import ru.yandex.strictweb.scriptjava.base.custom.SelectNodeBuilder;
import ru.yandex.strictweb.scriptjava.base.custom.TableNodeBuilder;

import java.util.*;

public class ButtonForm<E extends ButtonForm> extends CommonElements {
    static {
        document.body.appendChild(new ButtonForm("Button and select test").drawForm().node);
    }

    String name;
    EnumTest selected;
    E f2;

    public ButtonForm(String name) {
        this.name = name;
    }

    void test(E f) {
        f.drawForm();
        f2.createNode("QWE");
    }

    NodeBuilder drawForm() {
        final NodeBuilder log = $DIV();
        final SelectNodeBuilder select = $SELECT();

        TableNodeBuilder b = $TABLE();

        select.onChange(new DOMEventCallback() {
            public boolean delegate(Node n) {
                selected = EnumTest.valueOf(select.valueAsStr());
                Date d = new Date();
                log.add($B(d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds()))
                        .text(" " + selected.getTitle()).BR();
                return true;
            }
        });

        for (EnumTest e : EnumTest.values()) {
            select.add($OPTION(e, e.getTitle()));
        }

        return $DIV()
                .add(EL("h2").text(name))
                .add($BTN("Press me!", new DOMEventCallback() {
                    public boolean delegate(Node n) {
                        window.alert(null != selected ? selected + ": " + selected.getTitle() : "Nothing is selected");
                        return false;
                    }
                }))
                .text(" ")
                .add(select)
                .add($HR())
                .add(log)
                ;
    }
}
