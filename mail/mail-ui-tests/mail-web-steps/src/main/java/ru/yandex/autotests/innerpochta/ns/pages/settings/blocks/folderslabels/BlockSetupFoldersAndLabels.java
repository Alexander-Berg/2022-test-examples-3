package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 18.09.12
 * Time: 17:08
 */

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockSetupFoldersAndLabels extends MailElement {

    @Name("Блок «Папки»")
    @FindByCss(".b-manager:nth-of-type(1)")
    BlockFolders folders();

    @Name("Блок «Метки»")
    @FindByCss(".b-manager+div")
    BlockLabels labels();
}
