package ru.yandex.autotests.market.balance;

import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

public interface TestBalanceXmlRpcClient {

    /**
     * https://wiki.yandex-team.ru/Testirovanie/FuncTesting/Billing/IntegrationInterface/#setclientoverdraft
     *
     * @param clientId
     * @param serviceId
     * @param overdraftLimit
     * @return
     * @throws XmlRpcException
     */
    List<Object> SetClientOverdraft(Object clientId, int serviceId, int overdraftLimit) throws XmlRpcException;

    /**
     * https://wiki.yandex-team.ru/Testirovanie/FuncTesting/Billing/IntegrationInterface/#makeoebspayment
     */
    Object MakeOEBSPayment(Map map) throws XmlRpcException;
}
