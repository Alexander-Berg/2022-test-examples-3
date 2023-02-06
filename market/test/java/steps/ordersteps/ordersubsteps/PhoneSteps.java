package steps.ordersteps.ordersubsteps;

import java.util.Collections;
import java.util.List;

import ru.yandex.market.delivery.entities.common.Phone;

public class PhoneSteps {

    private static final String DEFAULT_PHONE = "11111111111";

    private PhoneSteps() {
        throw new UnsupportedOperationException();
    }

    public static Phone getPhone() {
        return getPhone(DEFAULT_PHONE);
    }

    public static Phone getPhone(String phoneNumber) {
        Phone phone = new Phone();

        phone.setAdditional("additional");
        phone.setPhoneNumber(phoneNumber);

        return phone;
    }

    public static List<Phone> getPhoneList() {
        return Collections.singletonList(getPhone(DEFAULT_PHONE));
    }

    public static List<Phone> getPhoneList(String phoneNumber) {
        return Collections.singletonList(getPhone(phoneNumber));
    }
}
