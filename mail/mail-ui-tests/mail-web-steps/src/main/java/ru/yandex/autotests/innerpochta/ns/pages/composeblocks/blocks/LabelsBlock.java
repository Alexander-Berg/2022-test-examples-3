package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 27.05.15.
 */
public interface LabelsBlock extends MailElement{

    @Name("Список проставленных меток")
    @FindByCss(".mail-Label")
    ElementsCollection<SampleLabelBlock> mailLabels();

}
