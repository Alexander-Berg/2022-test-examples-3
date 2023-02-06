package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import ru.yandex.autotests.direct.httpclient.util.requestbeantojson.RequestBeanToJsonProcessor;

/**
 * Created by shmykov on 28.04.15.
 * Суперкласс для бинов, которые нужно преобразовывать в json строку
 */
public abstract class JsonStringTransformableCmdBean {

    public String toJson() {
        return RequestBeanToJsonProcessor.toJson(this);
    }
}
