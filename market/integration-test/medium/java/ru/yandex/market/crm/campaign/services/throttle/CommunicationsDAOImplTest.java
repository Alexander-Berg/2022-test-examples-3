package ru.yandex.market.crm.campaign.services.throttle;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.crm.campaign.test.AbstractServiceMediumTest;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

/**
 * @author zloddey
 */
class CommunicationsDAOImplTest extends AbstractServiceMediumTest {
    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;
    @Inject
    private DaoFactory daoFactory;

    @BeforeEach
    public void prepare() {
        ytSchemaTestHelper.prepareCommunicationsTable();
    }

    @ParameterizedTest
    @EnumSource(CommunicationsDAOContract.class)
    void contractCompliance(CommunicationsDAOContract contract) {
        var dao = daoFactory.create(new ChannelDescription(UidType.EMAIL));
        contract.verify(dao);
    }
}
