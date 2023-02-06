/**
 * Date: 16.08.2007
 * Time: 17:37:35
 */

package ru.yandex.market.admin.ui.client.shop.testing;

import ru.yandex.market.admin.ui.client.data.DataFieldType;
import ru.yandex.market.admin.ui.client.data.SimpleDataBlock;
import ru.yandex.market.admin.ui.model.testing.UITestedShop;

/**
 * @author Kasumov Makhmud mkasumov@yandex-team.ru
 */
public class ShopTestingDataBlock extends SimpleDataBlock {

    public ShopTestingDataBlock() {
        addField(UITestedShop.STATUS, DataFieldType.TEXT, true, false);
        addField(UITestedShop.TEST_LOADING, DataFieldType.TEXT, true, false);
        addField(UITestedShop.TEST_QUALITY, DataFieldType.TEXT, true, false);
        addField(UITestedShop.TEST_CLONING, DataFieldType.TEXT, true, false);

    }

}
