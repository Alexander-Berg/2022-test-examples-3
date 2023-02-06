package ru.yandex.market.wms.common.spring.config;

import ru.yandex.market.wms.common.dao.LogisticUnitDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.LostLogDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OutboundRegisterDao;
import ru.yandex.market.wms.common.spring.service.SerialInventoryLostService;
import ru.yandex.market.wms.common.spring.service.SerialInventoryService;

public class LostWriteOffSrvParOne {
    private final SerialInventoryLostService serialInventoryLostService;
    private final SerialInventoryService serialInventoryService;
    private final LostLogDao lostLogDao;
    private final LogisticUnitDAO logisticUnitDAO;
    private final OutboundRegisterDao outboundRegisterDao;

    public LostWriteOffSrvParOne(SerialInventoryLostService serialInventoryLostService,
                                 SerialInventoryService serialInventoryService, LostLogDao lostLogDao,
                                 LogisticUnitDAO logisticUnitDAO, OutboundRegisterDao outboundRegisterDao) {
        this.serialInventoryLostService = serialInventoryLostService;
        this.serialInventoryService = serialInventoryService;
        this.lostLogDao = lostLogDao;
        this.logisticUnitDAO = logisticUnitDAO;
        this.outboundRegisterDao = outboundRegisterDao;
    }

    public SerialInventoryLostService getSerialInventoryLostService() {
        return serialInventoryLostService;
    }

    public SerialInventoryService getSerialInventoryService() {
        return serialInventoryService;
    }

    public LostLogDao getLostLogDao() {
        return lostLogDao;
    }

    public LogisticUnitDAO getLogisticUnitDAO() {
        return logisticUnitDAO;
    }

    public OutboundRegisterDao getOutboundRegisterDao() {
        return outboundRegisterDao;
    }
}
