package ru.yandex.travel.webdriver;


/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */

import org.aeonbits.owner.Config;

@Config.Sources("classpath:webdriver.properties")
public interface WebDriverConfig extends Config{

    @Key("webdriver.local")
    @DefaultValue("false")
    boolean isLocal();

    @DefaultValue("aqua")
    @Key("webdriver.remote.username")
    String getRemoteUsername();

    @DefaultValue("407a4ec42ce16bf67926696ecb91b847")
    @Key("webdriver.remote.password")
    String getRemotePassword();

    @DefaultValue("sg.yandex-team.ru")
    String getRemoteHost();

    @DefaultValue("4444")
    @Key("webdriver.remote.port")
    int getRemotePort();

    @Key("webdriver.browser.name")
    @DefaultValue("chrome")
    String getBrowserName();

    @Key("webdriver.browser.version")
    String getBrowserVersion();

}
