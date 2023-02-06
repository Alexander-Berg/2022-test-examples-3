package steps.orderSteps;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;

public class RecipientSteps {
    private static final String LAST_NAME = "RecipientLastName";
    private static final String FIRST_NAME = "RecipientFirstName";
    private static final String MIDDLE_NAME = "RecipientMiddleName";
    private static final String PHONE = "71234567891";
    private static final String EMAIL = "test-recipient@test.com";

    private RecipientSteps() {
    }

    @Nonnull
    public static Recipient getRecipient() {
        return new Recipient(
            new RecipientPerson(FIRST_NAME, MIDDLE_NAME, LAST_NAME),
            PHONE,
            EMAIL
        );
    }
}
