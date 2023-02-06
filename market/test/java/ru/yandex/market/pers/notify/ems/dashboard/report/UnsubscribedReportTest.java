package ru.yandex.market.pers.notify.ems.dashboard.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.pers.notify.api.service.sk.SecretKeyManager;
import ru.yandex.market.pers.notify.mail.dashboard.report.UnsubscribedReport;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.sk.SecretKeyData;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;
import ru.yandex.market.statface.StatfaceData;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pers.notify.mail.dashboard.StatfaceRowName.COUNT_MEASURE;


/**
 * @author artemmz
 *         created on 09.06.16.
 */
public class UnsubscribedReportTest extends MarketMailerMockedDbTest {
    @Autowired
    UnsubscribedReport unsubscribedReport;
    @Autowired
    SecretKeyManager secretKeyManager;

    @Test
    public void testGetData() throws IOException {
        Date now = new Date();
        Pair<Long, String> authUser = Pair.of(RND.nextLong(), "foo@bar.com");
        Pair<Long, String> noAuthUser = Pair.of(null, "clazz@bazz.com");
        // добавим дубликатов, не должны учитываться
        for (Pair<Long, String> user : List.of(authUser, authUser, authUser, noAuthUser, authUser, noAuthUser)) {
            for (NotificationType type : NotificationType.values()) {
                SecretKeyData data = new SecretKeyData().addEmail(user.getRight()).addUid(user.getLeft()).addType(type);
                secretKeyManager.saveStat(data);
            }
        }
        StatfaceData data = unsubscribedReport.getData(DateUtil.addDay(now, -1), DateUtil.addDay(now, 1));
        assertEquals(data.getRowCount(), NotificationType.values().length);
        List<LinkedHashMap> rows = (List<LinkedHashMap>) new ObjectMapper().readValue(data.toJson(), LinkedHashMap.class).get("values");
        rows.forEach(row -> assertEquals(2.0, row.get(COUNT_MEASURE)));
    }
}
