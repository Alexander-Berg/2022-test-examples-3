package ru.yandex.strictweb.scriptjava.test;

import ru.yandex.strictweb.scriptjava.base.ScriptJava;
import ru.yandex.strictweb.scriptjava.base.ajax.Ajax;

import java.util.Date;
import java.util.List;
import java.util.Vector;

public class TestObjectToXml {

    String str = "qqq";

    boolean bool = true;

    Date date = new Date();

    List<Object> array = null;

    public boolean test() {
        array = new Vector<>();
        array.add(new TestObjectToXml());
        array.add("Hello");
        array.add(new int[] {1, 2, 3});
        array.add(null);

        String res = Ajax.objectToXml(this, null);

        UnitTest.println(res);

        String d = ScriptJava.dateToStringSmart(date);

        String rs = "<o>" +
                "<s id=\"str\">qqq</s>" +
                "<b id=\"bool\">1</b>" +
                "<d id=\"date\">" + d + "</d>" +
                "<a id=\"array\">" +
                "<o>" +
                "<s id=\"str\">qqq</s>" +
                "<b id=\"bool\">1</b>" +
                "<d id=\"date\">" + d + "</d>" +
                "</o>" +
                "<s>Hello</s>" +
                "<a>" + "<n>1</n>" + "<n>2</n>" + "<n>3</n>" + "</a>" +
                "<null/>" +
                "</a>" +
                "</o>";

        return res == rs;
    }
}
