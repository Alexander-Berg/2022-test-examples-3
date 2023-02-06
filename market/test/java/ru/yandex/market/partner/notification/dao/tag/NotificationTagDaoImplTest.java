package ru.yandex.market.partner.notification.dao.tag;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.service.tag.model.NotificationTags;

import java.util.List;
import java.util.Map;

public class NotificationTagDaoImplTest extends AbstractFunctionalTest {
    private static final Map<String, String> TAG_SET_1 = Map.of("tag1", "value1", "tag2", "value2");
    private static final Map<String, String> TAG_SET_2 = Map.of("tag12", "value12", "tag22", "value22");

    @Autowired
    NotificationTagDao notificationTagDao;

    @Test
    @DbUnitDataSet(
            before = "NotificationTagDaoImpl/saveNotificationTags.before.csv",
            after = "NotificationTagDaoImpl/saveNotificationTags.after.csv"
    )
    public void shouldSaveNotificationTags() {
        NotificationTags data = new NotificationTags(TAG_SET_1,"testScope", true);
        notificationTagDao.saveNotificationFilters(0, data);
    }

    @Test
    @DbUnitDataSet(
            before = "NotificationTagDaoImpl/loadNotificationTags.before.csv",
            after = "NotificationTagDaoImpl/loadNotificationTags.after.csv"
    )
    public void shouldLoadNotificationTags() {
        List<NotificationTags> data = notificationTagDao.getNotificationFilters(0);
        org.junit.jupiter.api.Assertions.assertEquals(2, data.size());
        Assertions.assertThat(data).isEqualTo(
                List.of(new NotificationTags(TAG_SET_1, "testScope",true),
                        new NotificationTags(TAG_SET_2, "testScope2", false)
                )
        );
    }

}
