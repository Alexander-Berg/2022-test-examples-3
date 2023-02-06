package ru.yandex.autotests.innerpochta.util;

import com.google.common.collect.Sets;
import org.openqa.selenium.By;

import java.util.Set;

/**
 * @author a-zoshchuk
 */
public class CalTestsIgnoredElements {

    public static final Set<By> IGNORED_ELEMENTS = Sets.newHashSet(
        By.cssSelector(".qa-TouchAsideVersion")
    );
}
