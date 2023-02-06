package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.InstanceIdentity;
import ru.yandex.market.wms.common.spring.enums.TypeOfIdentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferIdentityDetailDaoTest extends IntegrationTest {
    @Autowired
    private TransferIdentityDetailDao transferIdentityDetailDao;

    @Test
    @DatabaseSetup("/db/dao/transfer-identity-detail/before.xml")
    void shouldFoundTransferDetailsByTransferKey() {
        Map<String, List<InstanceIdentity>> detailsFounded =
                transferIdentityDetailDao.findTransferIdentityDetailsByTransferKeyInSuccessStatus("0000000001");
        assertNotNull(detailsFounded);
        assertEquals(2, detailsFounded.size());

        assertEquals("CIS_1_1", detailsFounded.get("00001").get(0).getPk().getIdentity());
        assertEquals(TypeOfIdentity.CIS.getName(), detailsFounded.get("00001").get(0).getPk().getType());

        assertEquals("CIS_1_2", detailsFounded.get("00002").get(0).getPk().getIdentity());
        assertEquals(TypeOfIdentity.CIS.getName(), detailsFounded.get("00002").get(0).getPk().getType());
    }

    @Test
    @DatabaseSetup("/db/dao/transfer-identity-detail/before.xml")
    void shouldntFoundAnyTransferDetailsForNonexistantTransfer() {
        Map<String, List<InstanceIdentity>> detailsNotFounded =
                transferIdentityDetailDao.findTransferIdentityDetailsByTransferKeyInSuccessStatus("0000000666");
        assertNotNull(detailsNotFounded);
        assertTrue(detailsNotFounded.isEmpty());
    }

    @Test
    @DatabaseSetup("/db/dao/transfer-identity-detail/before.xml")
    void shouldFoundTransferDetailsOnlyInSuccessStatus() {
        Map<String, List<InstanceIdentity>> detailsFounded =
                transferIdentityDetailDao.findTransferIdentityDetailsByTransferKeyInSuccessStatus("0000000002");
        assertNotNull(detailsFounded);
        assertEquals(1, detailsFounded.size());

        assertEquals("CIS_2_1", detailsFounded.get("00001").get(0).getPk().getIdentity());
        assertEquals(TypeOfIdentity.CIS.getName(), detailsFounded.get("00001").get(0).getPk().getType());
    }

    @Test
    @DatabaseSetup("/db/dao/transfer-identity-detail/before.xml")
    void shouldntFoundTransferDetailsWhenDetailsInOtherThenSuccessStatusesExist() {
        Map<String, List<InstanceIdentity>> detailsNotFounded =
                transferIdentityDetailDao.findTransferIdentityDetailsByTransferKeyInSuccessStatus("0000000003");
        assertNotNull(detailsNotFounded);
        assertTrue(detailsNotFounded.isEmpty());
    }

}
