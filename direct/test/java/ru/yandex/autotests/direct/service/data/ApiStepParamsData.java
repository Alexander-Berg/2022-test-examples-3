package ru.yandex.autotests.direct.service.data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.yandex.direct.proxy.model.web.callstep.Parameter;

public class ApiStepParamsData {
    public static List<Parameter> defaultCreateDefaultCampaignParams() {
        return Collections
                .singletonList(new Parameter().withClazz("java.lang.String").withValue("\"at-direct-backend-c\""));
    }

    public static List<Parameter> defaultClientUpdate() {
        return Arrays
                .asList(new Parameter().withClazz("ru.yandex.autotests.directapi.model.api5.campaigns.AddRequestMap")
                                .withValue("{\"clients\" : [{\"ClientInfo\" : \"asd\"}]}"),
                        new Parameter().withClazz("java.lang.String").withValue("\"at-direct-backend-c\""));
    }

    public static List<Parameter> defaultGetShowCamp() {
        return Arrays.asList(new Parameter().withClazz("java.lang.String").withValue("\"at-direct-backend-c\""),
                new Parameter().withClazz("java.lang.String").withValue("146573163"));

    }

    public static List<Parameter> defaultCampaignAdd() {
        String yesterday = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String argument =
                "{\"campaigns\" : [{\"Name\": \"name\", \"StartDate\": \"" + yesterday + "\", \"TextCampaign\": {\"BiddingStrategy\":{\"Search\": {\"BiddingStrategyType\": \"HIGHEST_POSITION\"}, \"Network\": {\"BiddingStrategyType\": \"MAXIMUM_COVERAGE\"}}}}]}";
        return Arrays
                .asList(new Parameter().withClazz("ru.yandex.autotests.directapi.model.api5.campaigns.AddRequestMap")
                                .withValue(argument),
                        new Parameter().withClazz("java.lang.String").withValue("\"at-direct-backend-c\"")
                );

    }

}
