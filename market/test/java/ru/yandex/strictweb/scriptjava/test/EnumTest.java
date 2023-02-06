package ru.yandex.strictweb.scriptjava.test;

public enum EnumTest {
    A("ScriptJava compiler"),
    B("Supports:"), Enums(" Enums"), Anon(" Anonymous classes"),
    JS(" JavaScript Native code"), FOR(" for(type var : list) ..."), STD(" Standart java classes: List, Map, Set");

    private String title;

    EnumTest(String t) {
        this.title = t;
    }

    public String getTitle() {
        return title;
    }
}
