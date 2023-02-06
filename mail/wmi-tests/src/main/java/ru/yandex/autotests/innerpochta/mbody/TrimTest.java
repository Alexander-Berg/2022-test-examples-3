package ru.yandex.autotests.innerpochta.mbody;

import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.function.Function.identity;
import static org.cthul.matchers.chain.AndChainMatcher.both;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.autotests.innerpochta.beans.mbody.BodyMatchers.withTransformerResult;
import static ru.yandex.autotests.innerpochta.beans.mbody.MbodyMatchers.withBodies;
import static ru.yandex.autotests.innerpochta.beans.mbody.TextTransformerResultMatchers.withIsTrimmed;
import static ru.yandex.autotests.innerpochta.beans.mbody.TransformerResultMatchers.withTextTransformerResult;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.NO_TRIM;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;


@Aqua.Test
@Title("[MBODY] Сервис mbody на 8888 порту")
@Features(MyFeatures.MBODY)
@Stories(MyStories.MBODY)
@Credentials(loginGroup = "TrimTest")
@Issue("MAILPG-1938")
public class TrimTest extends MbodyBaseTest {
    private static final Integer trimThreshold = 1572864;
    private static final Integer significantDifference = 10000;

    // Actual size of body is slightly bigger than the original/limit as the content is being wrapped in some html tags
    private static final Integer insignificantDifference = 100;

    private static String mid;

    private Matcher bodyIsTrimmed(boolean e) {
        return withBodies(
                hasItem(
                        (Matcher) withTransformerResult(
                                withTextTransformerResult(
                                        withIsTrimmed(is(e))
                                )
                        )
                )
        );
    }

    private Matcher bodyIsTrimmed() {
        return bodyIsTrimmed(true);
    }

    private Matcher bodyIsNotTrimmed() {
        return bodyIsTrimmed(false);
    }

    private Matcher<Integer> sizeIsTrimmed() {
        return lessThanOrEqualTo(trimThreshold + insignificantDifference);
    }

    private Matcher<Integer> sizeIsNotTrimmed() {
        return greaterThan(trimThreshold + insignificantDifference);
    }

    private Matcher<Integer> sizeIsInRange(int length) {
        return both(greaterThanOrEqualTo(length)).and(lessThanOrEqualTo(length + insignificantDifference)).build();
    }

    @ClassRule
    public static CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @BeforeClass
    public static void prepare() {
        String longText = new String(new char[trimThreshold + significantDifference]).replace('\0', 'Z');
        Envelope envelope = sendWith(authClient)
                .viaProd()
                .text(longText)
                .send()
                .waitDeliver()
                .getEnvelope().orElse(null);

        assertNotNull("Не нашли отправленное письмо", envelope);

        mid = envelope.getMid();
    }

    @Test
    @Title("Должны обрезать слишком длинный текст письма")
    public void shouldTrimExtremelyLongBody() {
        Mbody res = apiMbody().message()
                .withMid(mid)
                .withUid(uid())
                .get(identity())
                .as(Mbody.class);

        assertThat(res, bodyIsTrimmed());


        String content = res.getBodies()
                .get(0)
                .getTransformerResult()
                .getTextTransformerResult()
                .getContent();

        assertThat("Письмо почему-то обрезалось",
                content.length(),
                sizeIsTrimmed());
    }

    @Test
    @Title("Не должны обрезать короткий текст письма")
    public void shouldNotTrimShortBody() {
        String shortText = "short text";
        Envelope envelope = sendWith(authClient)
                .viaProd()
                .text(shortText)
                .send()
                .waitDeliver()
                .getEnvelope().orElse(null);

        assertNotNull("Не нашли отправленное письмо", envelope);


        String midShort = envelope.getMid();
        Mbody res = apiMbody().message()
                .withMid(midShort)
                .withUid(uid())
                .get(identity())
                .as(Mbody.class);

        assertThat(res, bodyIsNotTrimmed());


        String content = res.getBodies()
                .get(0)
                .getTransformerResult()
                .getTextTransformerResult()
                .getContent();

        assertThat("Письмо почему-то обрезалось",
                content.length(),
                sizeIsInRange(content.length()));
    }


    @Test
    @Title("Не должны обрезать слишком длинный текст письма при запросе с флагом NoTrim")
    public void shouldNotTrimLongBodyWithNOTRIMParam() {
        Mbody res = apiMbody().message()
                .withFlags(NO_TRIM.toString())
                .withMid(mid)
                .withUid(uid())
                .get(identity())
                .as(Mbody.class);

        assertThat(res, bodyIsNotTrimmed());


        String content = res.getBodies()
                .get(0)
                .getTransformerResult()
                .getTextTransformerResult()
                .getContent();

        assertThat("Письмо почему-то обрезалось",
                content.length(),
                sizeIsNotTrimmed());
    }
}