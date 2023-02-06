package ru.yandex.xscript.decoder.resolver.extension;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author prediger
 * @date 21.08.18
 */
public class DateTimeExtensionTest {
    private DateTimeExtension extension;

    @Before
    public void setUp() {
        extension = new DateTimeExtension();
    }

    @Test
    public void argumentTypes() {
        Assert.assertArrayEquals(new SequenceType[0], extension.getArgumentTypes());
    }

    @Test
    public void resultType() {
        Assert.assertEquals(SequenceType.OPTIONAL_DATE_TIME, extension.getResultType(new SequenceType[0]));
    }

    @Test
    public void resultDateTimeIsCloseToSystemDateTime() throws XPathException {
        Sequence result = extension.makeCallExpression()
                .call(new XPathContextMajor(new Controller(new Configuration())), new Sequence[0]);
        // picked this just to be sure that dates are close enough
        int secondsDiff = 3;
        ZonedDateTime currentSystemTime = ZonedDateTime.now();
        ZonedDateTime res = ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME
                .parse(((ZeroOrOne) result).getStringValue()));

        boolean closeEnough = currentSystemTime.plusSeconds(secondsDiff).isAfter(res) &&
                currentSystemTime.minusSeconds(secondsDiff).isBefore(res);
        Assert.assertTrue(closeEnough);

    }

}
