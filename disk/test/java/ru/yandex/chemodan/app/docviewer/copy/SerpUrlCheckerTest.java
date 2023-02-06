package ru.yandex.chemodan.app.docviewer.copy;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author bursy
 */
public class SerpUrlCheckerTest extends DocviewerSpringTestBase {

    @Autowired
    private SerpUrlChecker serpUrlChecker;

    @Test
    public void verify() {
        String params = "name=112017.pdf&tm=1581742088&tld=ru&text=mime%3Apdf"
                + "&url=https%3A%2F%2Fwww.nalog.ru%2Fhtml%2Fsites%2Fwww.rn31.nalog.ru%2Fnews%2F112017.pdf"
                + "&lr=2&mime=pdf&l10n=ru&sign=982247f6b8e849cf23883263f36bb5c3&keyno=0";
        Assert.isTrue(serpUrlChecker.verify(params));

        params = "name=14382.pdf&tm=1581742088&tld=ru&text=mime%3Apdf"
                + "&url=http%3A%2F%2Fwww.rusbandy.ru%2Fpdf%2F14382.pdf&lr=2&mime=pdf&l10n=ru"
                + "&sign=cf9f1dba2b343ea890585d7b50b70858&keyno=0";
        Assert.isTrue(serpUrlChecker.verify(params));

        params = "name=e51d4be907e69224ce34f5075af1e3f9&tm=1581742088&tld=ru&text=mime%3Apdf"
                + "&url=http%3A%2F%2Fwww.gpmradio.ru%2Fapi%2Fcommon%2FdownloadFile%2Fe51d4be907e69224ce34f5075af1e3f9"
                + "&lr=2&mime=pdf&l10n=ru&sign=8fa63eba0dd41e63b04c751fa7e38b59&keyno=0";
        Assert.isTrue(serpUrlChecker.verify(params));

        params = "name=e51d4be907e69224ce34f5075af1e3f9&tm=1511035479&tld=ru&text=mime%3Apdf"
                + "&url=http%3A%2F%2Fwww.gpmradio.ru%2Fapi%2Fcommon%2FdownloadFile%2Fe51d4be907e69224ce34f5075af1e3f9"
                + "&lr=2&mime=pdf&l10n=ru&sign=8fa63eba0dd41e63b04c751fa7e38b59&keyno=0";
        Assert.isFalse(serpUrlChecker.verify(params));
    }

    @Test
    public void verifyWithExtraParams() {
        String params = "lang=ru&name=nir_440304_po_28112016.docx%3Fver%3D2016-11-29-214328-503&tm=1581742088&tld=ru"
                + "&text=%D0%A8%D0%B0%D1%82%D1%8B%D1%80%20%D0%AE.%D0%90.%20%D0%93%D0%B5%D0%BD%D0%B4%D0%B5%D1%80%D0%BD%"
                + "D1%8B%D0%B5%20%D0%BE%D1%81%D0%BE%D0%B1%D0%B5%D0%BD%D0%BD%D0%BE%D1%81%D1%82%D0%B8%20%D0%BF%D1%80%D0%"
                + "BE%D1%8F%D0%B2%D0%BB%D0%B5%D0%BD%D0%B8%D1%8F%20%D0%BF%D0%BE%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D1%87%D0%"
                + "B5%D1%81%D0%BA%D0%BE%D0%B9%20%D0%B8%20%D1%81%D0%BE%D1%86%D0%B8%D0%B0%D0%BB%D1%8C%D0%BD%D0%BE%D0%B9%"
                + "20%D0%B0%D0%BA%D1%82%D0%B8%D0%B2%D0%BD%D0%BE%D1%81%D1%82%D0%B8%20%D1%87%D0%B5%D0%BB%D0%BE%D0%B2%D0%"
                + "B5%D0%BA%D0%B0%20%D0%B2%20%D0%B7%D0%B0%D0%B2%D0%B8%D1%81%D0%B8%D0%BC%D0%BE%D1%81%D1%82%D0%B8%20%D0%"
                + "BE%D1%82%20%D0%B8%D0%BD%D0%B4%D0%B8%D0%B2%D0%B8%D0%B4%D1%83%D0%B0%D0%BB%D1%8C%D0%BD%D0%BE%D0%B3%D0%"
                + "BE%20%D1%83%D1%80%D0%BE%D0%B2%D0%BD%D1%8F&url=http%3A%2F%2Fwww.volgau.com%2FPortals%2F0%2Feducation"
                + "%2Fitf%2F440304_po%2Fnir_440304_po_28112016.docx%3Fver%3D2016-11-29-214328-503&lr=38&mime=docx"
                + "&l10n=ru&sign=19622a3d5c8ed1facd3ba7ca400b96e8&keyno=0";
        Assert.isTrue(serpUrlChecker.verify(params));
        params += "&nosw=1";
        Assert.isTrue(serpUrlChecker.verify(params));
    }
}
