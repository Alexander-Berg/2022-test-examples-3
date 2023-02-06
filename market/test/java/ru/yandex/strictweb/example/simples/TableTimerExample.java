package ru.yandex.strictweb.example.simples;

import ru.yandex.strictweb.scriptjava.base.CommonElements;
import ru.yandex.strictweb.scriptjava.base.NodeBuilder;
import ru.yandex.strictweb.scriptjava.base.VoidDelegate;
import ru.yandex.strictweb.scriptjava.base.custom.TableInnerNodeBuilder;
import ru.yandex.strictweb.scriptjava.base.util.IntervalIdentifier;

import java.util.Date;

public class TableTimerExample extends CommonElements {
    static {
        document.body.appendChild(EL("h2").text(ajaxName(TableTimerExample.class) + " example").node);
        document.body.appendChild(new TableTimerExample().draw().node);
    }

    int n = 0;

    private NodeBuilder draw() {
        final TableInnerNodeBuilder tbody = $TBODY();

        setInterval(new VoidDelegate<>() {
            public void voidDelegate(IntervalIdentifier intervalId) {
                n++;
                tbody.add($TR()
                        .add($TD().text("" + n))
                        .add($TD().text(dateToStringSmart(new Date())))
                );

                if (n >= 100) {
                    clearInterval(intervalId);
                }
            }
        }, 1000);

        return $DIV().style("maxHeight", "200px").styleOverflow("auto")
                .add(
                        $TABLE().border(1)
                                .add(
                                        $THEAD().add(
                                                $TR().TDH("#").TDH("Date & Time")
                                        )
                                ).add(tbody)
                )
                ;
    }
}
