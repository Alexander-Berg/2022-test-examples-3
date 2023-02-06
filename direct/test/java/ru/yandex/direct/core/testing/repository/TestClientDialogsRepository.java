package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.model.Dialog;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CLIENT_DIALOGS;
import static ru.yandex.direct.dbschema.ppc.tables.CampDialogs.CAMP_DIALOGS;

public class TestClientDialogsRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestClientDialogsRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }


    /**
     * Удаляем привязку диалога к кампании и сам диалог
     */
    public void deleteDialog(int shard, Long campaignId, Dialog dialog) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CAMP_DIALOGS)
                .where(CAMP_DIALOGS.CID.eq(campaignId))
                .execute();
        dslContextProvider.ppc(shard)
                .deleteFrom(CLIENT_DIALOGS)
                .where(CLIENT_DIALOGS.CLIENT_DIALOG_ID.eq(dialog.getId()))
                .execute();
    }


}
