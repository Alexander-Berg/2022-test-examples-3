package ru.yandex.xscript.decoder.resolver.extension;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.SequenceType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author prediger
 * @date 21.08.18
 */
public class PowerExtensionTest {
    private PowerExtension extension;

    @Before
    public void setUp() {
        extension = new PowerExtension();
    }

    @Test
    public void argumentTypes() {
        Assert.assertArrayEquals(new SequenceType[]{SequenceType.SINGLE_NUMERIC, SequenceType.SINGLE_NUMERIC},
                extension.getArgumentTypes());
    }

    @Test
    public void resultType() {
        Assert.assertEquals(SequenceType.SINGLE_NUMERIC,
                extension.getResultType(new SequenceType[]{SequenceType.SINGLE_NUMERIC, SequenceType.SINGLE_NUMERIC}));
    }

    @Test
    public void resultDateTimeIsCloseToSystemDateTime() throws XPathException {
        Sequence result = extension.makeCallExpression()
                .call(new XPathContextMajor(new Controller(new Configuration())),
                        new Sequence[]{new DoubleValue(3), new DoubleValue(3)});
        Assert.assertEquals(((NumericValue) result.head()).getDoubleValue(), 27.0, 0);

    }

}
