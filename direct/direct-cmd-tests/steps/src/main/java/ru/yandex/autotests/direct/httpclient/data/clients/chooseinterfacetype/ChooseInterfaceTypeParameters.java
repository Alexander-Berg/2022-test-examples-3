package ru.yandex.autotests.direct.httpclient.data.clients.chooseinterfacetype;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 03.02.15
 */
public class ChooseInterfaceTypeParameters extends BasicDirectFormParameters {

    public ChooseInterfaceTypeParameters() {}

    public ChooseInterfaceTypeParameters(String to) {
        this.to = to;
    }

    @FormParameter("to")
    private String to;

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
