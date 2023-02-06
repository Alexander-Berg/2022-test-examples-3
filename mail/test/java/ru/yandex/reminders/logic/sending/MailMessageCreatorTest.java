package ru.yandex.reminders.logic.sending;

import java.util.regex.Pattern;

import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonValue;
import ru.yandex.commune.mail.BodyPart;
import ru.yandex.commune.mail.ContentType;
import ru.yandex.commune.mail.DefaultContent;
import ru.yandex.commune.mail.MailMessage;
import ru.yandex.misc.regex.Matcher2;
import ru.yandex.misc.regex.Pattern2;
import ru.yandex.misc.test.Assert;
import ru.yandex.reminders.logic.sending.emails.EventEmailInfo;

public class MailMessageCreatorTest {
    private final String JSON = "{\n" +
            "      \"@context\":              \"http://schema.org\",\n" +
            "      \"@type\":                 \"EventReservation\",\n" +
            "      \"reservationNumber\":     \"YAC556677\",\n" +
            "      \"underName\": {\n" +
            "        \"@type\":               \"Person\",\n" +
            "        \"name\":                \"Vasya Pupkin\"\n" +
            "      },\n" +
            "      \"reservationFor\": {\n" +
            "        \"@type\":               \"Event\",\n" +
            "        \"name\":                \"YAC 2014\",\n" +
            "        \"startDate\":           \"2014-05-17T10:00:00+04:00\",\n" +
            "        \"location\": {\n" +
            "          \"@type\":             \"Place\",\n" +
            "          \"name\":              \"Yandex\",\n" +
            "          \"address\": {\n" +
            "            \"@type\":           \"PostalAddress\",\n" +
            "            \"streetAddress\":   \"Tolstoy St.\",\n" +
            "            \"addressLocality\": \"Moscow\",\n" +
            "            \"addressCountry\":  \"RU\"\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n";

    @Test
    public void createEventEmailWithJson() {
        JsonObject originalJson = JsonObject.parseObject(JSON);
        EventEmailInfo eventEmailInfo = new EventEmailInfo("event1", Option.of("desc1"), Option.of("subj1"),
                Option.of("sender1"), Option.of(originalJson));

        MailMessage mailMessage = MailMessageCreator.create(eventEmailInfo);
        String html = checkSubjAndGetHtmlBody(mailMessage);

        String jsonFromHtml = extractJsonFromHtml(html);
        Assert.isTrue(jsonFromHtml.contains("http:\\/\\/schema.org")); // slashes are escaped by JsonObject.serialize()
        Assert.equals(originalJson, JsonObject.parseObject(jsonFromHtml));
    }

    @Test
    public void createEventEmailWithoutJson() {
        EventEmailInfo eventEmailInfo = new EventEmailInfo("event1", Option.of("desc1"), Option.of("subj1"),
                Option.of("sender1"), Option.empty());

        MailMessage mailMessage = MailMessageCreator.create(eventEmailInfo);
        String html = checkSubjAndGetHtmlBody(mailMessage);

        Assert.isFalse(html.contains("<script type=\"application/ld+json\">"));
    }

    @Test
    public void createEventEmailWithJsonWithHtmlInsideAttrValue() {
        JsonObject originalJson = JsonObject.parseObject("{\"x\":\"<script><![CDATA[]]]]>]]></script>\"}");
        EventEmailInfo eventEmailInfo = new EventEmailInfo("event1", Option.of("desc1"), Option.of("subj1"),
                Option.of("sender1"), Option.of(originalJson));

        MailMessage mailMessage = MailMessageCreator.create(eventEmailInfo);
        String html = checkSubjAndGetHtmlBody(mailMessage);

        String jsonFromHtml = extractJsonFromHtml(html);
        Assert.assertContains(jsonFromHtml, "<script><![CDATA[]]]]>]]><\\/script>");// slash is escaped by JsonObject.serialize()
        Assert.equals(originalJson, JsonObject.parseObject(jsonFromHtml));
    }

    private String extractJsonFromHtml(String html) {
        Assert.isTrue(html.contains("<script type=\"application/ld+json\">"));
        Pattern2 p = new Pattern2(Pattern.compile(
                "<script type=\"application/ld\\+json\">(.*?)</script>", Pattern.MULTILINE & Pattern.DOTALL));
        Matcher2 m = p.matcher2(html);
        Assert.isTrue(m.find(), "script with application/ld+json found, but couldn't be extracted by regex");
        Option<String> jsonFromHtml = m.group(1);
        Assert.some(jsonFromHtml);
        return jsonFromHtml.get();
    }

    private String checkSubjAndGetHtmlBody(MailMessage mailMessage) {
        Assert.some("[sender1] subj1", mailMessage.getSubject());
        ListF<BodyPart> htmlParts =
                mailMessage.getBottomPartsWithTypeSubtype(ContentType.TEXT_HTML_UTF8.getTypeSubtype());
        Assert.sizeIs(1, htmlParts);
        return ((DefaultContent) htmlParts.first().getContent()).getValue().toString();
    }
}
