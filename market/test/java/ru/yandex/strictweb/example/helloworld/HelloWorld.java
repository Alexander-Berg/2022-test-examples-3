package ru.yandex.strictweb.example.helloworld;

import ru.yandex.strictweb.scriptjava.base.CommonElements;
import ru.yandex.strictweb.scriptjava.base.DOMEventCallback;
import ru.yandex.strictweb.scriptjava.base.Node;
import ru.yandex.strictweb.scriptjava.base.NodeBuilder;
import ru.yandex.strictweb.scriptjava.base.ScriptJava;

public class HelloWorld extends CommonElements {

    static {
        // точка входа
        NodeBuilder.wrap(ScriptJava.document.body)
                .removeChilds()
                .add(new HelloWorld().drawForm());
    }

    private NodeBuilder drawForm() {
        // создаем DIV и в нем обычную кнопку
        return $DIV().add($BTN("Нажми меня!", new DOMEventCallback() {
            public boolean delegate(Node n) {
                window.alert("Hello, World!");
                return false;
            }
        }));
    }
}
