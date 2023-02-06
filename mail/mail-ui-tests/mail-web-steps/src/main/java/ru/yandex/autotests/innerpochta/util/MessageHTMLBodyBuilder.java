package ru.yandex.autotests.innerpochta.util;

import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WIDE_IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * Билдер HTML тела письма.
 * Пример:
 * <pre>
 * {@code
 * messageHTMLBodyBuilder(user)
 *     .addTextLine("text")
 *     .addInlineAttach("attach.pdf")
 *     .addSignature("signature text")
 *     .build()
 * }
 * </pre>
 * <p>
 * В билдере есть пресеты тела письма, использовать можно так:
 * <pre>
 * {@code
 * messageHTMLBodyBuilder(user)
 *     .makeBodyWithInlineAttachAndText()
 * }
 * </pre>
 */
public class MessageHTMLBodyBuilder {

    private String htmlBody;
    private AllureStepStorage user;

    private MessageHTMLBodyBuilder(AllureStepStorage user) {
        this.user = user;
        htmlBody = "";
    }

    public static MessageHTMLBodyBuilder messageHTMLBodyBuilder(AllureStepStorage user) {
        return new MessageHTMLBodyBuilder(user);
    }

    private MessageHTMLBodyBuilder addLine(String text) {
        String LINE_TAG = "<div>%s</div>";
        htmlBody += String.format(LINE_TAG, text);
        return this;
    }

    public MessageHTMLBodyBuilder addTextLine(String text) {
        this.addLine(text);
        return this;
    }

    /**
     * Используемые аттачи нужно предварительно положить в /resources/attach/
     *
     * @param attachName имя файла картинки из папки attach
     * @return
     */
    public MessageHTMLBodyBuilder addInlineAttach(String attachName) {
        String sid = this.user.apiMessagesSteps().uploadAttachment(attachName);
        String INLINE_ATTACH_TAG = "<img src=\"https://webattach.mail.yandex.net/message_part_real/?sid=%s&amp;" +
            "no_disposition=y&amp;yandex_class=yandex_new_inline_%s\" />";
        this.addLine(String.format(INLINE_ATTACH_TAG, sid, sid));
        return this;
    }

    public MessageHTMLBodyBuilder addSignature(String signature) {
        this.addLine("")
            .addLine("&nbsp;")
            .addLine("--&nbsp;")
            .addLine(signature)
            .addLine("&nbsp;");
        return this;
    }

    public MessageHTMLBodyBuilder addBoldText(String text){
        this.addLine("<b>" + text + "</b>");
        return this;
    }

    public String build() {
        return htmlBody;
    }


    /**
     * Пресеты для тела письма
     */

    public String makeBodyWithInlineAttachAndText() {
        return this.addTextLine(getRandomName())
            .addInlineAttach(IMAGE_ATTACHMENT)
            .build();
    }

    public String makeBodyWithWideInlineAttachAndText() {
        return this.addTextLine(getRandomName())
            .addInlineAttach(WIDE_IMAGE_ATTACHMENT)
            .build();
    }

    public String makeBodyWithInlineAttachAndText(String text) {
        return this.addTextLine(text)
            .addInlineAttach(IMAGE_ATTACHMENT)
            .build();
    }
}
