package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.android;


import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;

public class AndroidSteps {
    private final Order order;
    private final Login login;

    public AndroidSteps(User user) {
        this.order = new Order();
        this.login = new Login(user);
    }

    public Order Order() {
        return this.order;
    }

    public Login Login() {
        return this.login;
    }
}
