package ru.yandex.autotests.directapi.steps;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.autotests.directapi.exceptions.DirectAPIException;

/**
 * Created by IntelliJ IDEA.
 * User: mariabye
 * Date: 05.07.12
 * Time: 10:00
 * Вызов позволяет включить все счета клиента
 */
public class InvoiceTurnOn{
    @Ignore
    @Test
    public void turnOnOverfraftInvoices() throws DirectAPIException {
        //TestBalanceUtils.payAllInvoicesByLogin("maria.baibik");
    }

    @Ignore
    @Test
    public void turnOnCreditInvoices() throws DirectAPIException {
       // TestBalanceUtils.payAllInvoicesByLogin("at-direct-api-test");
    }

}
