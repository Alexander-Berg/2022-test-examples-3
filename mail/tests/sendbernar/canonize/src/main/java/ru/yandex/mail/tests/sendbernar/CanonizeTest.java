package ru.yandex.mail.tests.sendbernar;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.devtools.test.Canonizer;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.hound.Labels;
import ru.yandex.mail.tests.sendbernar.generated.ComposeMessage;
import ru.yandex.mail.tests.sendbernar.generated.composemessage.ApiComposeMessage;
import ru.yandex.mail.tests.sendbernar.models.DiskAttachHelper;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.common.utils.Files.downloadFileWithNonrandomFilename;
import static ru.yandex.mail.tests.sendbernar.models.MessagesWithInlines.getSmileWithHtml;


@Aqua.Test
@Title("Канонизированные тесты")
@Description("Сравниваю вывод ручки /compose_message с разными аттачами")
public class CanonizeTest extends BaseXenoClass {
    @Override
    public AccountWithScope mainUser() {
        return Accounts.xeno;
    }

    static String cleanedEml(String eml) throws Exception {
        List<String> lines = new ArrayList<>(Arrays.asList(eml.split("\r\n")));
        String dateHeader = "Date: ";
        String receivedHeader = "Received: by";
        String boundaryMask = "boundary=";

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(dateHeader)) {
                lines.remove(i);
                break;
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(receivedHeader)) {
                lines.remove(i+1);
                lines.remove(i);
                i = 0;
            }
        }

        String boundary = null;
        for (String line : lines) {
            if (line.contains(boundaryMask)) {
                boundary = line;
            }
        }

        if (boundary == null) {
            return String.join("\r\n", lines);
        } else {
            int first = boundary.indexOf('"');
            String oldBoundary = boundary.substring(first + 1, boundary.length() - 1);

            return String.join("\r\n", lines).replace(oldBoundary, "boundary");
        }
    }

    @Test
    @Title("/compose_message и всё что можно скомпозировать")
    public void compareEmlWithoutVariableHeaders() throws Exception {
        String messageId = "<1121537786231@wmi5-qa.yandex.ru>";
        String picture = "https://proxy.sandbox.yandex-team.ru/1301303962";

        String smile = getSmileWithHtml();
        String id = uploadedIdStrictFilename(picture);
        DiskAttachHelper attach = new DiskAttachHelper();
        File imageJpeg = downloadFileWithNonrandomFilename(picture, "file");

        String lid = Labels.labels(
                HoundApi
                        .apiHound(HoundProperties.properties().houndUri(),
                                props().getCurrentRequestId())
                        .labels()
                        .withUid(mainUser().get().uid())
                        .post(shouldBe(HoundResponses.ok200()))
        ).lidByTitle("important_label");

        String eml = apiSendbernar()
                .composeMessage()
                .withUid(mainUser().get().uid())
                .withCaller("mobile")
                .withTo("to@yandex.ru")
                .withCc("cc@yandex.ru")
                .withBcc("bcc@yandex.ru")
                .withSubj("русская часть темы")
                .withText(smile)
                .withHtml(ApiComposeMessage.HtmlParam.YES)
                .withMessageId(messageId)
                .withUploadedAttachStids(id)
                .withDiskAttaches(attach.getHtml())
                .withPartsJson(getPartsJson(imageJpeg))
                .withLids(lid)
                .post(shouldBe(okMessageComposed()))
                .as(ComposeMessage.class)
                .getText();

        Canonizer.canonize(cleanedEml(eml));
    }

    @Test
    @Title("/compose_message и простейшее письмо")
    public void shouldComposeSimplestMessage() throws Exception {
        String messageId = "<31579022759@huron.sas.yp-c.yandex.net>";
        String eml = composeMessage()
                .withTo("to@yandex.ru")
                .withMessageId(messageId)
                .post(shouldBe(okMessageComposed()))
                .as(ComposeMessage.class)
                .getText();

        Canonizer.canonize(cleanedEml(eml));
    }
}
