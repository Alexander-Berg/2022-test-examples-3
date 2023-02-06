package ru.yandex.market.wms.common.spring.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;

import ru.yandex.market.wms.common.spring.dao.implementation.InstanceIdentityDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;

public class LostWriteOffSrvParTwo {
    private final OrderDao orderDao;
    private final Clock clock;
    private final PlatformTransactionManager platformTransactionManager;
    private final InstanceIdentityDAO instanceIdentityDAO;
    @Qualifier("archiveInstanceIdentityDao")
    private final InstanceIdentityDAO archiveInstanceIdentityDAO;

    public LostWriteOffSrvParTwo(OrderDao orderDao, Clock clock,
                                 PlatformTransactionManager platformTransactionManager,
                                 InstanceIdentityDAO instanceIdentityDAO,
                                 @Qualifier("archiveInstanceIdentityDao")
                                               InstanceIdentityDAO archiveInstanceIdentityDAO) {
        this.orderDao = orderDao;
        this.clock = clock;
        this.platformTransactionManager = platformTransactionManager;
        this.instanceIdentityDAO = instanceIdentityDAO;
        this.archiveInstanceIdentityDAO = archiveInstanceIdentityDAO;
    }

    public OrderDao getOrderDao() {
        return orderDao;
    }

    public Clock getClock() {
        return clock;
    }

    public PlatformTransactionManager getPlatformTransactionManager() {
        return platformTransactionManager;
    }

    public InstanceIdentityDAO getInstanceIdentityDAO() {
        return instanceIdentityDAO;
    }

    public InstanceIdentityDAO getArchiveInstanceIdentityDAO() {
        return archiveInstanceIdentityDAO;
    }
}
