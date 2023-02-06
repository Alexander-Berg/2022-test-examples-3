package ru.yandex.chemodan.app.psbilling.core;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import lombok.AllArgsConstructor;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.balanceclient.model.response.ClientActInfo;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetPartnerBalanceContractResponseItem;

@AllArgsConstructor
public class PsBillingBalanceFactory {
    private BalanceService balanceService;
    private BalanceClientStub balanceClientStub;
    private ClientBalanceDao clientBalanceDao;
    public final long defaultContractId = 2L;

    public void setIncomeSum(long clientId, Currency currency, double amount) {
        GetClientContractsResponseItem contract = createContract(clientId, defaultContractId, currency);
        createContractBalance(contract, x -> x.withClientPaymentsSum(BigDecimal.valueOf(amount)));
    }

    public GetPartnerBalanceContractResponseItem createContractBalance(
            GetClientContractsResponseItem contract, Function<
            GetPartnerBalanceContractResponseItem, GetPartnerBalanceContractResponseItem> customizer) {

        GetPartnerBalanceContractResponseItem result = new GetPartnerBalanceContractResponseItem()
                .withContractId(contract.getId())
                .withCurrencyCode(contract.getCurrency())
                .withFirstDebtAmount(BigDecimal.ZERO)
                .withExpiredDebtAmount(BigDecimal.ZERO)
                .withActSum(BigDecimal.ZERO)
                .withClientPaymentsSum(BigDecimal.ZERO);

        result = customizer.apply(result);
        balanceClientStub.addContractBalance(result);
        return result;
    }

    public void updateContractBalance(GetPartnerBalanceContractResponseItem item) {
        balanceClientStub.addContractBalance(item);
    }


    public GetClientContractsResponseItem createContract(long clientId, long id, Currency currency) {
        return createContract(clientId, id, currency, x -> x);
    }

    public GetClientContractsResponseItem createContract(long clientId, long id, Currency currency,
                                                         Function<GetClientContractsResponseItem,
                                                                 GetClientContractsResponseItem> modifier) {
        GetClientContractsResponseItem contract = new GetClientContractsResponseItem();
        contract.setId(id);
        contract.setCurrency(currency.getCurrencyCode());
        contract.setServices(Cf.set(balanceService.getServiceId()));
        contract.setIsActive(1);
        contract = modifier.apply(contract);

        balanceClientStub.createOrReplaceClientContract(clientId, contract);
        return contract;
    }

    public ClientActInfo createActInfo(long contractId, LocalDate actDate, BigDecimal amount, BigDecimal paidAmount) {
        ClientActInfo actInfo = new ClientActInfo();
        actInfo.setDt(getActDt(actDate).toDateTime(LocalTime.MIDNIGHT).getMillis());
        actInfo.setId(UUID.randomUUID().toString());
        actInfo.setAmount(amount);
        actInfo.setPaidAmount(paidAmount);
        actInfo.setContractId(contractId);
        return actInfo;
    }

    public LocalDate getActDt(LocalDate calcMonth) {
        // последний день месяца 00 00. Справедливо для всех актов - уточнил у Баланса
        return calcMonth.withDayOfMonth(1).plusMonths(1).minusDays(1);
    }

    public ClientBalanceEntity createBalance(Long clientId, String currency, Instant createDate) {
        Instant now = Instant.now();
        DateUtils.freezeTime(createDate);
        ClientBalanceEntity balance = clientBalanceDao.createOrUpdate(clientId, Currency.getInstance(currency),
                BigDecimal.TEN, Option.empty());
        DateUtils.freezeTime(now);
        return balance;
    }

    public ClientBalanceEntity createBalance(int clientId, String currency, double balance) {
        return createBalance(Long.valueOf(clientId), Currency.getInstance(currency), balance);
    }

    public ClientBalanceEntity createBalance(Long clientId, Currency currency, double balance) {
        return clientBalanceDao.createOrUpdate(clientId, currency, BigDecimal.valueOf(balance), Option.empty());
    }

    public ClientBalanceEntity createBalance(long clientId, Currency currency, double amount, Instant voidAt) {
        return clientBalanceDao.insert(
                ClientBalanceDao.InsertData.builder()
                        .clientId(clientId)
                        .balanceAmount(BigDecimal.valueOf(amount))
                        .balanceCurrency(currency)
                        .balanceVoidAt(Option.of(voidAt))
                        .build());
    }
}
