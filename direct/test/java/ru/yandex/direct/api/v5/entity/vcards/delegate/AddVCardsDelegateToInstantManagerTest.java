package ru.yandex.direct.api.v5.entity.vcards.delegate;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.vcard.model.InstantMessenger;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class AddVCardsDelegateToInstantManagerTest {
    private static final String TYPE = "icq";
    private static final String LOGIN = "1";

    @Parameterized.Parameter
    public com.yandex.direct.api.v5.vcards.InstantMessenger requestMessenger;
    @Parameterized.Parameter(value = 1)
    public InstantMessenger expectedMessenger;

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{null, null},
                new Object[]{
                        new com.yandex.direct.api.v5.vcards.InstantMessenger().withMessengerClient(TYPE)
                                .withMessengerLogin(LOGIN),
                        new InstantMessenger().withType(TYPE)
                                .withLogin(LOGIN)});
    }

    @Test
    public void test() {
        InstantMessenger actualMessenger = AddVCardsDelegate.toVcardInstantMessenger(requestMessenger);

        assertThat(actualMessenger).isEqualTo(expectedMessenger);
    }
}
