package ru.yandex.autotests.innerpochta.steps;

import io.qameta.allure.Attachment;
import org.hamcrest.MatcherAssert;
import ru.yandex.autotests.innerpochta.screen.differs.ImageDiffer;
import ru.yandex.autotests.innerpochta.util.AllureLogger;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.comparison.DiffMarkupPolicy;
import ru.yandex.qatools.ashot.comparison.ImageDiff;
import ru.yandex.qatools.ashot.comparison.PointsMarkupPolicy;
import ru.yandex.qatools.ashot.coordinates.Coords;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author cosmopanda
 */
@SuppressWarnings("UnusedReturnValue")
public class SaveScreenSteps {

    private static ImageDiff diff(Screenshot screenshot1, Screenshot screenshot2) {
        final int DIFF_PIXEL_NUMBER_TO_IGNORE = 2;
        DiffMarkupPolicy mailDiffPolicy = new PointsMarkupPolicy();
        mailDiffPolicy.setDiffSizeTrigger(DIFF_PIXEL_NUMBER_TO_IGNORE);
        ImageDiffer differ = new ImageDiffer().withDiffMarkupPolicy(mailDiffPolicy.withDiffColor(Color.MAGENTA));
        return differ.makeDiff(screenshot1, screenshot2);
    }

    @Step("Сравниваем скриншоты")
    public SaveScreenSteps saveDiffScreenshot(Screenshot page1, Screenshot page2, Integer pixelRatio,
                                              String screenName1, String screenName2) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<SaveScreenSteps> steps = executorService.submit(() -> saveDiffScreenshotInner(
            page1,
            page2,
            pixelRatio,
            screenName1,
            screenName2
        ));
        try {
            return steps.get(60, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AssertionError) {
                throw (AssertionError) cause;
            } else {
                throw new RuntimeException(e);
            }
        } catch (InterruptedException ignored) {
        }
        return this;
    }

    private SaveScreenSteps saveDiffScreenshotInner(Screenshot page1, Screenshot page2, Integer pixelRatio,
                                                    String screenName1, String screenName2) throws IOException {
        Set<Coords> sumIgnoredAreas = new HashSet<>();
        sumIgnoredAreas.addAll(page1.getIgnoredAreas());
        sumIgnoredAreas.addAll(page2.getIgnoredAreas());
        AllureLogger.logToAllure("Areas to ignore: " + String.valueOf(sumIgnoredAreas));
        for (Coords ignoredArea : sumIgnoredAreas) {
            ignoredArea.x = ignoredArea.x * pixelRatio;
            ignoredArea.y = ignoredArea.y * pixelRatio;
            ignoredArea.width = ignoredArea.width * pixelRatio;
            ignoredArea.height = ignoredArea.height * pixelRatio;
        }
        page1.setIgnoredAreas(sumIgnoredAreas);
        page2.setIgnoredAreas(sumIgnoredAreas);
        AllureLogger.logToAllure("Areas to ignore (with pixel ratio): " + String.valueOf(sumIgnoredAreas));
        saveScreenshot(page1.getImage(), screenName1);
        saveScreenshot(page2.getImage(), screenName2);
        // NEW SCREEN DIFF
        ImageDiff imageDiff = diff(page1, page2);
        saveScreenshot(imageDiff.getMarkedImage(), "Diff. Pixels number:" + imageDiff.getDiffSize());
        saveScreenshot(
            imageDiff.getTransparentMarkedImage(), "Transparent diff. Pixels number: " + imageDiff.getDiffSize());
        MatcherAssert.assertThat("Страницы продакшена и тестинга отличаются", !imageDiff.hasDiff());
        return this;
    }

    @Attachment(value = "Screener {1}", type = "image/png", fileExtension = "png")
    private byte[] saveScreenshot(BufferedImage image, String name) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            baos.flush();
            return baos.toByteArray();
        }
    }
}
