/**
 * Date: 18.07.2007
 * Time: 13:29:51
 */

package ru.yandex.market.admin.ui.model.testing;

import ru.yandex.market.admin.ui.model.MapUIDataModel;
import ru.yandex.market.admin.ui.model.StringID;

/**
 * @author Kasumov Makhmud mkasumov@yandex-team.ru
 */
public class UIShopTestingHistoryItem extends MapUIDataModel {

    public static final StringID DATE = new StringID("DATE");
    public static final StringID CHANGES = new StringID("CHANGES");
    public static final StringID ACTOR_ID = new StringID("ACTOR_ID");
    public static final StringID ACTOR_NAME = new StringID("ACTOR_NAME");
    public static final StringID COMMENT = new StringID("COMMENT");

    protected MapUIDataModel newInstance() {
        return new UIShopTestingHistoryItem();
    }
}
