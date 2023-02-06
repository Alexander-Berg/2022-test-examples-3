package ru.yandex.market.pers.address.factories;

import org.apache.commons.lang3.tuple.Pair;
import ru.yandex.market.pers.address.services.blackbox.BlackboxClient;
import ru.yandex.market.pers.address.services.blackbox.UserInfoResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlackboxUserFactory {
    private String firstName;
    private String lastName;
    private String phone;
    private List<UserInfoResponse.Address> emails = new ArrayList<>();

    public static BlackboxUserFactory builder() {
        return new BlackboxUserFactory();
    }

    public BlackboxUserFactory setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public BlackboxUserFactory setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public BlackboxUserFactory setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public BlackboxUserFactory addEmail(String email) {
        return addEmail(email, false);
    }

    public BlackboxUserFactory addEmail(String email, boolean isDefault) {
        this.emails.add(new UserInfoResponse.Address(email, isDefault));
        return this;
    }

    public UserInfoResponse build() {
        return new UserInfoResponse(
            Collections.singletonList(new UserInfoResponse.User(
                Stream.of(
                    Pair.of(BlackboxClient.LAST_NAME_FLAG_NAME_MAGIC, lastName),
                    Pair.of(BlackboxClient.FIRST_NAME_FLAG_NAME_MAGIC, firstName),
                    Pair.of(BlackboxClient.PHONE_FLAG_NAME_MAGIC, phone)
                )
                    .filter(pair -> pair.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                emails
            ))
        );
    }
}
