package ru.yandex.market.partner.testing.model;

import java.util.List;

import ru.yandex.market.core.cutoff.model.AboScreenshotDto;
import ru.yandex.market.core.cutoff.model.CutoffMessageInfo;

/**
 * Детали отправленного сообщения для cutoff/param_value.
 *
 * @author avetokhin 31/08/16.
 */
public class MessageDetails {
    private final String subject;
    private final String body;
    private final List<AboScreenshotDto> screenshots;

    public MessageDetails(final String subject, final String body, final List<AboScreenshotDto> screenshots) {
        this.subject = subject;
        this.body = body;
        this.screenshots = screenshots;
    }

    /**
     * Создать сообщение на основании {@link CutoffMessageInfo}.
     */
    public static MessageDetails fromCutoffMessageInfo(final CutoffMessageInfo info) {
        if (info == null) {
            return null;
        }
        return new MessageDetails(info.getSubject(), info.getBody(), info.getScreenshots());
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public List<AboScreenshotDto> getScreenshots() {
        return screenshots;
    }
}
