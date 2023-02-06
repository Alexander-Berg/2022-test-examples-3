package ru.yandex.market.partner.mvc.controller.feature;

import java.util.Collections;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.common.balance.xmlrpc.model.ContractType;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на логику работы {@link SubsidiesController}.
 *
 * @author fbokovikov
 */
public class SubsidiesControllerFunctionalTest extends FunctionalTest {

    private static final Long SHOP_ID = 774L;

    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceService balanceService;

    /**
     * Тест проверяет следующий сценарий:
     * <ol>
     * <li>У магазина не заполен идентификатор клиента предоплаты</li>
     * </ol>
     * Ручка вернет пустой ответ.
     */
    @Test
    @DbUnitDataSet
    public void sellerClientIdNotFound() {
        ResponseEntity<String> response = getSubsidiesContract();
        assertResult(response, StringUtils.EMPTY);
    }

    /**
     * Тест проверяет, что при выполнении следующих условий:
     * <ol>
     * <li>У магазина заведен договор на предоплату:</li>
     * <ul>
     * <li>{@code STATUS = COMPLETED}</li>
     * <li>Заполнен идентификатор предоплатного клиента</li>
     * </ul>
     * <li>В балансе нет активных дотационных договоров</li>
     * </ol>
     * Ручка вернет пустой ответ.
     */
    @Test
    @DbUnitDataSet(before = "contractNotFound.csv")
    public void contractNotFound() {
        when(balanceService.getClientContracts(100500L, ContractType.SPENDABLE))
                .thenReturn(Collections.emptyList());
        ResponseEntity<String> response = getSubsidiesContract();
        assertResult(response, StringUtils.EMPTY);
    }

    private ResponseEntity<String> getSubsidiesContract() {
        return FunctionalTestHelper.get(
                baseUrl + "/subsidies/contract?datasource_id={datasourceId}",
                SHOP_ID
        );
    }

    private void assertResult(ResponseEntity<String> response, String contractNo) {
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyEquals(
                                "result",
                                "{\"contractNo\":" + "\"" + contractNo + "\"" + "}"
                        )
                )
        );
    }

}
