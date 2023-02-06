package ru.yandex.market.crm.core.test.utils;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.core.yt.paths.CrmYtTables;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.CrmInfo;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Email;

@Component
public class EmailFactsTestHelper {
    private static int factsCount = 0;

    public static YTreeMapNode emailFact(String email, SendingType sendingType, String sendingId) {
        CrmInfo crmInfo = CrmInfo.newBuilder()
                .setAccount("market")
                .setSendingId(sendingId)
                .build();
        return emailFact(email, sendingType, crmInfo);
    }

    public static YTreeMapNode emailFact(String email, SendingType sendingType, CrmInfo crmInfo) {
        long timestamp = ZonedDateTime.now()
                .toInstant()
                .toEpochMilli();
        return emailFact(email, sendingType, crmInfo, timestamp);
    }

    public static YTreeMapNode emailFact(String email, SendingType sendingType, CrmInfo crmInfo, long timestamp) {
        Email fact = Email.newBuilder()
                .setFactId(String.valueOf(++factsCount))
                .setSendingType(sendingType)
                .setDeliveryStatus(Email.DeliveryStatus.SENT)
                .setTimestamp(timestamp)
                .setCrmInfo(crmInfo)
                .setUid(Uids.create(UidType.EMAIL, email))
                .setOriginalUid(Uids.create(UidType.EMAIL, email))
                .build();

        return YTree.mapBuilder()
                .key("id").value(email)
                .key("id_type").value("email")
                .key("timestamp").value(timestamp)
                .key("fact_id").value(fact.getFactId())
                .key("fact").value(new YTreeStringNodeImpl(
                        fact.toByteArray(),
                        null
                ))
                .buildMap();
    }

    private final YtClient ytClient;
    private final CrmYtTables crmYtTables;

    public EmailFactsTestHelper(YtClient ytClient,
                                CrmYtTables crmYtTables) {
        this.ytClient = ytClient;
        this.crmYtTables = crmYtTables;
    }

    public void prepareEmailsFacts(YTreeMapNode... rows) {
        ytClient.write(
                crmYtTables.getEmailTable(),
                List.of(rows)
        );
    }
}
