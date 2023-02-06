package ru.yandex.xscript.decoder.resolver.extension;

import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ayratgdl
 * @date 06.08.18
 */
public class XmlEscapeExtensionTest {
    private XmlEscapeExtension extension;

    @Before
    public void setUp() {
        extension = new XmlEscapeExtension();
    }

    @Test
    public void getArgumentTypesAreOptionString() {
        Assert.assertArrayEquals(new SequenceType[] {SequenceType.OPTIONAL_STRING}, extension.getArgumentTypes());
    }

    @Test
    public void getResultTypeIsOptionalString() {
        Assert.assertEquals(SequenceType.OPTIONAL_STRING, extension.getResultType(null));
    }

    @Test
    public void escapeSimpleSymbols() throws XPathException {
        Sequence result = extension.makeCallExpression().call(null, new Sequence[]{new StringValue("abc")});
        Assert.assertEquals(new StringValue("abc").getStringValue(), ((StringValue) result).getStringValue());
    }

    @Test
    public void escapeXmlSymbols() throws XPathException {
        Sequence result = extension.makeCallExpression().call(null, new Sequence[]{new StringValue("a>b&c")});
        Assert.assertEquals(new StringValue("a&gt;b&amp;c").getStringValue(), ((StringValue) result).getStringValue());
    }

    @Test
    public void escapeEmptyString() throws XPathException {
        Sequence result = extension.makeCallExpression().call(null, new Sequence[]{StringValue.EMPTY_STRING});
        Assert.assertEquals(StringValue.EMPTY_STRING.getStringValue(), ((StringValue) result).getStringValue());
    }
}
