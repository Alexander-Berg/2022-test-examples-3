package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.yandex.autotests.direct.httpclient.util.requestbeantojson.RequestBeanCustomNamingStrategy;
import ru.yandex.autotests.direct.httpclient.util.requestbeantojson.RequestBeanExclusionStrategy;

/**
 * Created by shmykov on 28.05.15.
 *  Билдер для случаев, когда в json в качестве имени поля используется динамическая строка(например, id)
 */
public class CmdBeanBuilder extends JsonStringTransformableCmdBean {

    protected GsonBuilder builder = new GsonBuilder();

    protected Gson gson = builder
            .setFieldNamingStrategy(new RequestBeanCustomNamingStrategy())
            .setExclusionStrategies(new RequestBeanExclusionStrategy())
            .create();
}
