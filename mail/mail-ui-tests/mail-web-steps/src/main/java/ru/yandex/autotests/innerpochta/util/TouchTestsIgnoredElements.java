package ru.yandex.autotests.innerpochta.util;

import com.google.common.collect.Sets;
import org.openqa.selenium.By;

import java.util.Set;

/**
 * Created by puffyfloof
 */
public class TouchTestsIgnoredElements {
    public static final Set<By> IGNORED_ELEMENTS = Sets.newHashSet(
            By.cssSelector(".direct"),
            By.cssSelector(".ico_loader"),
            By.cssSelector(".leftPanel-touchVersion"),
            By.cssSelector(".YndxBug")
    );
}
