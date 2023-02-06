package ru.yandex.autotests.innerpochta.util;

import org.openqa.selenium.Keys;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 14.12.12
 * Time: 13:24
 */
public class KeysOwn {
    public KeysOwn(Keys key) {
        this.key = key;
    }

    private Keys key;

    public Keys key() {
        return key;
    }

    public static KeysOwn key(Keys key) {
        return new KeysOwn(key);
    }

    @Override
    public String toString() {
        return key.name();
    }
}
