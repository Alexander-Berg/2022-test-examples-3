package ru.yandex.direct.core.entity.client.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.YandexOffice;
import ru.yandex.direct.core.entity.client.model.ContactPhone;
import ru.yandex.direct.core.entity.user.model.User;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class GetManagerContactPhonesTest {

    private ClientOfficeService clientOfficeService;

    @Before
    public void before() {
        clientOfficeService = new ClientOfficeService(null, null);
    }

    @Parameterized.Parameter(0)
    public User user;

    @Parameterized.Parameter(1)
    public YandexOffice yandexOffice;

    @Parameterized.Parameter(2)
    public List<ContactPhone> expectedContacts;

    @Parameterized.Parameters
    public static Object[][] parameters() {
        return new Object[][] {
                // All offices
                {createUser("12345"), createOffice("msk", "+7 (495) 739-70-00"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (495) 739-70-00", 12345L))},
                {createUser("12345"), createOffice("spb", "+7 (812) 633-36-00"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (812) 633-36-00", 12345L))},
                {createUser("12345"), createOffice("ekb", "+7 (343) 385-01-00"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (343) 385-01-00", 12345L))},
                {createUser("12345"), createOffice("ukr", "+380 (48) 737-44-10"),
                        List.of(createPhone("0 800 60-48-61", null),
                                createPhone("+380 (48) 737-44-10", 12345L))},
                {createUser("12345"), createOffice("kaz", "+7 (727) 313-28-05"),
                        List.of(createPhone("+7 (727) 313-28-05", 12345L))},
                {createUser("12345"), createOffice("nsk", "+7 (383) 230-43-06"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (383) 230-43-06", 12345L))},
                {createUser("12345"), createOffice("kazan", "+7 (843) 524-71-71"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (843) 524-71-71", 12345L))},
                {createUser("12345"), createOffice("kiev", "+380 (44) 586-41-48"),
                        List.of(createPhone("0 800 60-48-61", null),
                                createPhone("+380 (44) 586-41-48", 12345L))},
                {createUser("12345"), createOffice("rostov", "+7 (863) 268-83-00"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (863) 268-83-00", 12345L))},
                {createUser("12345"), createOffice("nnov", "+7 (831) 233-06-06"),
                        List.of(createPhone("+7 (831) 233-06-06", 12345L))},
                {createUser("12345"), createOffice("boston", "+1 (617) 398-7870"),
                        List.of(createPhone("+1 (617) 398-7870", 12345L))},
                {createUser("12345"), createOffice("che", "+ 41 41 248 08 60"),
                        List.of(createPhone("+ 41 41 248 08 60", 12345L))},
                {createUser("12345"), createOffice("tur", "+90 212 386-87-60"),
                        List.of(createPhone("+90 212 386-87-60", 12345L))},
                {createUser("12345"), createOffice("blr", "+375 17 336-31-36"),
                        List.of(createPhone("+7 (495) 739-37-77", null),
                                createPhone("8 820 00-73-00-52", 12345L))},
                {createUser("12345"), createOffice("vvo", "8 800 333-96-39"),
                        List.of(createPhone("8 800 250-96-39", 12345L))},
                {createUser("12345"), createOffice("krr", "8 800 333-96-39"),
                        List.of(createPhone("8 800 250-96-39", 12345L))},
                {createUser("12345"), createOffice("rtv", "8 800 333-96-39"),
                        List.of(createPhone("8 800 250-96-39", 12345L))},
                {createUser("12345"), createOffice("kuf", "8 800 333-96-39"),
                        List.of(createPhone("8 800 250-96-39", 12345L))},
                {createUser("12345"), createOffice("cek", "8 800 333-96-39"),
                        List.of(createPhone("8 800 250-96-39", 12345L))},
                {createUser("12345"), createOffice("ufa", "8 800 333-96-39"),
                        List.of(createPhone("8 800 250-96-39", 12345L))},
                {createUser("12345"), createOffice("voz", "8 800 333-96-39"),
                        List.of(createPhone("8 800 250-96-39", 12345L))},
                {createUser("12345"), createOffice("pee", "8 800 333-96-39"),
                        List.of(createPhone("8 800 250-96-39", 12345L))},
                {createUser("12345"), createOffice("tjm", "8 800 333-96-39"),
                        List.of(createPhone("8 800 250-96-39", 12345L))},
                {createUser("12345"), createOffice("kra", "8 800 333-96-39"),
                        List.of(createPhone("8 800 250-96-39", 12345L))},

                // Different user phones from db
                {createUser("3543"), createOffice("msk", "+7 (495) 739-70-00"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (495) 739-70-00", 3543L))},
                {createUser("12345"), createOffice("msk", "+7 (495) 739-70-00"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (495) 739-70-00", 12345L))},
                {createUser(null), createOffice("msk", "+7 (495) 739-70-00"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (495) 739-70-00", null))},
                {createUser(""), createOffice("msk", "+7 (495) 739-70-00"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (495) 739-70-00", null))},
                {createUser("8 800 250‑96-39 доб.3266"),
                        createOffice("msk", "+7 (495) 739-70-00"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (495) 739-70-00", null))},
                {createUser("+7 (383) 230 43 06, 8 800 234 24 80"),
                        createOffice("msk", "+7 (495) 739-70-00"),
                        List.of(createPhone("8 800 250-96-39", null),
                                createPhone("+7 (495) 739-70-00", null))},

                // Null cases
                {createUser(null), null, List.of()},
                {createUser("12345"), null, List.of()},
                {createUser("12345"), createOffice("new nickname", "+7 (495) 739-70-00"), List.of()},
        };
    }

    private static User createUser(String phone) {
        return new User()
                .withPhone(phone);
    }

    private static YandexOffice createOffice(String officeNick, String officePhone) {
        return new YandexOffice()
                .withOfficeNick(officeNick)
                .withOfficePhone(officePhone);
    }

    private static ContactPhone createPhone(String phone, Long extension) {
        return new ContactPhone()
                .withPhone(phone)
                .withExtension(extension);
    }

    @Test
    public void testGetManagerContactPhones() {
        List<ContactPhone> contacts = clientOfficeService.getManagerContactPhones(user, yandexOffice);
        assertThat(contacts).isEqualTo(expectedContacts);
    }
}
