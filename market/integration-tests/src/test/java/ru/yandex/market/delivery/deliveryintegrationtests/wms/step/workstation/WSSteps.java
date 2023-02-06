package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;

public class WSSteps {
    private final Login login;
    private final Inbound inbound;
    private final Outbound outbound;
    private final Items items;
    private final Reports reports;
    private final Wave wave;
    private final Order order;
    private final Packing packing;
    private final Task task;
    private final Inventorization inventorization;

    public WSSteps(WebDriver driver, User defaultUser) {
        this.login = new Login(driver, defaultUser);
        this.inbound = new Inbound(driver);
        this.outbound = new Outbound(driver);
        this.items = new Items(driver);
        this.reports = new Reports(driver);
        this.wave = new Wave(driver);
        this.order = new Order(driver);
        this.packing = new Packing(driver);
        this.task = new Task(driver);
        this.inventorization = new Inventorization(driver);
    }

    public Login Login() {
        return login;
    }

    public Inbound Inbound() {
        return inbound;
    }

    public Outbound Outbound() {
        return outbound;
    }

    public Items Items() {
        return items;
    }

    public Reports Reports() {
        return reports;
    }

    public Wave Wave() {
        return wave;
    }

    public Order Order() {
        return order;
    }

    public Packing Packing() {
        return packing;
    }

    public Task Task() {
        return task;
    }

    public Inventorization Inventorization() { return inventorization; }
}
