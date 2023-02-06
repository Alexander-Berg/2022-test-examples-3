package ru.yandex.market.partner.ds;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.common.balance.model.BalanceException;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;

/**
 * Тесты для {@link ChangeDatasourceManagerServantlet}.
 */
class ChangeDatasourceManagerServantletTest extends FunctionalTest {

    @Autowired
    private BalanceService balanceService;

    @Test
    @DbUnitDataSet(
            before = "ChangeDatasourceManagerServantletTest.before.csv",
            after = "ChangeDatasourceManagerServantletTest.afterUpdateManager.csv"
    )
    void changeManager() {
        final ResponseEntity<String> result =
                FunctionalTestHelper.post(baseUrl + "/changeDatasourceManager?_user_id=123&newManagerId=456&datasourceId=774&datasourceId=777");
        assertThat(result.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(result.getBody(), not(containsString("<errors>")));
    }

    /**
     * Проверить случай, когда менеджер не существует.
     */
    @Test
    @DbUnitDataSet
    void testWrongManager() {
        final String response = FunctionalTestHelper.post(getUrl()).getBody();
        assertThat(response, containsString(ChangeDatasourceManagerServantlet.WRONG_MANAGER.getMessageCode()));
    }

    /**
     * Проверить случай, когда менеджер не найден в Балансе.
     */
    @Test
    @DbUnitDataSet(before = "testWrongBalanceAccess.csv")
    void testWrongBalanceAccess() {
        Mockito.doThrow(new BalanceException(
                "<error>" +
                        "    <msg>Object not found: Manager for uid was not found</msg>" +
                        "    <object>Manager for uid 648078683 was not found</object>" +
                        "    <wo-rollback>0</wo-rollback>" +
                        "    <method>process_order</method>" +
                        "    <code>NOT_FOUND</code>" +
                        "    <parent-codes>" +
                        "        <code>EXCEPTION</code>" +
                        "    </parent-codes>" +
                        "    <contents>Object not found: Manager for uid was not found</contents>" +
                        "</error>"))
                .when(balanceService)
                .createOrUpdateOrderByCampaign(any(), Mockito.anyLong());

        final String response = FunctionalTestHelper.post(getUrl(), String.class).getBody();
        assertThat(response, containsString(ChangeDatasourceManagerServantlet.WRONG_BALANCE_ACCESS.getMessageCode()));

        Mockito.verify(balanceService, Mockito.times(1)).createOrUpdateOrderByCampaign(any(), Mockito.anyLong());
        Mockito.verifyNoMoreInteractions(balanceService);
    }

    /**
     * Проверить случай, когда менеджер для uid'a не найден.
     */
    @Test
    @DbUnitDataSet(before = "testWrongBalanceAccess.csv")
    void testWrongManagerForUid() {
        //language=xml
        Mockito.doThrow(new BalanceException("" +
                "<error>\n" +
                "    <msg>Manager for uid 1395442803 was not found</msg>\n" +
                "    <tanker-fields>['object']</tanker-fields>\n" +
                "    <object/>\n" +
                "    <manager-uid>1395442803</manager-uid>\n" +
                "    <wo-rollback>0</wo-rollback>\n" +
                "    <method>_process_order</method>\n" +
                "    <code>MANAGER_NOT_FOUND</code>\n" +
                "    <parent-codes>\n" +
                "        <code>NOT_FOUND</code>\n" +
                "        <code>EXCEPTION</code>\n" +
                "    </parent-codes>\n" +
                "    <contents>Manager for uid 1395442803 was not found</contents>\n" +
                "</error>"
        ))
                .when(balanceService)
                .createOrUpdateOrderByCampaign(any(), Mockito.anyLong());

        final String response = FunctionalTestHelper.post(getUrl(), String.class).getBody();
        assertThat(response, containsString(ChangeDatasourceManagerServantlet.WRONG_MANAGER.getMessageCode()));

        Mockito.verify(balanceService, Mockito.times(1)).createOrUpdateOrderByCampaign(any(), Mockito.anyLong());
        Mockito.verifyNoMoreInteractions(balanceService);
    }


    private String getUrl() {
        return baseUrl + "/changeDatasourceManager?_user_id=123&newManagerId=456&datasourceId=774";
    }

}
