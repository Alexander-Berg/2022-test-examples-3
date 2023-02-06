package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.innerpochta.steps.DefaultSteps;
import ru.yandex.screenshooter.ScreenshooterUtil;
import ru.yandex.screenshooter.beans.Difference;
import ru.yandex.screenshooter.beans.PageScreenshot;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static ru.yandex.qatools.elliptics.ElClient.elliptics;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 12.10.12
 * Time: 19:19
 */
public class ScreenShotsMatcher extends TypeSafeMatcher<PageScreenshot> {

    public static final String DEFAULT_STR = "http://ya.ru";

//    private AllureStepStorage user = new AllureStepStorage();

    private String name;
    private List<PageScreenshot> production;
    private static long session = System.currentTimeMillis();

    public boolean matchesSafely(PageScreenshot beta) {
        for (int i = 0; i < production.size(); i++) {

            try {
                File file = File.createTempFile(name + i, "difference.png");
                Difference dif = ScreenshooterUtil.makeDifference(beta.toString(), beta,
                        production.get(i), file, session, 30);

                String prodUrl = elliptics().path(ScreenShotsMatcher.class)
                        .name(substringAfterLast(production.get(i).getPath(), "/"))
                        .update(new File(production.get(i).getPath()))
                        .get().url();

                String diffUrl = elliptics().path(ScreenShotsMatcher.class)
                        .name(file.getName())
                        .update(file)
                        .get().url();

//
//                user.defaultSteps().openPageWithoutScreenshot(defaultIfEmpty(prodUrl, DEFAULT_STR));
//                user.defaultSteps().productionScreen(String.format("(%d/%d)", i + 1, production.size()),
//                        defaultIfEmpty(prodUrl, DEFAULT_STR));
//
//                user.defaultSteps().openPageWithoutScreenshot(defaultIfEmpty(diffUrl, DEFAULT_STR));
//                user.defaultSteps().differenceScreen(String.format("(%d/%d)", i + 1, production.size()),
//                        defaultIfEmpty(diffUrl, DEFAULT_STR));
                if (!dif.isDifferencePresent()) {
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public ScreenShotsMatcher(List<PageScreenshot> production, String name, DefaultSteps defaultUser) {
        this.name = name;
        this.production = production;
//        this. = user.defaultSteps();
    }

//    @Factory
//    public static ScreenShotsMatcher matchesAnyOfThisScreenShots(List<PageScreenshot> production, String name,
//                                                                 DefaultSteps user.defaultSteps()) {
////        return new ScreenShotsMatcher(production, name, user.defaultSteps());
//    }

    @Override
    public void describeMismatchSafely(PageScreenshot beta, Description description) {
        description.appendText("Скриншот беты не совпадает с эталонными скриншотами продакшена");
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Скриншот беты совпадает с одним из эталонных скриншотов продакшена");
    }
}
