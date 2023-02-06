package ru.yandex.market.notifier.jobs.zk;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.storage.sql.DataSourceType;
import ru.yandex.market.checkout.storage.sql.MasterContextHolder;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.criteria.EvictionSearch;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.jobs.tms.NotificationEvictionJob;
import ru.yandex.market.notifier.storage.InboxDao;

public class MasterReplicasTest extends AbstractServicesTestBase {

    @Resource
    private NotificationEvictionJob notificationEvictionJob;

    @Autowired
    private InboxDao inboxDao;

    @Mock
    private SqlSessionTemplate mockTemplate;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @BeforeEach
    public void setup() {
        inboxDao.setSqlSessionTemplate(mockTemplate);
    }

    @AfterEach
    public void tearDown() {
        inboxDao.setSqlSessionTemplate(sqlSessionTemplate);
    }

    @Test
    @DisplayName("Проверяет, что запросы на чтение уходят на реплики, а запросы на модификацию на мастер")
    public void testEvictionJob() {
        List<Notification> notifications = new ArrayList<>();
        Notification notification = new Notification();
        notification.setId(12L);
        notification.addDeliveryChannel(new DeliveryChannel(ChannelType.EMAIL, "X@mail.ru", 1L));
        notifications.add(notification);

        Mockito.when(mockTemplate.<Notification>selectList(Mockito.anyString(), Mockito.any(EvictionSearch.class)))
                .then(invocation -> {
                    Assertions.assertEquals(DataSourceType.SLAVE, MasterContextHolder.getDatasourceType(),
                            "Select должен выполниться на реплике");
                    return notifications;
                });

        Mockito.when(mockTemplate.delete(Mockito.anyString(), Mockito.any(Notification.class)))
                .then(invocation -> {
                    Assertions.assertEquals(DataSourceType.MASTER, MasterContextHolder.getDatasourceType(),
                            "Delete должен выполниться на мастере");
                    return 1;
                });

        Mockito.when(mockTemplate.update(Mockito.anyString(), Mockito.any(DeliveryChannel.class)))
                .then(invocation -> {
                    Assertions.assertEquals(DataSourceType.MASTER, MasterContextHolder.getDatasourceType(),
                            "Update должен выполниться на мастере");
                    return 1;
                });

        notificationEvictionJob.doJob(null);
    }
}
