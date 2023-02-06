package steps.orderSteps;

import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.buyer.PersonalDataStatus;

public class BuyerSteps {
    private static final String ID = "123";
    private static final Long UID = 333L;
    private static final String UUID = "321";
    private static final Long MUID = 222L;
    private static final String IP = "127.0.0.1";
    private static final Long REGION_ID = 213L;
    private static final String LAST_NAME = "lastName";
    private static final String FIRST_NAME = "firstName";
    private static final String MIDDLE_NAME = "middleName";
    private static final String PHONE = "71234567890";
    private static final String EMAIL = "test@test.com";
    private static final boolean DONT_CALL = true;

    private BuyerSteps() {
    }

    public static Buyer getBuyer() {
        Buyer buyer = new Buyer();

        buyer.setId(ID);
        buyer.setUid(UID);
        buyer.setUuid(UUID);
        buyer.setMuid(MUID);
        buyer.setIp(IP);
        buyer.setRegionId(REGION_ID);
        buyer.setLastName(LAST_NAME);
        buyer.setFirstName(FIRST_NAME);
        buyer.setMiddleName(MIDDLE_NAME);
        buyer.setPhone(PHONE);
        buyer.setEmail(EMAIL);
        buyer.setDontCall(DONT_CALL);

        return buyer;
    }

    public static Buyer getBuyerWithPersonalDataGathered() {
        Buyer buyer = getBuyer();

        buyer.setPersonalDataGathered(PersonalDataStatus.SUCCESS);

        return buyer;
    }
}
