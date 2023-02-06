package ru.yandex.autotests.innerpochta.sendbernar;


import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Scope;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgsWithLids;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes.PRODUCTION;

@Aqua.Test
@Title("Ручка send_message")
@Description("Отправляем письмо с меншнами")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "MentionsTest")
public class MentionsTest extends BaseSendbernarClass {
    @ClassRule
    public static HttpClientManagerRule mention2 = auth().with("MentionsTest2");

    @Rule
    public CleanMessagesMopsRule cleanMention2 = new CleanMessagesMopsRule(mention2).inbox().outbox();

    @ClassRule
    public static HttpClientManagerRule mention3 = auth().with("MentionsTest3");

    @Rule
    public CleanMessagesMopsRule cleanMention3 = new CleanMessagesMopsRule(mention3).inbox().outbox();

    private static List<String> lids(HttpClientManagerRule rule) {
        return Arrays.asList(
                lidByName("mention_label", rule),
                lidByName("mention_unvisited_label", rule)
        );
    }

    @Test
    @Scope(PRODUCTION)
    public void shouldNotFailWithNonyandexMention() {
        sendMessage()
                .withTo("vicdev@yahoo.com")
                .withSubj(subj)
                .withMentions("vicdev@yahoo.com")
                .post(shouldBe(ok200()));

        waitWith.sent().subj(subj).waitDeliver();
    }

    @Test
    public void shouldSetMentionsLabel() {
        sendMessage()
                .withTo(mention2.acc().getSelfEmail()+","+mention3.acc().getSelfEmail())
                .withSubj(subj)
                .withMentions(mention2.acc().getSelfEmail())
                .withMentions(mention3.acc().getSelfEmail())
                .post(shouldBe(ok200()));


        String mid2 = waitWith.usingHttpClient(mention2).subj(subj).inbox().waitDeliver().getMid();
        String mid3 = waitWith.usingHttpClient(mention3).subj(subj).inbox().waitDeliver().getMid();


        List<String> lids2 = lids(mention2);
        List<String> lids3 = lids(mention3);

        assertThat("Нет метки на письме",
                mention2,
                hasMsgsWithLids(Collections.singletonList(mid2), lids2));


        assertThat("Нет метки на письме",
                mention3,
                hasMsgsWithLids(Collections.singletonList(mid3), lids3));
    }
}
