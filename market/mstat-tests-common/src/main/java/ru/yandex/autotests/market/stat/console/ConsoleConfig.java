package ru.yandex.autotests.market.stat.console;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;

/**
 * Created by entarrion on 06.04.15.
 */
@Resource.Classpath("console/console.properties")
public class ConsoleConfig {
    @Property("dictionaries.yt.console.port")
    private int dictionariesYtConsolePort = 12354;
    @Property("dictionaries.yt.console.host")
    private String dictionariesYTConsoleHost = "mstgate01ht.market.yandex.net";
    @Property("dictionaries.yt.default.triggers.file")
    private String dictionariesYtDefaultTriggersFile = "console/dictionariesDefaultTriggers.xml";

    public ConsoleConfig() {
        PropertyLoader.populate(this);
    }


    public int getDictionariesYtConsolePort() {
        return dictionariesYtConsolePort;
    }

    public String getDictionariesYTConsoleHost() {
        return dictionariesYTConsoleHost;
    }

    public String getDictionariesYtDefaultTriggersFile() {
        return dictionariesYtDefaultTriggersFile;
    }
}
