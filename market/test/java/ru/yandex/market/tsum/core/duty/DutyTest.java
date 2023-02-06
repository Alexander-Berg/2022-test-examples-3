package ru.yandex.market.tsum.core.duty;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.tsum.core.duty.extractor.LoginExtractor;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 10/08/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class DutyTest {

    private static String getEventNameFromDutyName(String dutyName) {
        return String.format("Дежурный %s@", dutyName);
    }

    @Test
    public void testExtractLogin() {
        LoginExtractor extractor = DutyLoginExtractors.defaultExtractor();
        Assert.assertEquals("andreevdm", extractor.extractLogin("andreevdm@incident"));
        Assert.assertEquals("andreevdm", extractor.extractLogin("andreevdm@ops"));
        Assert.assertEquals("andreevdm", extractor.extractLogin("andreevdm@support"));
        Assert.assertEquals("andreevdm", extractor.extractLogin("andreevdm"));
        Assert.assertEquals("andreevdm", extractor.extractLogin("@AndreevDm "));
        Assert.assertEquals("andreevdm", extractor.extractLogin("Дежурство @andreevdm "));
        Assert.assertEquals("andreevdm", extractor.extractLogin("Сегодня дежурный @andreevdm!!!"));
        Assert.assertEquals("antsa4", extractor.extractLogin("Никого нет, отдувается менеджер antsa4"));
        Assert.assertEquals("iurik", extractor.extractLogin(
                "[IDX] iurik@incidents, green-yeti@release, bzz13@support"
        ));
        Assert.assertEquals("pochemuto", extractor.extractLogin(
                "[Дежурство в МВО] s-ermakov@, moskovkin@, galaev@(RM), pochemuto@(FM)"
        ));
        Assert.assertEquals("mrgrien", extractor.extractLogin("Дежурство mrgrien@, jkt@"));
        Assert.assertEquals("a-shar", extractor.extractLogin("Дежурство a-shar@, mariakuz@"));
    }

    @Test
    public void testExtractFirstLogin() {
        LoginExtractor extractor = DutyLoginExtractors.first();
        Assert.assertEquals("andreevdm", extractor.extractLogin("andreevdm@"));
        Assert.assertEquals("andreevdm", extractor.extractLogin("1 andreevdm@ops"));
    }

    @Test
    public void testExtractIdxDutyLogin() {
        LoginExtractor extractor = DutyLoginExtractors.idx();
        Assert.assertEquals("iurik", extractor.extractLogin("[IDX] iurik@incidents, green-yeti@release, bzz13@support"));
        Assert.assertEquals("iurik", extractor.extractLogin("[IDX]iurik@incidents, green-yeti@release, bzz13@support"));
        Assert.assertEquals("a-square", extractor.extractLogin("[IDX] a-square@incidents, green-yeti@release, bzz13@support"));
        Assert.assertEquals("bzz13", extractor.extractLogin("[IDX] bzz13@incidents, green-yeti@release, kgorelov@support"));
    }

    @Test
    public void testExtractMboDutyLogin() {
        LoginExtractor extractor = DutyLoginExtractors.mbo();
        Assert.assertEquals("pochemuto", extractor.extractLogin("[Дежурство в МВО] s-ermakov@, moskovkin@, galaev@(RM), pochemuto@(FM)"));
        Assert.assertEquals("pochemuto", extractor.extractLogin("[Дежурство в МВО] s-ermakov@, moskovkin@, pochemuto@ (FM), galaev@(RM)"));
        Assert.assertEquals("pochemuto", extractor.extractLogin("[Дежурство в МВО]pochemuto@(FM), s-ermakov@, moskovkin@, galaev@(RM)"));
        Assert.assertEquals("s-ermakov", extractor.extractLogin("[Дежурство в МВО] s-ermakov@(FM), moskovkin@, galaev@(RM), pochemuto@"));
    }

    @Test
    public void testExtractMarketstatDutyLogin() {
        LoginExtractor extractor = DutyLoginExtractors.marketstat();
        Assert.assertEquals("kateleb", extractor.extractLogin("Дежурство @kateleb"));
        Assert.assertEquals("kateleb", extractor.extractLogin("Дежурство @kateleb\n\n,блаблабла:"));
        Assert.assertEquals("kateleb", extractor.extractLogin("Дежурство  @kateleb"));
        Assert.assertEquals("kateleb", extractor.extractLogin("Дежурство аналитики: konovalovsv@\n\nДежурство @kateleb"));
        Assert.assertEquals("", extractor.extractLogin("Дежурство аналитики: konovalovsv@"));
    }

    @Test
    public void testExtractLogisticsDeliveryDutyLogin() {
        LoginExtractor extractor = DutyLoginExtractors.logistics();
        Assert.assertEquals("avetokhin", extractor.extractLogin("[NSK][RELEASE][FUCKUP]avetokhin"));
        Assert.assertEquals("avetokhin", extractor.extractLogin("[NSK][RELEASE][FUCKUP] avetokhin "));
        Assert.assertEquals("avetokhin", extractor.extractLogin("[RELEASE][FUCKUP]avetokhin"));
        Assert.assertEquals("avetokhin", extractor.extractLogin("[FUCKUP]avetokhin"));
        Assert.assertEquals("avetokhin", extractor.extractLogin("[NSK][RELEASE][FUCKUP]avetokhin@"));
        Assert.assertEquals("anclav", extractor.extractLogin("[RELEASE][FUCKUP]anclav@ (backup: aezhko@)"));
        Assert.assertEquals("Shift-red", extractor.extractLogin("[МарДо][RELEASE][FUCKUP]Shift-red, aezhko"));
        Assert.assertEquals("avetokhin", extractor.extractLogin("[NSK][RELEASE][FUCKUP] avetokhin , sogreshilin "));
        Assert.assertEquals("", extractor.extractLogin("[NSK][RELEASE]avetokhin"));
        Assert.assertEquals("", extractor.extractLogin("[NSK][SUPPORT]avetokhin"));
        Assert.assertEquals("", extractor.extractLogin("[WMS]avetokhin"));
        Assert.assertEquals("", extractor.extractLogin("[NSK]avetokhin"));
    }

    @Test
    public void testExtractCheckouterReleaseMasterDutyLogin() {
        LoginExtractor extractor = DutyLoginExtractors.checkouterRelease();
        Assert.assertEquals("frodo", extractor.extractLogin("Дежурство @gendalf, @bilbo, @frodo"));
        Assert.assertEquals("", extractor.extractLogin("Дежурство @gendalf, @bilbo"));
        Assert.assertEquals("frodo", extractor.extractLogin("Дежурство @gendalf,   @bil-bo, @frodo"));
        Assert.assertEquals("fro-do", extractor.extractLogin("Дежурство @gendalf, @bilbo, @fro-do"));
        Assert.assertEquals("frodo", extractor.extractLogin("Дежурство @gendalf, @bilbo, @frodo\n"));
    }

}
