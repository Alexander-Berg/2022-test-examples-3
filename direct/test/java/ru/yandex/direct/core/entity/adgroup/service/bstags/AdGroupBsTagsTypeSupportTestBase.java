package ru.yandex.direct.core.entity.adgroup.service.bstags;

import java.util.List;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

public class AdGroupBsTagsTypeSupportTestBase {

    @Autowired
    protected AdGroupBsTagsSettingsProvider bsTagsSettingsProvider;

    @Autowired
    protected Steps steps;

    protected ClientInfo client;

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClient();
    }

    protected AdGroupBsTagsSettings getAdGroupBsTagsSettings(AdGroup adGroup) {
        return bsTagsSettingsProvider.getAdGroupBsTagsSettings(List.of(adGroup), client.getClientId())
                .get(adGroup);
    }
}
