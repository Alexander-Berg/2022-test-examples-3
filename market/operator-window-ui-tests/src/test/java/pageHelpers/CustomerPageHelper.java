package ui_tests.src.test.java.pageHelpers;

import Classes.customer.Customer;
import org.openqa.selenium.WebDriver;
import pages.Pages;

public class CustomerPageHelper {
    private WebDriver webDriver;

    public CustomerPageHelper(WebDriver webDriver){
        this.webDriver=webDriver;
    }

    /**
     * Найти через скрипт в админке неавторизованный заказ
     */
    public String getGidOfUnauthorizedOrder() {
        String orderGid = PageHelper.otherHelper(webDriver)
                .runningScriptFromAdministrationPage(
                        "api.db.select('from order o where o.buyerMuid is not null and o.buyerMuid = o.buyerUid order by id desc', [:], 1)"
                )
                .replaceAll("[\\[\\]\"]", "");
        return orderGid;
    }

    /**
     * Получить из gid заказа его номер
     */
    public String getOrderId(String orderGid) {
        String orderId = orderGid.replaceAll(".*T", "");
        return orderId;
    }

    /**
     * Привязать заказ
     * @param orderId
     */
    public void bindingOrder(String orderId) {
        Pages.ticketPage(webDriver).toast().hideNotificationError();
        Pages.customerPage(webDriver).header().selectOrder(orderId);
        Pages.customerPage(webDriver).header().clickBindingOrderButton();
    }

    /**
     * Получить gid клиента по uid
     * @param customerUid
     */
    public String getCustomerGid(String customerUid) {
        String customerGid = PageHelper.otherHelper(webDriver)
                .runningScriptFromAdministrationPage(
                        "api.db.of('customer').get('" + customerUid + "')"
                );
        return customerGid;
    }

    /**
     * Получить информацию о клиенте из заказа
     */
    public Customer getCustomer(){
        Customer customer = new Customer();
        customer
                .setMainProperties(new Classes.customer.MainProperties()
                        .setFullName(Pages.customerPage(webDriver).mainProperties().getCustomerFullName())
                        .setPhone(Pages.customerPage(webDriver).mainProperties().getCustomerPhone())
                        .setEmail(Pages.customerPage(webDriver).mainProperties().getCustomerEmail())
                        .setRegistrationDate(Pages.customerPage(webDriver).mainProperties().getCustomerRegistrationDate())
                        .setUid(Pages.customerPage(webDriver).mainProperties().getCustomerUID())
                        .setCashback(Pages.customerPage(webDriver).mainProperties().getCustomerCashback())
                );
        return customer;
    }
}
