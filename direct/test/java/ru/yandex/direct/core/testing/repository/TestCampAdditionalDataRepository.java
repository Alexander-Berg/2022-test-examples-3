package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_ADDITIONAL_DATA;

public class TestCampAdditionalDataRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    public void addHref(int shard, Long cid, String href) {
        dslContextProvider.ppc(shard)
                .insertInto(CAMP_ADDITIONAL_DATA)
                .set(CAMP_ADDITIONAL_DATA.CID, cid)
                .set(CAMP_ADDITIONAL_DATA.HREF, href)
                .onDuplicateKeyUpdate()
                .set(CAMP_ADDITIONAL_DATA.HREF, href)
                .execute();
    }
}
