package ru.yandex.market.mbo.integration.test.billing;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.db.MboDbSelector;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author kravchenko-aa
 * @date 2019-11-26
 */
public class BillingPaidActionsTest extends BaseIntegrationTest {

    @Autowired
    JdbcOperations siteCatalogJdbcTemplate;
    @Autowired
    MboDbSelector ngPaidOperationDbSelector;

    @Test
    public void allPaidActionsAreDefined() {
        Map<Integer, PaidAction> paidActions =
            Stream.of(PaidAction.values())
                .collect(toMap(PaidAction::getId, Function.identity()));

        List<Integer> inDb = ngPaidOperationDbSelector.getJdbcTemplate()
            .queryForList("SELECT id FROM ng_paid_operation", Integer.class);

        paidActions.keySet().removeAll(inDb);

        assertThat("no PaidActions, which doesn't have description in ng_paid_operation", paidActions, is(emptyMap()));
    }
}
