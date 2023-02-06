package ru.yandex.autotests.innerpochta.hound.v2.positive;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.hound.V2FirstEnvelopeDateResponse;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.*;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка v2/first_envelope_date")
@Description("Тесты на ручку v2/first_envelope_date")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2GetFirstEnvelopeDateTest")
public class FirstEnvelopeDatePositiveTest extends BaseHoundTest {

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public DeleteFoldersRule folderDelete = DeleteFoldersRule.with(authClient).all();

    @Test
    @Title("first_envelope_date с непустой inbox папкой")
    public void firstEnvelopeDateWithInboxFid() throws Exception {
        List<Envelope> envelopes = sendWith(authClient).viaProd().count(3).send().waitDeliver().
                getEnvelopes();
        long utcTimestamp = envelopes.stream()
                .map(Envelope::getReceiveDate)
                .min(Comparator.naturalOrder())
                .get();

        Long response = apiHoundV2().firstEnvelopeDate().withUid(uid()).withFid(folderList.defaultFID()).
                get(shouldBe(ok200())).as(V2FirstEnvelopeDateResponse.class).getFirstEnvelopeDate();

        assertThat(response, CoreMatchers.equalTo(utcTimestamp));
    }

    @Test
    @Title("first_envelope_date с непустой пользовательской папкой")
    public void firstEnvelopeDateWithUserFid() throws Exception {
        String fid = Mops.newFolder(authClient, Util.getRandomString());

        List<Envelope> envelopes = sendWith(authClient).viaProd().count(3).send().waitDeliver().
                getEnvelopes();
        List<String> mids = envelopes.stream()
                .map(Envelope::getMid)
                .collect(Collectors.toList());
        Mops.complexMove(authClient, fid, new MidsSource(mids))
                .post(shouldBe(okSync()));
        long utcTimestamp = envelopes.stream()
                .map(Envelope::getReceiveDate)
                .min(Comparator.naturalOrder())
                .get();

        Long response = apiHoundV2().firstEnvelopeDate().withUid(uid()).withFid(fid).
                get(shouldBe(ok200())).as(V2FirstEnvelopeDateResponse.class).getFirstEnvelopeDate();

        assertThat(response, CoreMatchers.equalTo(utcTimestamp));
    }
}
