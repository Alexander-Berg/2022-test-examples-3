package steps;

import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.recipient.PersonDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.recipient.UpdateRecipientDto;

public class UpdateRecipientSteps {

    public static final Long ORDER_ID = 7000L;
    public static final Long PARCEL_ID = 3000L;
    public static final Long UPDATE_REQUEST_ID = 88585L;
    public static final long PARTNER_ID = 51L;
    public static final String FIRST_NAME = "name";
    public static final String LAST_NAME = "last name";
    public static final String MIDDLE_NAME = null;
    public static final String PERSONAL_FULLNAME_ID = "test-personal-fullname-id";

    public static final String PHONE = "+7 960 000 1111";
    public static final String PERSONAL_PHONE_ID = "test-personal-phone-id";
    public static final String EMAIL = "trrr@mail.ru";
    public static final String PERSONAL_EMAIL_ID = "test-personal-email-id";
    public static final boolean DATA_GATHERED = true;
    public static final String TRACK_CODE = "ABC1000";

    private UpdateRecipientSteps() {
    }

    public static UpdateRecipientDto createUpdateRecipientDto() {
        return new UpdateRecipientDto(
            ORDER_ID,
            PARCEL_ID,
            UPDATE_REQUEST_ID,
            PARTNER_ID,
            new PersonDto(FIRST_NAME, LAST_NAME, MIDDLE_NAME),
            PHONE,
            EMAIL,
            DATA_GATHERED,
            TRACK_CODE,
            PERSONAL_PHONE_ID,
            PERSONAL_EMAIL_ID,
            PERSONAL_FULLNAME_ID
        );
    }

    public static UpdateRecipientDto createUpdateRecipientDto(Long updateRequestId) {
        return new UpdateRecipientDto(
            ORDER_ID,
            PARCEL_ID,
            updateRequestId,
            PARTNER_ID,
            new PersonDto(FIRST_NAME, LAST_NAME, MIDDLE_NAME),
            PHONE,
            EMAIL,
            DATA_GATHERED,
            TRACK_CODE,
            PERSONAL_PHONE_ID,
            PERSONAL_EMAIL_ID,
            PERSONAL_FULLNAME_ID
        );
    }
}
