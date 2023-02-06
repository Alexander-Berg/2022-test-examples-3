package ru.yandex.market.crm.campaign.test.utils;

import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.yt.client.YtClient;

/**
 * @author apershukov
 */
@Component
public class BannedPromocodesTestHelper {

    public static YTreeMapNode promocodeRecord(long promoId, @Nullable Long puid, @Nullable String uuid) {
        var builder = YTree.mapBuilder()
                .key("promo_id").value(promoId);

        if (puid != null) {
            builder.key("puid").value(puid);
        }

        if (uuid != null) {
            builder.key("uuid").value(uuid);
        }

        return builder.buildMap();
    }

    private final YtSchemaTestHelper ytSchemaTestHelper;
    private final YtClient ytClient;
    private final YPath bannedPromocodesTable;

    public BannedPromocodesTestHelper(YtSchemaTestHelper ytSchemaTestHelper,
                                      YtClient ytClient,
                                      @Value("${var.banned_promocodes_table}") String bannedPromocodesTable) {
        this.ytSchemaTestHelper = ytSchemaTestHelper;
        this.ytClient = ytClient;
        this.bannedPromocodesTable = YPath.simple(bannedPromocodesTable);
    }

    public void prepareBannedTable(YTreeMapNode... rows) {
        ytSchemaTestHelper.createTable(bannedPromocodesTable, "banned_promocodes.yson");
        ytClient.write(bannedPromocodesTable, YTableEntryTypes.YSON, List.of(rows));
    }
}
