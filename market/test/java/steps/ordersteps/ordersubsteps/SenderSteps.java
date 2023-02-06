package steps.ordersteps.ordersubsteps;

import ru.yandex.market.delivery.entities.common.Sender;

public class SenderSteps {

    private SenderSteps() {
        throw new UnsupportedOperationException();
    }

    public static Sender getSender() {
        Sender sender = new Sender();

        sender.setId(ResourceIdSteps.getResourceId());
        sender.setIncorporation("incorporation");
        sender.setPhones(PhoneSteps.getPhoneList());
        sender.setEmail("sender@email.com");
        sender.setContact(PersonSteps.getPerson());
        sender.setName("senderName");
        sender.setAddress(LocationSteps.getLocation());

        return sender;
    }
}
