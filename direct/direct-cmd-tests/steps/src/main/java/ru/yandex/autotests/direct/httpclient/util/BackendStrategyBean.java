package ru.yandex.autotests.direct.httpclient.util;

import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.utils.beans.MongoBeanLoader;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;

/**
 * Created by aleran on 26.10.2015.
 */
public class BackendStrategyBean {

    private static final String TEMPLATE_COLLECTION_NAME = "DirectHttpTemplates";
    private static final String TEMPLATE_PREFIX = "strategy";

    private static String getTemplateName(Strategies strategies) {
        String tmplName = "";
        //convert TEMPLATE_NAME to templateName;
        for (String word : strategies.name().split("_")) {
            tmplName += word.substring(0, 1);
            tmplName += word.substring(1).toLowerCase();
        }
        tmplName = TEMPLATE_PREFIX + tmplName;
        return tmplName;
    }

    public static CampaignStrategy getStrategyBean(Strategies strategies) {
        MongoBeanLoader<CampaignStrategy> loader = new MongoBeanLoader<>(CampaignStrategy.class, TEMPLATE_COLLECTION_NAME);
        return loader.getBean(getTemplateName(strategies));
    }
}
