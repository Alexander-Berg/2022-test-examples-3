package ru.yandex.common.magic.hello;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface HelloAsync {
    void sayHello(String name, AsyncCallback async);

    void callHello(String name, AsyncCallback async);
}
