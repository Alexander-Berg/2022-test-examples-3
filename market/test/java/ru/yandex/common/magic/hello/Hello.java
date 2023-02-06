/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 17.02.2007
 * Time: 16:25:51
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.common.magic.hello;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * @author ashevenkov
 */
public interface Hello extends RemoteService {

    String sayHello(String name);

    void callHello(String name);

}
