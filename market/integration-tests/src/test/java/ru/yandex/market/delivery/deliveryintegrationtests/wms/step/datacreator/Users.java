package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;

@Resource.Classpath("wms/infor.properties")
public class Users {

    @Property("infor.password")
    private String password;

    public Users() {
        PropertyLoader.newInstance().populate(this);
    }

    private static final DatacreatorClient dataCreator = new DatacreatorClient();

    public static int getDefaultUserLockDuration() {
        int defaultDuration;

        try {
            defaultDuration = (int) RetryableTest.class.getMethod("duration").getDefaultValue();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        int lockSeconds = defaultDuration * 60;
        return lockSeconds;
    }

    @Step("Занимаем свободного пользователя через datacreator")
    public User lockUser(int lockMinutes) {
        Integer lockSeconds = lockMinutes * 60;
        ValidatableResponse response = dataCreator.usersLock("AT", lockSeconds);

        String username = response.extract().asString();
        return new User(username, password);
    }

    public User lockUser() {
        return lockUser(getDefaultUserLockDuration());
    }

    @Step("Снимаем лок с пользователя в datacreator")
    public void unlockUser(User user) {
        dataCreator.usersUnlock(user.getLogin());
    }
}
