package ru.yandex.autotests.market.stat.dictionaries_yt.steps;

import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionariesJob;
import ru.yandex.autotests.market.stat.beans.Packages;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionariesConsole;
import ru.yandex.autotests.market.stat.meta.TmsDaoFactory;
import ru.yandex.autotests.market.stat.steps.GeneralTmsSteps;

import java.util.Collections;

/**
 * Created by kateleb on 23.05.17.
 */
public class DictionariesYtTmsSteps extends GeneralTmsSteps<DictionariesJob> {
    public DictionariesYtTmsSteps() {
        super(TmsDaoFactory.getTmsDaoForDictionariesYt(), Collections.singletonList(Packages.DICTIONARIES_YT),
                DictionariesConsole.startSessionForYt());
    }
}
