package ru.yandex.market.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.balance.xmlrpc.model.ClientContractsStructure;
import ru.yandex.market.common.balance.xmlrpc.model.ContractType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientContractInfo;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.subsidies.SubsidiesBalanceServiceHelper;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.protocol.ProtocolService;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class CheckSubsidiesOfferExecutorTest extends FunctionalTest {

    @Autowired
    private FeatureService featureService;

    @Autowired
    private SubsidiesBalanceServiceHelper subsidiesBalanceServiceHelper;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private BalanceService balanceService;

    private CheckSubsidiesOfferExecutor checkSubsidiesOfferExecutor;

    @BeforeEach
    void setUp() {
        when(balanceService.getClientContracts(eq(2L), eq(ContractType.SPENDABLE))).thenReturn(new ArrayList<>());
        when(balanceService.getClientContracts(eq(3L), eq(ContractType.SPENDABLE)))
                .thenReturn(Collections.singletonList(ClientContractInfo.fromBalanceStructure(
                        new ClientContractsStructure() {{
                            setIsActive(1);
                            setIsSuspended(0);
                            setCurrency("RUR");
                        }})));
        when(balanceService.getClientContracts(eq(4L), eq(ContractType.SPENDABLE)))
                .thenReturn(Collections.singletonList(ClientContractInfo.fromBalanceStructure(
                        new ClientContractsStructure() {{
                            setIsActive(0);
                            setIsSuspended(1);
                            setCurrency("EUR");
                        }})));
        when(balanceService.getClientContracts(eq(5L), eq(ContractType.SPENDABLE)))
                .thenReturn(Arrays.asList(
                        ClientContractInfo.fromBalanceStructure(new ClientContractsStructure() {{
                            setIsActive(0);
                            setCurrency("USD");
                            setIsSuspended(1);
                        }}), ClientContractInfo.fromBalanceStructure(new ClientContractsStructure() {{
                            setIsActive(1);
                            setCurrency("USD");
                            setIsSuspended(0);
                        }})));

        this.checkSubsidiesOfferExecutor = new CheckSubsidiesOfferExecutor(
                subsidiesBalanceServiceHelper,
                featureService,
                protocolService
        );
    }

    @Test
    @DbUnitDataSet(before = "doJob.before.csv")
    void doJob() {
        checkSubsidiesOfferExecutor.doJob(null);
        // Пришлось такие ассерты делать из-за сортировки датасетов...
        //Должен отключиться т.к. нет ни одного договора в балансе
        assertThat(
                featureService.getFeature(1, FeatureType.SUBSIDIES),
                hasProperty("status", equalTo(ParamCheckStatus.REVOKE))
        );
        //Должен отключиться т.к. нет ни одного договора в балансе
        assertThat(
                featureService.getFeature(2, FeatureType.SUBSIDIES),
                hasProperty("status", equalTo(ParamCheckStatus.REVOKE))
        );
        //Должен остаться включенным, т.к. единственный договор активен
        assertThat(
                featureService.getFeature(3, FeatureType.SUBSIDIES),
                hasProperty("status", equalTo(ParamCheckStatus.SUCCESS))
        );
        //Должен отключиться т.к. единственный договор не активен
        assertThat(
                featureService.getFeature(4, FeatureType.SUBSIDIES),
                hasProperty("status", equalTo(ParamCheckStatus.REVOKE))
        );
        //Должен остаться включенным т.к. один из договоров активен
        assertThat(
                featureService.getFeature(5, FeatureType.SUBSIDIES),
                hasProperty("status", equalTo(ParamCheckStatus.SUCCESS))
        );
    }

}
