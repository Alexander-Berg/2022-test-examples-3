package ru.yandex.canvas.steps;

import java.util.Date;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.canvas.model.Size;
import ru.yandex.canvas.model.html5.Source;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

@ParametersAreNonnullByDefault
public class SourceSteps {
    public static Source defaultActiveSource(Long clientId) {
        return defaultActiveSource(clientId, Size.of(300, 300));
    }

    public static Source defaultActiveSource(Long clientId, Size size) {
        return new Source()
                .setId(randomAlphanumeric(32))
                .setClientId(clientId)
                .setName(randomAlphanumeric(32))
                .setArchive(false)
                .setWidth(size.getWidth())
                .setHeight(size.getHeight())
                .setUrl("http://www.yandex.ru/preview/" + randomAlphabetic(20))
                .setDate(new Date())
                .setScreenshotIsDone(true)
                .setScreenshotUrl("screenshot_url");
    }

    public static Source defaultActiveCpmYndxFrontpageSource(Long clientId) {
        return defaultActiveSource(clientId)
                .setWidth(1456)
                .setHeight(180);
    }

    public static Source defaultActiveCpmGeoproductSource(Long clientId) {
        return defaultActiveSource(clientId)
                .setWidth(640)
                .setHeight(100);
    }
}
