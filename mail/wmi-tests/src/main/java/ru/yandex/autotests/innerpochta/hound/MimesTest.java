package ru.yandex.autotests.innerpochta.hound;

import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.seleniumhq.jetty7.http.HttpStatus;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.yplatform.MimePart;
import ru.yandex.autotests.innerpochta.beans.yplatform.Mimes;
import ru.yandex.autotests.innerpochta.beans.yplatform.MimesError;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Scope;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.MulcagateClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ErrorMatchers.withCode;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ErrorMatchers.withMessage;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ErrorMatchers.withReason;
import static ru.yandex.autotests.innerpochta.beans.yplatform.MessagePartsMatchers.withRoot;
import static ru.yandex.autotests.innerpochta.beans.yplatform.MimePartMatchers.withCharset;
import static ru.yandex.autotests.innerpochta.beans.yplatform.MimePartMatchers.withContentSubtype;
import static ru.yandex.autotests.innerpochta.beans.yplatform.MimePartMatchers.withContentType;
import static ru.yandex.autotests.innerpochta.beans.yplatform.MimePartMatchers.withEncoding;
import static ru.yandex.autotests.innerpochta.beans.yplatform.MimePartMatchers.withHid;
import static ru.yandex.autotests.innerpochta.beans.yplatform.MimesErrorMatchers.withError;
import static ru.yandex.autotests.innerpochta.beans.yplatform.MimesMatchers.withMimes;
import static ru.yandex.autotests.innerpochta.beans.yplatform.RootMessagePartMatchers.withMimeParts;
import static ru.yandex.autotests.innerpochta.beans.yplatform.RootMessagePartMatchers.withStid;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiHound;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes.TESTING;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsMapWithSize.anEmptyMap;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.CommonUtils.getResource;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка /mimes")
@Description("Должны возвращать stid'ы и MIME-part'ы писем корневой части и windat-аттачей")
@Features(MyFeatures.HOUND)
@Stories(MyStories.ATTACH)
@Credentials(loginGroup = "HoundMimes")
@Issue("MAILDEV-672")
public class MimesTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Формат выдачи успешного запроса")
    @Description("Должны на успешный запрос получать MIME-part'ы в JSON соответствующий схеме")
    public void shouldReturnJsonAppropriateToSchemeWithMimePartsOnSuccess() throws Exception {
        final String mid = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        apiHound().mimes().withUid(uid()).withMid(mid)
                .get(identity()).then()
                .assertThat().statusCode(HttpStatus.OK_200).contentType("application/json")
                .body(matchesJsonSchema(getResource(getClass(), "/jsonschema/yplatform/mimes.json")));
    }

    @Test
    @Title("Формат выдачи неудачного запроса")
    @Description("Должны на неудачный запрос получать ошибку в JSON соответствующий схеме")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReturnJsonAppropriateToSchemeWithErrorOnFail() throws IOException {
        apiHound().mimes()
                .get(identity()).then()
                .assertThat().statusCode(HttpStatus.OK_200).contentType("application/json")
                .body(matchesJsonSchema(getResource(getClass(), "/jsonschema/yplatform/mimes_error.json")));
    }

    @Test
    @Title("Запрос без uid")
    @Description("Должны получать ошибку на запрос без uid")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReturnErrorWhenNoUid() throws IOException {
        final MimesError result = apiHound().mimes()
                .get(identity()).peek().as(MimesError.class);

        assertThat(result, withError(allOf(
                withCode(is(5001L)),
                withMessage(is("invalid argument")),
                withReason(is("uid parameter is required"))
        )));
    }

    @Test
    @Title("Запрос с невалидным uid")
    @Description("Должны получать ошибку на запрос с невалидным uid")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReturnErrorWhenInvalidUid() throws IOException {
        final MimesError result = apiHound().mimes().withUid("text")
                .get(identity()).peek().as(MimesError.class);

        assertThat(result, withError(allOf(
                withCode(is(5001L)),
                withMessage(is("invalid argument")),
                withReason(startsWith("Sharpei service responded with error code 400"))
        )));
    }

    @Test
    @Title("Запрос без mid")
    @Description("Должны получать пустой объект mimes на запрос без mid'ов")
    public void shouldReturnEmptyMimesWhenNoMids() throws IOException {
        final Mimes result = apiHound().mimes().withUid(uid())
                .get(identity()).peek().as(Mimes.class);

        assertThat(result, withMimes((Matcher) anEmptyMap()));
    }

    @Test
    @Title("Наличие stid в ответе")
    @Description("Должны получать stid письма по mid")
    public void shouldReturnStidForMid() throws Exception {
        final Envelope envelope = sendWith(authClient).viaProd().send().waitDeliver().getEnvelope().get();
        final String mid = envelope.getMid();
        final String stid = envelope.getStid();

        final Mimes result = apiHound().mimes().withUid(uid()).withMid(mid)
                .get(identity()).peek().as(Mimes.class);

        assertThat(result, withMimes((Matcher) hasEntry(is(mid), withRoot(withStid(is(stid))))));
    }

    @Test
    @Title("Опционально парсим метаданные о парте, если передан параметр with_mulca=yes")
    @Description("Проверяем, что для старого письма ручка /mimes с with_mulca=yes выдает " +
        "mime-парты. Когда все письма в почте будут перенесены в MDS, тест можно удалить.")
    @Issue("MAILDEV-819")
    @Ignore("MAILDEV-947")
    public void shouldAlwaysReturnMimePartsForWithMulcaYes() {
        final String mid = "162411061562049175";
        final Mimes withoutMulcaRes = apiHound().mimes()
            .withUid(uid())
            .withMid(mid)
            .get(identity()).peek().as(Mimes.class);
        assertThat(withoutMulcaRes,
            withMimes((Matcher) hasEntry(
                is(mid),
                withRoot(
                    withMimeParts((Matcher)anEmptyMap())
                )
            ))
        );

        final Mimes withMulcaRes = apiHound().mimes()
            .withUid(uid())
            .withMid(mid)
            .withWithMulca("yes")
            .get(identity()).peek().as(Mimes.class);
        assertThat(withMulcaRes,
            withMimes((Matcher) hasEntry(
                is(mid),
                withRoot(
                    withMimeParts(allOf(
                        hasEntry(is("1"), allOf(
                            withHid(is("1")),
                            withContentType(is("multipart")),
                            withContentSubtype(is("mixed")),
                            withCharset(is("US-ASCII")),
                            withEncoding(is("7bit"))
                        )),
                        hasEntry(is("1.1"), allOf(
                            withHid(is("1.1")),
                            withContentType(is("text")),
                            withContentSubtype(is("html")),
                            withCharset(is("utf-8")),
                            withEncoding(is("8bit"))
                        )),
                        hasEntry(is("1.2"), allOf(
                            withHid(is("1.2")),
                            withContentType(is("image")),
                            withContentSubtype(is("png")),
                            withCharset(is("US-ASCII")),
                            withEncoding(is("base64"))
                        )),
                        hasEntry(is("1.3"), allOf(
                            withHid(is("1.3")),
                            withContentType(is("application")),
                            withContentSubtype(is("pdf")),
                            withCharset(is("US-ASCII")),
                            withEncoding(is("base64"))
                        ))
                    ))
                )
            ))
        );
    }

    @Test
    @Title("Опционально метаданные вложенных партов из вложенного письма, если передан with_inline=yes")
    @Description("Форвардим письмо как вложение. Дергаем ручку /mimes с with_inline=yes и " +
        "проверяем, что вложенные парты выдаются.")
    @Issue("MAILDEV-819")
    public void shouldReturnInlineMsgMimePartsForWithInlineYes() throws Exception {
        final String text = "hello inline letter";
        final String originalMid = sendWith(authClient).viaProd().text(text).send().waitDeliver().getMid();
        final String mid = sendWith(authClient).viaProd().addFwdMids(originalMid).send().waitDeliver().getMid();

        final Mimes result = apiHound().mimes()
            .withUid(uid())
            .withMid(mid)
            .withWithInline("yes")
            .get(identity()).peek().as(Mimes.class);

        assertThat(result,
            withMimes((Matcher) hasEntry(
                is(mid),
                withRoot(
                    withMimeParts(allOf(
                        hasEntry(is("1.1"), allOf(
                            withHid(is("1.1")),
                            withContentType(is("message")),
                            withContentSubtype(is("rfc822")),
                            withCharset(is("US-ASCII")),
                            withEncoding(is("8bit"))
                        )),
                        hasEntry(is("1.1.1"), allOf(
                            withHid(is("1.1.1")),
                            withContentType(is("text")),
                            withContentSubtype(is("plain")),
                            withCharset(is("US-ASCII")),
                            withEncoding(is("7bit"))
                        ))
                    ))
                )
            ))
        );
    }

    @Test
    @Title("Проверяем, что по полученным оффсетам мы действительно скачаем из mulcagate правильный парт")
    @Description("Форвардим письмо как вложение. Дергаем ручку /mimes с with_inline=yes и " +
            "проверяем, что вложенные парты выдаются.")
    @Issue("MAILDEV-819")
    @Scope(TESTING)
    public void shouldReturnInlineMsgMimePartsForWithInlineYesCheckPart() throws Exception {
        final String text = "hello inline letter";
        final String originalMid = sendWith(authClient).viaProd().text(text).send().waitDeliver().getMid();
        final String mid = sendWith(authClient).viaProd().addFwdMids(originalMid).send().waitDeliver().getMid();

        final Mimes result = apiHound().mimes()
                .withUid(uid())
                .withMid(mid)
                .withWithInline("yes")
                .get(identity()).peek().as(Mimes.class);

        final String stid = result.getMimes().get(mid).getRoot().getStid();
        final MimePart inlinePart = result.getMimes().get(mid).getRoot().getMimeParts().get("1.1.1");
        MulcagateClient mulcagate = new MulcagateClient(props().mulcagateUri());
        final String storageData = mulcagate.get(stid, inlinePart.getOffsetBegin(),
                inlinePart.getOffsetEnd());
        assertThat(storageData, is(text));
    }
}
