package ru.yandex.market.pers.address.factories;

import ru.yandex.market.pers.address.controllers.model.ContactDto;

import java.util.function.Function;

import static ru.yandex.market.pers.address.factories.ContactFactory.DEFAULT_FIRST_NAME;
import static ru.yandex.market.pers.address.factories.ContactFactory.DEFAULT_LAST_NAME;

public class ContactDtoFactory {
    public static ContactDto sample() {
        return sample(Function.identity());
    }

    public static ContactDto sample(
            Function<ContactDto.ContactDtoBuilder, ContactDto.ContactDtoBuilder> callback
    ) {
        return callback.apply(
                ContactDto.contactDtoBuilder()
                        .setEmail("user@yandex-team.ru")
                        .setRecipient(DEFAULT_FIRST_NAME + " Иванович " + DEFAULT_LAST_NAME)
                        .setPhoneNum("+796514578945")
        ).build();
    }

    public static ContactDto anotherSample() {
        return ContactDto.contactDtoBuilder()
                .setEmail("anotherUser@yandex-team.ru")
                .setRecipient("Петр Иванович Петров")
                .setPhoneNum("+79651357954")
                .build();
    }

}
