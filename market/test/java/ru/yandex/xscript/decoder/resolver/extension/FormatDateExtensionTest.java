package ru.yandex.xscript.decoder.resolver.extension;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DateTimeValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static net.sf.saxon.value.SequenceType.OPTIONAL_DATE_TIME;
import static net.sf.saxon.value.SequenceType.OPTIONAL_STRING;
import static net.sf.saxon.value.SequenceType.SINGLE_STRING;

/**
 * @author prediger
 * @date 21.08.18
 */
public class FormatDateExtensionTest {
    private FormatDateExtension extension;

    @Before
    public void setUp() {
        extension = new FormatDateExtension();
    }

    @Test
    public void argumentTypes() {
        Assert.assertArrayEquals(new SequenceType[]{OPTIONAL_DATE_TIME, SINGLE_STRING, OPTIONAL_STRING},
                extension.getArgumentTypes());
    }

    @Test
    public void resultType() {
        Assert.assertEquals(SequenceType.SINGLE_STRING,
                extension.getResultType(new SequenceType[]{OPTIONAL_DATE_TIME, SINGLE_STRING, OPTIONAL_STRING}));
    }

    @Test
    public void yearPatternIsCorrect() throws XPathException {
        ZonedDateTime currentSystemTime = ZonedDateTime.now();
        Sequence result = extension.makeCallExpression()
                .call(new XPathContextMajor(new Controller(new Configuration())),
                        new Sequence[]{new ZeroOrOne<>(
                                new DateTimeValue(currentSystemTime.getYear(), (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, 0, false)),
                                new StringValue("%yyyy")});

        DateTimeFormatter testFormatter = DateTimeFormatter.ofPattern("yyyy");
        String formattedSystemTime = currentSystemTime.format(testFormatter);
        String res = result.head().getStringValue();

        Assert.assertEquals(formattedSystemTime, res);
    }

}
