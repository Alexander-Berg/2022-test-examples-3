/**
 * Date: 15.08.2007
 * Time: 15:41:28
 */

package ru.yandex.market.admin.ui.model.testing;

import ru.yandex.market.admin.ui.model.StringID;

/**
 * @author Kasumov Makhmud mkasumov@yandex-team.ru
 */
public class UIShopTestingTypeFilter extends StringID {

    public static final UIShopTestingTypeFilter ALL = new UIShopTestingTypeFilter("ALL");
    public static final UIShopTestingTypeFilter OLD = new UIShopTestingTypeFilter("OLD");
    public static final UIShopTestingTypeFilter NEW = new UIShopTestingTypeFilter("NEW");
    public static final UIShopTestingTypeFilter DELAYED = new UIShopTestingTypeFilter("DELAYED");

    public UIShopTestingTypeFilter() {
    }

    public UIShopTestingTypeFilter(String id) {
        super(id);
    }
}
