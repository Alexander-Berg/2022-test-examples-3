package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InboundTable;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;

public class UISteps {
    private WebDriver driver;

    private final Login login;
    private final Receiving receiving;
    private final Order order;
    private final Navigation navigation;
    private final Inventorization inventorization;
    private final Wave wave;
    private final Placement placement;
    private final Nok nok;
    private final Balances balances;
    private final Items items;
    private final SupervisorActivity supervisorActivity;
    private final DimensionManagement dimensionManagement;
    private final Dropping dropping;
    private final InboundTable inboundTable;
    private final Replenishment replenishment;

    public UISteps(WebDriver driver, User defaultUser, InboundTable inboundTable) {
        this.driver = driver;
        this.inboundTable = inboundTable;
        this.login = new Login(driver, defaultUser);
        this.receiving = new Receiving(driver, inboundTable);
        this.order = new Order(driver);
        this.navigation = new Navigation(driver);
        this.inventorization = new Inventorization(driver);
        this.wave = new Wave(driver);
        this.placement = new Placement(driver);
        this.nok = new Nok(driver);
        this.balances = new Balances(driver);
        this.items = new Items(driver);
        this.supervisorActivity = new SupervisorActivity(driver, defaultUser);
        this.replenishment = new Replenishment(driver);
        this.dimensionManagement = new DimensionManagement(driver);
        this.dropping = new Dropping(driver);
    }

    public Login Login() { return login; }

    public Receiving Receiving() {
        return receiving;
    }

    public Order Order() { return order; }

    public Navigation Navigation() { return navigation; }

    public Inventorization Inventorization() { return inventorization; }

    public Wave Wave() { return wave; }

    public Placement Placement() { return placement; }

    public Nok Nok() { return nok; }

    public Balances Balances() { return balances; }

    public Items Items() { return items; }

    public SupervisorActivity SupervisorActivity() { return supervisorActivity; }

    public Replenishment Replenishment() { return replenishment; }

    public DimensionManagement DimensionManagement() { return dimensionManagement; }

    public Dropping Dropping() {
        return dropping;
    }
}
