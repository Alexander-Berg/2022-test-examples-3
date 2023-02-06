package ru.yandex.autotests.market.stat.dictionaries_yt.dao;

import org.springframework.context.annotation.Configuration;
import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;

/**
 * Created by kateleb on 02.06.17.
 */
@Configuration
@Resource.Classpath("yt-dictionaries.properties")
public class YTDictionariesConfig {

    @Property("yt.dictionaries.username")
    private String username = "robot-mrkt-ci";
    @Property("yql.dictionaries.token")
    private String yqlToken = System.getenv("MARKET_CI_YT_TOKEN");
    @Property("yt.dictionaries.token")
    private String ytToken = System.getenv("MARKET_CI_YT_TOKEN");
    @Property("yt.dictionaries.path")
    private String ytRelativePath = "/hahn/home/market/prestable/mstat/dictionaries";
    @Property("yt.dictionaries.offers.path")
    private String ytOffersPath = "//home/market/production/mstat/dictionaries/market_offers_ch";
    @Property("yt.dictionaries.proxy")
    private String ytProxy = "hahn.yt.yandex.net";


    public String getYtToken() {
        return ytToken;
    }

    public String getYtPath() {
        return ytRelativePath.replace("hahn", "");
    }

    public String getYtOffersPath() {
        return ytOffersPath;
    }

    public String getYtProxy() {
        return ytProxy;
    }

    public YTDictionariesConfig() {
        PropertyLoader.populate(this);
    }

}
