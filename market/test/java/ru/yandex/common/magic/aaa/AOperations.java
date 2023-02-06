/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 23.03.2007
 * Time: 16:57:14
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.common.magic.aaa;

import java.util.Set;

/**
 * @author ashevenkov
 */
public interface AOperations {

    void setId(long id);
    long getId();

    Set<? extends AOperations> getChilds();
    void setChilds(Set<? extends AOperations> operationses);

}
