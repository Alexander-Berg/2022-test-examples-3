package ru.yandex.market.loyalty.core.dao;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.welcome.FreeDeliveryRequestRegisterDao;
import ru.yandex.market.loyalty.core.dao.welcome.WelcomePromoRequestStatus;
import ru.yandex.market.loyalty.core.model.delivery.FreeDeliveryRequestFromRegister;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 15.12.2020
 */
public class FreeDeliveryRequestFromRegisterDaoTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private FreeDeliveryRequestRegisterDao freeDeliveryRequestRegisterDao;

    @Before
    public void createFreeDeliveryRequest() {
        FreeDeliveryRequestFromRegister newFreeDeliveryRequestFromRegister =
                FreeDeliveryRequestFromRegister
                        .builder()
                        .withUid(123L)
                        .withStatus(WelcomePromoRequestStatus.IN_QUEUE)
                        .withMessage("")
                        .withCreationTime(new Date())
                        .withPreliminary(false)
                        .withReqId("")
                        .withProcessingTime(null)
                        .withTryCount(0)
                        .build();
        freeDeliveryRequestRegisterDao.createDeliveryPromoRequest(newFreeDeliveryRequestFromRegister);
    }

    @Test
    public void shouldFindAllFreeDeliveryRequests() {
        List<FreeDeliveryRequestFromRegister> allRequests = freeDeliveryRequestRegisterDao.getAllRequests();
        assertEquals(1, allRequests.size());
        FreeDeliveryRequestFromRegister freeDeliveryRequestFromDb = allRequests.get(0);
        assertEquals(123L, (long) freeDeliveryRequestFromDb.getUid());
    }

    @Test
    public void shouldFindDeliveryRequestByUid() {
        List<FreeDeliveryRequestFromRegister> requestsByUid = freeDeliveryRequestRegisterDao.findByUid(123L);
        assertEquals(1, requestsByUid.size());
        assertEquals(123L, (long) requestsByUid.get(0).getUid());
    }

    @Test
    public void shouldUpdateDeliveryRequest() {
        FreeDeliveryRequestFromRegister requestBeforeUpdate = freeDeliveryRequestRegisterDao.getAllRequests().get(0);
        freeDeliveryRequestRegisterDao.updateStatusAndProcessingTimeById(requestBeforeUpdate.getId(),
                WelcomePromoRequestStatus.SUCCESS);
        FreeDeliveryRequestFromRegister requestAfterUpdate =
                freeDeliveryRequestRegisterDao.findById(requestBeforeUpdate.getId());
        assertEquals(WelcomePromoRequestStatus.SUCCESS, requestAfterUpdate.getStatus());
        assertNotNull(requestAfterUpdate.getProcessingTime());
    }

    @Test
    public void shouldFindWarmUpSamples() {
        FreeDeliveryRequestFromRegister result = freeDeliveryRequestRegisterDao.getWarmUpSamples(1).get(0);
        assertNotNull(result);
        assertEquals(Long.valueOf(123L), result.getUid());
    }
}
