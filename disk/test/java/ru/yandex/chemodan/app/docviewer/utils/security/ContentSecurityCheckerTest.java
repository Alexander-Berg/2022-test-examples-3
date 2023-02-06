package ru.yandex.chemodan.app.docviewer.utils.security;

import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.MimeTypes;
import ru.yandex.chemodan.app.docviewer.states.ErrorCode;
import ru.yandex.chemodan.app.docviewer.states.UserException;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class ContentSecurityCheckerTest {
    @Test
    public void checkContent() {
        InputStreamSource source = new ClassPathResourceInputStreamSource(this.getClass(), "evil.ods");
        try {
            ContentSecurityChecker.CHECKER.checkContent(MimeTypes.MIME_OPENDOCUMENT_SPREADSHEET, source);
            Assert.fail("Checker should throw exception, that file is non-secure");
        } catch (UserException ue) {
            Assert.equals(ErrorCode.UNSUPPORTED_CONVERTION, ue.getErrorCode());
        }
    }

    @Test
    public void isNonSecureFormula() {
        ContentSecurityChecker checker = new ContentSecurityChecker();

        System.out.println(StringEscapeUtils.unescapeHtml4("&#119;"));
        Assert.isFalse(checker.isNonSecureFormula("SUM"));
        Assert.isTrue(checker.isNonSecureFormula("COM.MICROSOFT.WEBSERVICE"));
        Assert.isTrue(checker.isNonSecureFormula("COM.MICROSOFT.webservice"));
        Assert.isTrue(checker.isNonSecureFormula("COM.MICROSOFT.wEbSerVice"));
        Assert.isTrue(checker.isNonSecureFormula("COM.MICROSOFT.&#119;EbSerVice"));
    }
}
