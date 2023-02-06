package ui_tests.src.test.java.pageHelpers;

import Classes.customer.Customer;
import Classes.order.MainProperties;
import Classes.order.PaymentProperties;
import org.openqa.selenium.WebDriver;
import pages.Pages;

public class OrderHelper {
    private WebDriver webDriver;

    public OrderHelper(WebDriver webDriver){
        this.webDriver=webDriver;
    }

    /**
     * Получить основную информацию о заказе
     */
    public MainProperties getMainProperties(){
        MainProperties mainProperties = new MainProperties();
        mainProperties
                .setOrderNumber(Pages.orderPage(webDriver).header().getOrderNumber())
                .setTypeMarket(Pages.orderPage(webDriver).header().getTypeMarket())
                .setDateCreate(Pages.orderPage(webDriver).header().getDateCreate())
                ;
        return mainProperties;
    }

    /**
     * Получить информацию о клиенте из заказа
     */
    public Customer getCustomer(){
        Customer customer = new Customer();
        customer
                .setMainProperties(new Classes.customer.MainProperties()
                .setFullName(Pages.orderPage(webDriver).customerProperties().mainProperties().getFullName())
                .setPhone(Pages.orderPage(webDriver).customerProperties().mainProperties().getPhoneCustomer())
                .setEmail(Pages.orderPage(webDriver).customerProperties().mainProperties().getEmailCustomer())
        )
        ;
        return customer;
    }

    /**
     * Получить информацию об оплате из заказа
     */
    public PaymentProperties getPaymentProperties(){
        PaymentProperties paymentProperties = new PaymentProperties();
        paymentProperties
                .setPayer(Pages.orderPage(webDriver).paymentProperties().getPayer())
                .setCostDelivery(Pages.orderPage(webDriver).paymentProperties().getCostDelivery())
                .setOrderAmount(Pages.orderPage(webDriver).paymentProperties().getOrderAmount())
        ;
        return paymentProperties;
    }

    /**
     * Добавить комментарий на странице заказа
     * @param value
     */
    public void addComment(String value){
        Pages.orderPage(webDriver).generalInformationTab().comments().setTextComment(value);
        Pages.orderPage(webDriver).generalInformationTab().comments().clickAddButton();
    }

}
