/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 23.03.2007
 * Time: 17:02:34
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.common.magic.aaa;

/**
 * @author ashevenkov
 */
public class AInfo {

    private long id;

    public AInfo() {
    }

    public AInfo(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
