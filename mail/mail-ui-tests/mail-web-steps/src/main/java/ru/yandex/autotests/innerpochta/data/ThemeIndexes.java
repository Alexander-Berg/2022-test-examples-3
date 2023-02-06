package ru.yandex.autotests.innerpochta.data;

/**
 * @author cosmopanda
 */
public enum ThemeIndexes {

    SIMPLE_THEME(10), //любая тема без выпадушки
    COLOR_THEME(4), //цветовая схема в выпадушке цветной темы
    SEASON_THEME(1), //тема "настроение"
    SEASON_WINTER_THEME(0), //зима в теме "настроение"
    WEATHER_THEME(2);

    private int index;

    ThemeIndexes(int index) {
        this.index = index;
    }

    public int index() { return index; }

}
