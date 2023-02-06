package ru.yandex.chemodan.uploader.web;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class YaDiskUserAgentTest {

    @Test
    public void parserTest1() {
        String userAgent =
                "{\"os\":\"iOS\",\"src\":\"disk.mobile\",\"vsn\":\"1.70.2095\"" +
                ",\"id\":\"3ED4FEAF-CF03-4DB4-8BD5-93CA1337C758\",\"device\":\"phone\"}";
        YaDiskUserAgent ua = YaDiskUserAgent.PS.getParser().parseJson(userAgent);

        Assert.equals("iOS", ua.os);
        Assert.equals("1.70.2095", ua.version);
        Assert.equals("3ED4FEAF-CF03-4DB4-8BD5-93CA1337C758", ua.id);
        Assert.some("phone", ua.device);
        Assert.some("disk.mobile", ua.source);
    }

    @Test
    public void parserTest2() {
        String userAgent = "{\"os\":\"windows\",\"vsn\":\"1.4.3.4879\",\"id\":\"771920c2120d4e05a826be60130dda63\"}";
        YaDiskUserAgent ua = YaDiskUserAgent.PS.getParser().parseJson(userAgent);

        Assert.equals("windows", ua.os);
        Assert.equals("1.4.3.4879", ua.version);
        Assert.equals("771920c2120d4e05a826be60130dda63", ua.id);
        Assert.none(ua.device);
        Assert.none(ua.source);
    }

    @Test
    public void parseNotYaDiskUserAgent() {
        Assert.none(YaDiskUserAgent.parse("Mozilla/5.0"));
        Assert.none(YaDiskUserAgent.parse("Yandex.Disk "));
    }

    @Test
    public void parseFullYaDiskUserAgent() {
        String userAgent = "Yandex.WebDav 0.5.9: Yandex.Disk " +
                "{\"os\":\"mac\",\"vsn\":\"1.4.1.5675\",\"id\":\"8443936799900187441\"}";
        Option<YaDiskUserAgent> ua = YaDiskUserAgent.parse(userAgent);
        Assert.some(ua);
        Assert.equals("mac", ua.get().os);
    }
}
