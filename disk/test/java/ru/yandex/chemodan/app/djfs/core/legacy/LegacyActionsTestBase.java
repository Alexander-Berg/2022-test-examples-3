package ru.yandex.chemodan.app.djfs.core.legacy;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.djfs.core.client.LogReaderHttpClient;
import ru.yandex.chemodan.app.djfs.core.db.mongo.DjfsBenderFactory;
import ru.yandex.chemodan.app.djfs.core.filesystem.SupportBlockedHidsDao;
import ru.yandex.chemodan.app.djfs.core.legacy.formatting.ResourcePojo;
import ru.yandex.chemodan.app.djfs.core.legacy.formatting.ResourcePojoBuilder;
import ru.yandex.chemodan.app.djfs.core.test.DjfsDoubleUserWithSharedResourcesTestBase;
import ru.yandex.chemodan.app.djfs.core.web.JsonStringResult;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.misc.bender.parse.BenderJsonParser;

public abstract class LegacyActionsTestBase extends DjfsDoubleUserWithSharedResourcesTestBase {
    @Autowired
    SupportBlockedHidsDao supportBlockedHidsDao;

    @Autowired
    Blackbox2 blackbox;

    @Autowired
    ResourcePojoBuilder resourcePojoBuilder;

    @Autowired
    LogReaderHttpClient logReaderHttpClient;

    @Autowired
    LegacyFilesystemActions legacyFilesystemActions;

    private final BenderJsonParser<ResourcePojo> resourcePojoParser =
            DjfsBenderFactory.createForJson(ResourcePojo.class).getParser();

    ListF<ResourcePojo> parseJsonListStringResult(JsonStringResult jsonStringResult) {
        return resourcePojoParser.parseListJson(jsonStringResult.getResult());
    }
}
