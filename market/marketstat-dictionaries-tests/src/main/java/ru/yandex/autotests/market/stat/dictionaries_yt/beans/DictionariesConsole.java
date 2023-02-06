package ru.yandex.autotests.market.stat.dictionaries_yt.beans;

import ru.yandex.autotests.market.console.Console;
import ru.yandex.autotests.market.stat.console.AbstractTmsConsole;
import ru.yandex.autotests.market.stat.console.ConsoleConfig;
import ru.yandex.autotests.market.tmsconsole.TmsConsole;

/**
 * Created by entarrion on 20.09.16.
 */
public class DictionariesConsole extends AbstractTmsConsole<DictionariesJob> {
    private Console console;

    private DictionariesConsole(String host, int port, String defaultTriggersFile) {
        this.console = Console.startSessionAndClearInput(host, port);
        this.tmsConsole = TmsConsole.startSession(host, port, defaultTriggersFile);
    }

    public static DictionariesConsole startSessionForYt() {
        ConsoleConfig config = new ConsoleConfig();
        return new DictionariesConsole(config.getDictionariesYTConsoleHost(),
            config.getDictionariesYtConsolePort(),
            config.getDictionariesYtDefaultTriggersFile());
    }
}
