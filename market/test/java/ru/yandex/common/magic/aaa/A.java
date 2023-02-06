/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 23.03.2007
 * Time: 16:55:31
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.common.magic.aaa;

import java.util.HashSet;
import java.util.Set;

/**
 * @author ashevenkov
 */
public class A implements AOperations {

    private Set<? extends AOperations> childs = new HashSet<A>();
    private long id;

    public Set<? extends AOperations> getChilds() {
        return childs;
    }

    public void setChilds(Set<? extends AOperations> childs) {
        this.childs = childs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
