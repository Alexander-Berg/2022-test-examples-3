import org.junit.Assert;
import org.junit.Test;
import ru.yandex.Utils;

/**
 * @author elwood
 */
public class UtilsTest {
    @Test
    public void testAddLinkUrlArgs() {
        Assert.assertEquals("http://example.com/dir/?yaclid=0000000000", Utils.addLinkUrlArgs("http://example.com/dir/", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/?yaclid=0000000000&arg=val", Utils.addLinkUrlArgs("http://example.com/?arg=val", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/?yaclid=0000000000&arg1=val1&arg2=val2", Utils.addLinkUrlArgs("http://example.com/?arg1=val1&arg2=val2", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/page.html?yaclid=0000000000&arg=val", Utils.addLinkUrlArgs("http://example.com/page.html?arg=val", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/dir/?yaclid=0000000000#anchor", Utils.addLinkUrlArgs("http://example.com/dir/#anchor", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/dir/?yaclid=0000000000&arg=val#anchor", Utils.addLinkUrlArgs("http://example.com/dir/?arg=val#anchor", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/?yaclid=0000000000#anchor", Utils.addLinkUrlArgs("http://example.com/?#anchor", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/?yaclid=0000000000#anchor", Utils.addLinkUrlArgs("http://example.com/#anchor", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/?yaclid=0000000000&val=privet!%20Medved", Utils.addLinkUrlArgs("http://example.com/?val=privet!%20Medved", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/?yaclid=0000000000&val=privet!%20Medved#anckor%20with%20space", Utils.addLinkUrlArgs("http://example.com/?val=privet!%20Medved#anckor%20with%20space", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/?yaclid=0000000000&val=privet!%20Medved#anckor%20with%20double%20%20space", Utils.addLinkUrlArgs("http://example.com/?val=privet!%20Medved#anckor%20with%20double%20%20space", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/?yaclid=0000000000#anckor%20with%20space", Utils.addLinkUrlArgs("http://example.com/?#anckor%20with%20space", "yaclid=0000000000"));
        Assert.assertEquals("http://example.com/?yaclid=0000000000#anckor%20with%20double%20%20space", Utils.addLinkUrlArgs("http://example.com/?#anckor%20with%20double%20%20space", "yaclid=0000000000"));
        Assert.assertEquals("http://xn--80aswg.xn--p1ai/?yaclid=0000000000", Utils.addLinkUrlArgs("http://сайт.рф/", "yaclid=0000000000"));
        Assert.assertEquals("http://xn--80aswg.xn--p1ai?yaclid=0000000000", Utils.addLinkUrlArgs("http://xn--80aswg.xn--p1ai", "yaclid=0000000000"));
        Assert.assertEquals("http://xn--90acixgjdx4dxbyb.xn--p1ai/?yaclid=0000000000", Utils.addLinkUrlArgs("http://xn--90acixgjdx4dxbyb.xn--p1ai/", "yaclid=0000000000"));
        Assert.assertEquals("//xn--90acixgjdx4dxbyb.xn--p1ai/?yaclid=0000000000", Utils.addLinkUrlArgs("//xn--90acixgjdx4dxbyb.xn--p1ai/", "yaclid=0000000000"));
    }
}
