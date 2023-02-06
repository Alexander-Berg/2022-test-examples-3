package ru.yandex.autotests.innerpochta.atlas;

import io.qameta.atlas.webdriver.AtlasWebElement;
import ru.yandex.autotests.innerpochta.atlas.extensions.HoverMethodExtension;

/**
 * @author eremin-n-s
 */
public interface MailElement extends AtlasWebElement<MailElement> {

    /**
     * This method handled by the {@link HoverMethodExtension}.
     */
    MailElement hover();

    default void setChecked(boolean state) {
        if(isSelected() != state){
            click();
        }
    }

}
