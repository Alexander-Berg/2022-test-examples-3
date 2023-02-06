package ru.yandex.strictweb.example.sampleajax;

import ru.yandex.strictweb.scriptjava.base.CommonElements;
import ru.yandex.strictweb.scriptjava.base.DOMEventCallback;
import ru.yandex.strictweb.scriptjava.base.InputValidator;
import ru.yandex.strictweb.scriptjava.base.Node;
import ru.yandex.strictweb.scriptjava.base.NodeBuilder;
import ru.yandex.strictweb.scriptjava.base.ScriptJava;
import ru.yandex.strictweb.scriptjava.base.ValidatorHelperBase;
import ru.yandex.strictweb.scriptjava.base.VoidDelegate;
import ru.yandex.strictweb.scriptjava.base.ajax.Ajax;

import java.util.Date;
import java.util.Map;

public class SampleUiForm extends CommonElements {
    static {
        // точка входа
        initAjax();

        // берем <body>
        NodeBuilder.wrap(ScriptJava.document.body)

                // убираем из <body> все надписи
                .removeChilds()

                // добавляем туда формочку
                .add(new SampleUiForm().drawForm());
    }

    SampleHelperBean helper = new SampleHelperBean();

    private static void initAjax() {
        Ajax.DEFAULT_LOADING_IMG = "/js/yui/assets/skins/sam/wait.gif";
        Ajax.defaultErrorHandler = new VoidDelegate<>() {
            public void voidDelegate(Throwable exc) {
                if (exc == null) {
                    return;
                }
                window.alert("Error: " + exc.getMessage());
            }
        };
    }

    private NodeBuilder drawForm() {
        NodeBuilder div = $DIV();

        div.add(drawServerTimeButton());

        SomeModel o = helper.getObject(123L);
        div.add(drawObjectForm(o));

        drawServerProperties(div);

        return div;
    }

    private NodeBuilder drawObjectForm(SomeModel o) {
        final NodeBuilder form = $DIV().field("Object")
                .text("Object ID: " + o.id).add($HIDDEN(ajaxName(o.id), o.id)).BR()
                .text("Object name: ").add($INPUT().name(ajaxName(o.name)).value(o.name)).BR()
                .text("Object email: ").add($INPUT().name(ajaxName(o.email)).value(o.email).validator(new InputValidator() {
                    public boolean isValid(Node n) {
                        return n.value.toString().indexOf("@") > 0;
                    }

                    public String getMessage() {
                        return "Realy bad email =)";
                    }
                }));

        form.add($BTN("Save", new DOMEventCallback() {
            public boolean delegate(Node n) {
                if (!new ValidatorHelperBase().validate(form)) {
                    return false;
                }
                window.alert(helper.saveObject((SomeModel) (Object) form.node));
                return false;
            }
        }));

        return form;
    }

    private void drawServerProperties(NodeBuilder div) {
        // синхронный вызов
        Map<Object, Object> prop = helper.getSystemProperties();

        for (Object key : prop.keySet()) {
            div.textB(key + "").text(" :: " + prop.get(key)).BR();
        }
    }

    private NodeBuilder drawServerTimeButton() {
        return $BTN("Show server time&date", new DOMEventCallback() {
            public boolean delegate(Node n) {
                ajaxAsyncCall(helper.getServerDate(), new VoidDelegate<>() {
                    public void voidDelegate(Date date) {
                        window.alert(dateToStringSmart(date));
                    }
                });
                return false;
            }
        });
    }
}
