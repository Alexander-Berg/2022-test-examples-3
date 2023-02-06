package ru.yandex.autotests.innerpochta.mbody;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.beans.mdoby.Flag;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.DiskAttachHelper;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static java.util.function.Function.identity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("Дисковые аттачи в mbody")
@Features(MyFeatures.MBODY)
@Stories(MyStories.MBODY)
@Credentials(loginGroup = "DiskAttachesTest")
@RunWith(DataProviderRunner.class)
public class DiskAttachesTest extends MbodyBaseTest {

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("В поле hash кладется параметр hash из url")
    @Issue("MAILDEV-2050")
    public void shouldContainHashFromUrl() throws IOException {
        DiskAttachHelper attach = DiskAttachHelper.withHashInUrl();

        Envelope envelope = sendWith(authClient)
            .addDiskAttaches(attach)
            .send()
            .waitDeliver()
            .getEnvelope().orElse(null);

        assertNotNull("Не нашли отправленное письмо", envelope);

        Mbody mbody = apiMbody().message()
            .withUid(uid())
            .withMid(envelope.getMid())
            .withFlags(Flag.XML_STREAMER_ON.toString())
            .get(identity()).peek().as(Mbody.class);
        String url = mbody.getAttachments().get(0)
            .getNarodTransformerResult().get(0)
            .getUrl();
        String hash = mbody.getAttachments().get(0)
            .getNarodTransformerResult().get(0)
            .getHash();

        assertThat("В поле hash должен содержаться hash из url", url,
            containsString("hash=" + hash));

    }

    @Test
    @Title("data-hash аттрибут должен содержать хеш, переданный в json")
    @Issue("MAILPG-4671")
    public void shouldContainHashFromDataAttributeNotFromUrl() throws IOException {
        DiskAttachHelper attach = new DiskAttachHelper();

        Envelope envelope = sendWith(authClient)
                .addDiskAttaches(attach)
                .send()
                .waitDeliver()
                .getEnvelope().orElse(null);

        assertNotNull("Не нашли отправленное письмо", envelope);

        Mbody mbody = apiMbody().message()
                .withUid(uid())
                .withMid(envelope.getMid())
                .withFlags(Flag.XML_STREAMER_ON.toString())
                .get(identity()).peek().as(Mbody.class);

        String hash = mbody.getAttachments().get(0)
                .getNarodTransformerResult().get(0)
                .getHash();

        assertEquals("Должен содержать хеш, полученный из data аттрибута", attach.getHash(), hash);
    }
}
