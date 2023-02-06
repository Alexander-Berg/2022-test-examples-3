package ru.yandex.market.tpl.core.domain.user;

import java.time.Instant;
import java.util.Optional;

import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.api.model.user.UserRole;
import ru.yandex.market.tpl.api.model.user.UserStatus;
import ru.yandex.market.tpl.api.model.user.UserType;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.service.user.transport.TransportType;

@UtilityClass
public class UserUtil {

    public static final String EMAIL = "pashka_komarov@yandex.ru";
    public static final String FIRST_NAME = "Пашка";
    public static final String LAST_NAME = "Комаров";
    public static final String NAME = LAST_NAME + " " + FIRST_NAME;

    public User createUserWithoutSchedule(long uid, TransportType transportType, Company company) {
        return createUserWithoutSchedule(uid, transportType, company, FIRST_NAME, LAST_NAME, EMAIL);
    }

    public User createUserWithoutSchedule(long uid, TransportType transportType, Company company, String firstName,
                                          String lastName, String email) {
        return createUserWithoutSchedule(uid, transportType,company, firstName, lastName, email, null);
    }

    public User setStatus(User user, UserStatus userStatus, Instant updatedAt) {
        user.setNewStatus(userStatus, updatedAt);
        return user;
    }

    public User createUserWithoutSchedule(long uid, TransportType transportType, Company company, String firstName,
                                          String lastName, String email, String yaProId) {
        User user = new User();
        user.setUid(uid);
        user.setEmail(Optional.ofNullable(email).orElse(EMAIL));
        user.setRole(UserRole.COURIER);
        user.setUserType(UserType.PARTNER);
        user.setName(NAME);
        user.setPhone("+79990000000");
        user.setFirstName(Optional.ofNullable(firstName).orElse(FIRST_NAME));
        user.setLastName(Optional.ofNullable(lastName).orElse(LAST_NAME));
        user.setDeleted(false);
        user.setCompany(company);
        user.setTransportType(transportType);
        user.setYaProId(yaProId);
        user.setVehicleNumber("E999КХ");
        user.setDsmExternalId("dsm-id-for-" + Long.toString(uid));
        return user;
    }

    public void setId(User user, long id) {
        user.setId(id);
    }

    public void setRole(User user, UserRole role) {
        user.setRole(role);
    }

    public void setDsmExternalId(User user, String dsmExternalId) {
        user.setDsmExternalId(dsmExternalId);
    }

    public void setUserType(User user, UserType type) {
        user.setUserType(type);
    }

    public void setTransportType(User user, TransportType transportType) {
        user.setTransportType(transportType);
    }

    public void setPhoneConfirmedAt(User user, Instant instant) {
        user.setPhoneConfirmedAt(instant);
    }

    public User of(long id, long uid, String name) {
        User user = new User();
        user.setId(id);
        user.setUid(uid);
        user.setName(name);
        return user;
    }

    public User withCompany(Company company) {
        User user = new User();
        user.setCompany(company);
        return user;
    }

    public User createUserWithoutSchedule(long uid) {
        return createUserWithoutSchedule(uid, null, null);
    }

}
