package ru.yandex.market.billing.imports.supply;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.imports.supply.model.FulfillmentSupply;
import ru.yandex.market.billing.imports.supply.model.FulfillmentSupplyStatus;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.billing.imports.supply.model.FulfillmentSupplyStatus.FINISHED;
import static ru.yandex.market.billing.imports.supply.model.FulfillmentSupplyStatus.IN_PROGRESS;
import static ru.yandex.market.billing.imports.supply.model.FulfillmentSupplyStatus.PROCESSED;

public class FulfillmentSupplyYtDaoSuppliesTest extends FunctionalTest {

    @Autowired
    private FulfillmentSupplyYtDao fulfillmentSupplyYtDao;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/fulfillment_shop_request/1d/latest"
            },
            csv = "FulfillmentSupplyYtDaoSuppliesTest.selectsOnlyRequiredStatuses.yql.csv",
            yqlMock = "FulfillmentSupplyYtDaoSuppliesTest.selectsOnlyRequiredStatuses.yql.mock"
    )
    public void selectsOnlyRequiredStatuses() {
        List<FulfillmentSupply> fetchedSupplies = new ArrayList<>();
        fulfillmentSupplyYtDao.suppliesApplyWithConsumer(fetchedSupplies::add);

        Set<FulfillmentSupplyStatus> statuses = StreamEx.of(fetchedSupplies).map(FulfillmentSupply::getStatus).toSet();
        assertThat(statuses).containsOnly(IN_PROGRESS, PROCESSED, FINISHED);
    }
}
