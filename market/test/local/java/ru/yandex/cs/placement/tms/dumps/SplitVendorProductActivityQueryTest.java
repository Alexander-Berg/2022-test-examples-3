package ru.yandex.cs.placement.tms.dumps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;
import ru.yandex.vendor.model.VendorProductsActivity;

@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/dumps/SplitVendorProductActivityQueryTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/dumps/SplitVendorProductActivityQueryTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
public class SplitVendorProductActivityQueryTest extends AbstractCsPlacementTmsFunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;

    @Test
    public void testSplitVendorProductActivityQuery() {
        var query = new SplitVendorProductActivityQuery(
                vendorNamedParameterJdbcTemplate,
                csBillingNamedParameterJdbcTemplate
        );

        VendorProductsActivity activity1 = new VendorProductsActivity();
        activity1.setVendorId(1L);
        activity1.setVendorTitle("Offer vendor1");
        activity1.setOrderId("132-101");
        activity1.setClientId(0L);
        activity1.setProductId(1L);
        activity1.setProductName("Рекомендованные магазины");
        activity1.setTarifficationPeriodId(11L);
        activity1.setTarifficationPeriodName("Ежемесячно");
        activity1.setContractTypeId(2L);
        activity1.setContractTypeName("Постоплата");
        activity1.setTariffMinSum(3.0);
        activity1.setManagerLogin("vasya");
        activity1.setWasActive(true);
        activity1.setDatasourceId(1L);
        activity1.setWasActiveInList(false);
        activity1.setWasActiveOthers(true);

        VendorProductsActivity activity2 = new VendorProductsActivity();
        activity2.setVendorId(2L);
        activity2.setVendorTitle("Offer vendor2");
        activity2.setOrderId("132-102");
        activity2.setClientId(0L);
        activity2.setProductId(2L);
        activity2.setProductName("Ставки на модели");
        activity2.setTarifficationPeriodId(1L);
        activity2.setTarifficationPeriodName("Ежедневно");
        activity2.setContractTypeId(2L);
        activity2.setContractTypeName("Постоплата");
        activity2.setTariffMinSum(6.0);
        activity2.setManagerLogin("sveta");
        activity2.setWasActive(true);
        activity2.setDatasourceId(2L);
        activity2.setWasActiveInList(false);
        activity2.setWasActiveOthers(true);

        List<VendorProductsActivity> actual = new ArrayList<>();
        query.execute(actual::add, Collections.emptyMap());
        Assertions.assertThat(actual)
                .containsExactlyInAnyOrderElementsOf(Arrays.asList(activity1, activity2));
    }

}
