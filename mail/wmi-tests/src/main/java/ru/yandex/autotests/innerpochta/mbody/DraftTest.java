package ru.yandex.autotests.innerpochta.mbody;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.mbody.Body;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.beans.mdoby.Flag;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.savedraft.ApiSaveDraft;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.List;

import static java.util.function.Function.identity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.fromClasspath;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("Показ черновиков")
@Description("Проверяем корректное отображение черновиков в message-body")
@Features(MyFeatures.MBODY)
@Stories({MyStories.DRAFT, MyStories.MBODY})
@Credentials(loginGroup = "MbodyDraft")
public class DraftTest extends MbodyBaseTest {
    private static final String SMILE_HTML_PATH = "sanitizer/cid-smile.htm";

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    private String saveSmileDraft(boolean isHtml) throws IOException {
        return sendWith(authClient)
                .viaProd()
                .subj(Util.getRandomString())
                .text(fromClasspath(SMILE_HTML_PATH))
                .html(isHtml ? ApiSaveDraft.HtmlParam.YES.value() : ApiSaveDraft.HtmlParam.NO.value())
                .saveDraft()
                .waitDeliver()
                .getMid();
    }

    private String getMbodyContent(Mbody res) {
        final List<Body> bodies = res.getBodies();
        assertThat("Выдача mbody должна содержать несколько элементов в поле bodies", bodies,
            hasSize(greaterThan(0)));
        return bodies.get(0).getTransformerResult().getTextTransformerResult().getContent();
    }

    @Test
    @Title("Для черновика подставляем вместо cid-а ссылку на webattach")
    @Description("Если передан флаг Draft и письмо text/html, то вместо ссылки с cid-ом " +
        "у картинки/смайла должна быть ссылка на webattach с нужными параметрами")
    @Issue("MAILDEV-126")
    public void testInlineAttachInDraft() throws IOException {
        final Mbody res = apiMbody().message()
            .withFlags(Flag.DRAFT.toString())
            .withMid(saveSmileDraft(true))
            .withUid(uid())
            .get(identity()).peek().as(Mbody.class);
        final String content = getMbodyContent(res);

        assertThat("Аттрибут src у img должен содержать ссылку на message_part_real",
            content, containsString(props().webattachStable()+"/message_part_real/?sid="));
        assertThat("Аттрибут src у img должен содержать параметр no_disposition",
            content, containsString("no_disposition=y"));
        assertThat("Аттрибут src у img должен содержать параметр про класс yandex_new_inline",
            content, containsString("yandex_class=yandex_new_inline"));
    }

    @Test
    @Title("Для text/plain черновика выдаем письмо как есть")
    @Description("Если передан флаг Draft и письмо text/plain, то mbody должен выдавать текст " +
        "письма неизменным из хранилища")
    @Issue("MAILDEV-126")
    public void testTextDraft() throws IOException {
        final Mbody res = apiMbody().message()
            .withFlags(Flag.DRAFT.toString())
            .withMid(saveSmileDraft(false))
            .withUid(uid())
            .get(identity()).peek().as(Mbody.class);
        final String content = getMbodyContent(res);
        final String draftContent = fromClasspath(SMILE_HTML_PATH);

        assertThat("Выдача mbody должна соответствовать исходному тексту черновика",
            content.trim(), equalTo(draftContent));
    }

    @Test
    @Title("В черновиках не должны оборачивать ссылки в re.jsx")
    @Description("Если передан флаг Draft, то mbody не должен оборачивать ссылки в re.jsx")
    @Issue("MAILDEV-707")
    public void testDraftWithUrl() throws IOException {
        final String subj = Util.getRandomString();
        final String draftContent = "<a href=\"http://yandex.ru\">http://yandex.ru</a>";
        String mid = sendWith(authClient)
                .viaProd()
                .subj(subj)
                .text(draftContent)
                .html(ApiSaveDraft.HtmlParam.YES.value())
                .saveDraft()
                .waitDeliver()
                .getMid();
        final Mbody res = apiMbody().message()
            .withFlags(Flag.DRAFT.toString())
            .withMid(mid)
            .withUid(uid())
            .get(identity()).peek().as(Mbody.class);
        final String content = getMbodyContent(res);

        assertThat("Выдача mbody должна соответствовать исходному тексту черновика",
            content.trim(), equalTo(draftContent));
    }
}
