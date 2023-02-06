package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.common;

import org.junit.jupiter.api.Assertions;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;

@Resource.Classpath({"wms/infor.properties"})
public class UserLoader {
    private static UserLoader instance;

    @Property("infor.username")
    private static String username;

    @Property("infor.password")
    private static String password;

    public UserLoader() {
        ru.qatools.properties.PropertyLoader.newInstance().populate(this);
        Assertions.assertNotNull(username);
        Assertions.assertNotNull(password);
    }

    public static synchronized UserLoader getInstance() {
        if (instance == null) {
            instance = new UserLoader();
        }
        return instance;
    }

    public User getDefaultUser() {
        return new User(username, password);
    }
}
