package ru.yandex.market.ocrm.module.loyalty.test;

import java.math.BigDecimal;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.utils.DomainException;
import ru.yandex.market.loyalty.api.model.perk.PerkStatResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.ocrm.module.common.Customer;
import ru.yandex.market.ocrm.module.loyalty.LoyaltyCashbackAccrual;
import ru.yandex.market.ocrm.module.loyalty.ModuleLoyaltyTestConfiguration;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ocrm.module.loyalty.LoyaltyConfiguration.CASHBACK_ACCRUAL_UPPER_LIMIT;
import static ru.yandex.market.ocrm.module.loyalty.LoyaltyConfiguration.CASHBACK_CAMPAIGN_NAME;
import static ru.yandex.market.ocrm.module.loyalty.LoyaltyConfiguration.CASHBACK_ISSUER;
import static ru.yandex.market.ocrm.module.loyalty.LoyaltyConfiguration.CASHBACK_PRODUCT_ID;
import static ru.yandex.market.ocrm.module.loyalty.LoyaltyConfiguration.CASHBACK_SERVICE_ID;

@DirtiesContext
@ContextConfiguration(classes = ModuleLoyaltyTestConfiguration.class)
@ExtendWith(SpringExtension.class)
public class CashbackAccrualTest {

    @Inject
    private BcpService bcpService;
    @Inject
    private ConfigurationService configurationService;
    @Inject
    private MarketLoyaltyClient marketLoyaltyClient;

    @BeforeEach
    public void setUp() {
        clearInvocations(marketLoyaltyClient);
        when(marketLoyaltyClient.perkStatus(any(PerkType.class), anyLong(), anyLong(), anyBoolean()))
                .thenReturn(PerkStatResponse.builder().build());


        configurationService.setValue(CASHBACK_CAMPAIGN_NAME.key(), "qwe");
        configurationService.setValue(CASHBACK_SERVICE_ID.key(), "asd");
        configurationService.setValue(CASHBACK_ISSUER.key(), "zxc");
        configurationService.setValue(CASHBACK_PRODUCT_ID.key(), "tgb");
    }

    /**
     * Проверяем корректность создания запроса кешбэка с последующим ударом по ручке Loyalty
     * <p>
     * Запрашиваемый кешбэк должен быть положителен и не превышать установленный лимит, иначе получаем ошибку
     * и удар по ручке Loyalty не происходит
     *
     * @param upperLimit    - максимально допустимый кешбэк за раз (0 и отрицательные значения считаем отсутствием
     *                      настройки)
     * @param amount        - начисляемый кешбэк
     * @param errorExpected - ожидаем ли ошибку (true для некорректных <code>amount</code> и <code>upperLimit</code>)
     */
    @Transactional
    @ParameterizedTest(name = "Limit = {0}, Amount = {1}, Error expected = {2}")
    @CsvSource({
            "100.50, 45.10, false", "110.60, 110.60, false", "120.75, 200, true",
            "130.90, 0, true", "140.40, -50.9, true", "0, 0, true",
            "0, 50.8, true", "0, -50.8, true", "-25.3, 50.8, true",
            "-25.3, 0, true", "-25.3, -50.8, true"
    })
    public void cashbackAccrualTest(BigDecimal upperLimit, BigDecimal amount, boolean errorExpected) {
        configurationService.setValue(CASHBACK_ACCRUAL_UPPER_LIMIT.key(), upperLimit);

        Customer customer = bcpService.create(Customer.FQN, Map.of(
                Customer.TITLE, "qwe",
                Customer.UID, 123456789));

        Executable accrualOperation = () ->
                bcpService.create(LoyaltyCashbackAccrual.FQN, Map.of(
                        LoyaltyCashbackAccrual.CONTEXT, customer.getGid(),
                        LoyaltyCashbackAccrual.CASHBACK_AMOUNT, amount));

        if (errorExpected) {
            Assertions.assertThrows(DomainException.class, accrualOperation);
            verifyNoInteractions(marketLoyaltyClient);
        } else {
            Assertions.assertDoesNotThrow(accrualOperation);
            verify(marketLoyaltyClient).accrual(any());
        }
    }

    /**
     * Если в конфигурации системы не установлена максимально допустимая сумма кешбэка, попытка начислить
     * кешбэк должна приводить к ошибке и удар по ручке Loyalty не происходит
     */
    @Test
    @Transactional
    public void notSetCashbackUpperLimitTest() {
        configurationService.setValue(CASHBACK_ACCRUAL_UPPER_LIMIT.key(), null);

        Customer customer = bcpService.create(Customer.FQN, Map.of(
                Customer.TITLE, "qwe",
                Customer.UID, 123456789));

        Assertions.assertThrows(DomainException.class, () ->
                bcpService.create(LoyaltyCashbackAccrual.FQN, Map.of(
                        LoyaltyCashbackAccrual.CONTEXT, customer.getGid(),
                        LoyaltyCashbackAccrual.CASHBACK_AMOUNT, BigDecimal.TEN)));

        verifyNoInteractions(marketLoyaltyClient);
    }

}
