package ru.yandex.autotests.innerpochta.mbody;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.beans.mdoby.Flag;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendmessage.ApiSendMessage;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static java.util.function.Function.identity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.MessagesWithInlines.getInlineHtml;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

@Aqua.Test
@Title("Инлайновые аттачи в mbody")
@Features(MyFeatures.MBODY)
@Stories(MyStories.MBODY)
@Credentials(loginGroup = "InlineAttachesTest")
@RunWith(DataProviderRunner.class)
public class InlineAttachesTest extends MbodyBaseTest {

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Генерация ссылки на инлайновую картинку")
    @Description("Проверяем, что mbody по cid-у приаттаченной картинки сгенерирует ссылку " +
        "на эту картинку в теле письма, где cid используется.")
    public void testInlineImage() throws IOException {
        File inline = File.createTempFile(getRandomString(), null);

        asByteSink(inline).write(asByteSource(getResource("img/imgrotated/not_rotate.jpg")).read());
        String sid = sendWith(authClient).writeAttachment(inline);
        String viewLargeUrl = props().webattachHost() + "/message_part_real/?sid=" + sid + "&no_disposition=y";

        Envelope envelope = sendWith(authClient)
                .viaProd()
                .text(getInlineHtml(sid, viewLargeUrl))
                .html(ApiSendMessage.HtmlParam.YES.value())
                .send()
                .waitDeliver()
                .getEnvelope().orElse(null);

        assertNotNull("Не нашли отправленное письмо", envelope);

        Mbody mbody = apiMbody().message()
            .withUid(uid())
            .withMid(envelope.getMid())
            .withFlags(Flag.XML_STREAMER_ON.toString())
            .get(identity()).peek().as(Mbody.class);
        String content = mbody.getBodies().get(0).getTransformerResult()
            .getTextTransformerResult().getContent();

        assertThat("В тепе письма должна содержаться ссылка на message_part", content,
            containsString("src=\"../message_part/"));
        assertThat("У ссылки должнен быть правильный параметр yandex_class", content,
            containsString("yandex_class=yandex_inline_content_" + envelope.getStid()));
    }

    @Test
    @Title("Имя файла в инлайновом аттаче должно быть заурлэнкожено")
    @Issue("MAILPG-1857")
    @DataProvider({
        "%",
        "Ё",
        "<",
        "="
    })
    public void shouldUrlencodeReservedOrNotAllowedSymbolsInInlineAttachName(String charForUrlEncode) throws IOException {
        File inline = File.createTempFile(charForUrlEncode + getRandomString(), null);

        asByteSink(inline).write(asByteSource(getResource("img/imgrotated/not_rotate.jpg")).read());
        String sid = sendWith(authClient).writeAttachment(inline);
        String viewLargeUrl = props().webattachHost() + "/message_part_real/?sid=" + sid + "&no_disposition=y";

        Envelope envelope = sendWith(authClient)
            .text(getInlineHtml(sid, viewLargeUrl))
            .html(ApiSendMessage.HtmlParam.YES.value())
            .send()
            .waitDeliver()
            .getEnvelope().orElse(null);

        assertNotNull("Не нашли отправленное письмо", envelope);

        Mbody mbody = apiMbody().message()
            .withUid(uid())
            .withMid(envelope.getMid())
            .withFlags(Flag.XML_STREAMER_ON.toString())
            .get(identity()).peek().as(Mbody.class);
        String content = mbody.getBodies().get(0)
            .getTransformerResult()
            .getTextTransformerResult()
            .getContent();

        String encodedChar = URLEncoder.encode(charForUrlEncode, StandardCharsets.UTF_8.name());
        assertThat("В тепе письма должна содержаться ссылка на message_part", content,
            containsString("src=\"../message_part/" + encodedChar));
    }
}
