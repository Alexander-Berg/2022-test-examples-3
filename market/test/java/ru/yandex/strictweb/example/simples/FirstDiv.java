package ru.yandex.strictweb.example.simples;

import ru.yandex.strictweb.scriptjava.base.CommonElements;
import ru.yandex.strictweb.scriptjava.base.NodeBuilder;

public class FirstDiv extends CommonElements {
    static {
        NodeBuilder div = $DIV()
                .text("This method wil add a text-node to div")
                .add(
                        $P().text("Adding <p> with text in it")
                                .styleColor("red")
                                .styleBorder("1px solid black")
                )
                .add(
                        $SPAN().innerHTML("Adding <b>html</b> using innerHTML, but this is not good way")
                );

        document.body.appendChild(EL("h2").text(ajaxName(FirstDiv.class) + " example").node);
        document.body.appendChild(div.node);
    }
}
