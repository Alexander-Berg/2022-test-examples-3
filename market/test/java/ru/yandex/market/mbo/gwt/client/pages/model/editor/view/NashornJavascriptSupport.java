package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.db.rules.NashornJsExecutor;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.JsSupport;

import java.util.HashSet;
import java.util.Set;

/**
 * В тестах для эмуляции js-слоя используется java-движок Nashorn.
 *
 * @author gilmulla
 */
public class NashornJavascriptSupport extends NashornJsExecutor implements JsSupport {
    private Set<Long> triggerParams = new HashSet<>();

    public Set<Long> getTriggerParams() {
        return triggerParams;
    }

    @Override
    public void setTriggerParameters(Set<Long> triggerParameters) {
        this.triggerParams = triggerParameters;
    }

    @Override
    public void initJsValueFunction() {

    }
}
