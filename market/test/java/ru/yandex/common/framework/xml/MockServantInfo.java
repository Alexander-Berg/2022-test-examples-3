/**
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Yandex</p>
 * <p>Date: 08.06.2006</p>
 * <p>Time: 18:29:15</p>
 */
package ru.yandex.common.framework.xml;

import ru.yandex.common.framework.core.ServantInfo;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
class MockServantInfo implements ServantInfo {
    public String getName() {
        return "test";
    }

    public String getVersion() {
        return "-1";
    }

    public String getHostName() {
        return "localhost";
    }
}
