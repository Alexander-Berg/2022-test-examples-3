/**
 * Date: 11.07.2007
 * Time: 15:43:52
 */

package ru.yandex.market.admin.ui.model.testing;

import ru.yandex.market.admin.ui.model.MapUIDataModel;
import ru.yandex.market.admin.ui.model.StringID;

/**
 * @author Kasumov Makhmud mkasumov@yandex-team.ru
 */
public class UITestedShop extends MapUIDataModel {

    /**
     * Идентификатор магазина.
     */
    public static final StringID ID = new StringID("ID");
    public static final StringID NAME = new StringID("NAME");
    public static final StringID READY = new StringID("READY");
    public static final StringID APPROVRED = new StringID("APPROVRED");
    public static final StringID IN_PROGRESS = new StringID("IN_PROGRESS");
    public static final StringID CANCELLED = new StringID("CANCELLED");
    public static final StringID FATAL_CANCELLED = new StringID("FATAL_CANCELLED");
    public static final StringID STATUS = new StringID("STATUS");
    public static final StringID TEST_LOADING = new StringID("TEST_LOADING");
    public static final StringID TEST_QUALITY = new StringID("TEST_QUALITY");
    public static final StringID TEST_CLONING = new StringID("TEST_CLONING");
    public static final StringID DURATION = new StringID("DUR");
    public static final StringID ITERATION = new StringID("ITER");

    protected MapUIDataModel newInstance() {
        return new UITestedShop();
    }
}
