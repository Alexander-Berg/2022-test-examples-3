package steps.ordersteps.ordersubsteps;

import ru.yandex.market.delivery.entities.common.Recipient;

public class RecipientSteps {

    private RecipientSteps() {
        throw new UnsupportedOperationException();
    }

    public static Recipient getRecipient() {
        Recipient recipient = new Recipient();

        recipient.setFio(PersonSteps.getPerson());
        recipient.setPhones(PhoneSteps.getPhoneList());
        recipient.setEmail("test@test.com");

        return recipient;
    }
}
