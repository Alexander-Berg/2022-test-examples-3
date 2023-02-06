/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 17.02.2007
 * Time: 16:26:27
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.common.magic.hello;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ru.yandex.common.magic.service.MagicRemoteServiceServlet;

/**
 * @author ashevenkov
 */
public class DefaultHelloService extends MagicRemoteServiceServlet implements Hello {

    public String sayHello(String name) {
        return "Hello my dear friend, " + name;
    }

    public void callHello(String name) {
        try {
            Method method = Hello.class.getMethod("sayHello", String.class);
            method.invoke(this, name);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
