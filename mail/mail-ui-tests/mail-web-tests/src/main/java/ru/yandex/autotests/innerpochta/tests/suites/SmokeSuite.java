package ru.yandex.autotests.innerpochta.tests.suites;

import io.qameta.allure.junit4.Tag;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.runners.Categories;
import ru.yandex.autotests.innerpochta.tests.suites.categories.SmokeTests;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Сьют для смоука")
@Features(FeaturesConst.SMOKE_SUITE)
@Tag(FeaturesConst.SMOKE_SUITE)
@Stories(FeaturesConst.SMOKE_SUITE)
@RunWith(Categories.class)
@Categories.IncludeCategory(SmokeTests.class)
public class SmokeSuite {
}