package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.tsd;

import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.InboundTable;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;

public class TsdSteps {
    private Login login;
    private Inbound inbound;
    private Move move;
    private Complectation complectation;
    private Outgoing outgoing;
    private InboundTable inboundTable;

    public TsdSteps(WebDriver driver, User defaultUser, InboundTable inboundTable) {
        this.login = new Login(driver, defaultUser);
        this.inbound = new Inbound(driver);
        this.move = new Move(driver);
        this.complectation = new Complectation(driver);
        this.outgoing = new Outgoing(driver);
        this.inboundTable = inboundTable;
    }

    public Inbound Inbound() {
        return inbound;
    }

    public Login Login() {
        return login;
    }

    public Move Move() {
        return move;
    }

    public Complectation Complectation() {
        return complectation;
    }

    public Outgoing Outgoing() {
        return outgoing;
    }
}
