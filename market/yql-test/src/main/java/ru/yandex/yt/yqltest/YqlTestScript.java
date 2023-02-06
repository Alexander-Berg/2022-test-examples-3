package ru.yandex.yt.yqltest;

import ru.yandex.yt.yqltestable.YqlTestable;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.08.2021
 */
public class YqlTestScript {
    // script to run
    private final String yql;

    // property to read data from
    private String requestProperty = "yql";


    public YqlTestScript(String yql) {
        this.yql = yql;
    }

    public static YqlTestScript simple(String yql) {
        return new YqlTestScript(yql);
    }

    /**
     * Read yql from file. Prepare request as $yql (could be changed later).
     *
     * @param scriptPath Path to read script.
     * @return Prepared script test.
     */
    public static YqlTestScript fromFile(String scriptPath) {
        return new YqlTestScript(YqlTestable.readFile(scriptPath));
    }

    public String getYql() {
        return yql;
    }

    public String getRequestProperty() {
        return requestProperty;
    }

    public YqlTestScript requestProperty(String requestProperty) {
        this.requestProperty = requestProperty;
        return this;
    }

}
