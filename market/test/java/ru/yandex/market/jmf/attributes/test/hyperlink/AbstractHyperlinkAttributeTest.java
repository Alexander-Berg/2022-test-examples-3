package ru.yandex.market.jmf.attributes.test.hyperlink;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.hyperlink.Hyperlink;

public abstract class AbstractHyperlinkAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return new Hyperlink(Randoms.url(), Randoms.string());
    }
}
