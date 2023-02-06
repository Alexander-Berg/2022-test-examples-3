package ru.yandex.market.crm.core.test.utils;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.external.blackbox.YandexBlackboxClient;
import ru.yandex.market.crm.external.blackbox.YandexTeamBlackboxClient;
import ru.yandex.market.crm.external.blackbox.response.UserInfo;
import ru.yandex.market.crm.external.blackbox.response.UserInfo.Sex;

import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author apershukov
 */
@Component
public class BlackboxHelper {

    public static UserProfile profile(String email, Sex sex) {
        Address address = new Address()
                .setAddress(email)
                .setDefault(true);

        UserProfile profile = new UserProfile();
        profile.setAddresses(Collections.singletonList(address));
        profile.setSex(sex);

        return profile;
    }

    public static UserProfile profile(String email) {
        return profile(email, Sex.MALE);
    }

    private final YandexBlackboxClient yandexBlackboxClient;

    private final YandexTeamBlackboxClient yandexTeamBlackboxClient;

    public BlackboxHelper(YandexBlackboxClient yandexBlackboxClient,
                          YandexTeamBlackboxClient yandexTeamBlackboxClient) {
        this.yandexBlackboxClient = yandexBlackboxClient;
        this.yandexTeamBlackboxClient = yandexTeamBlackboxClient;
    }

    public void setUpResolveUserInfoByUid(long puid, UserProfile profile) {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail(profile.getAddresses()
                .stream()
                .filter(x -> x.isDefault)
                .map(Address::getAddress)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("no default address")));

        userInfo.setSex(profile.getSex());

        // TODO Сделать с этим что-нибудь. Пока вываливается из нашей концепции.
        when(yandexBlackboxClient.getUserInfoByUid(
                eq(puid),
                anySet(),
                anySet(),
                anySet()))
                .thenReturn(userInfo);
    }

    public void setUpResolveYandexTeamInfoByUid(long puid, String login) {
        UserInfo userInfo = new UserInfo();
        userInfo.setLogin(login);
        userInfo.setUid(puid);

        when(yandexTeamBlackboxClient.getUserInfoByUid(
                eq(puid),
                anySet(),
                anySet(),
                anySet()
        )).thenReturn(userInfo);
    }

    public void setUpResolveUserInfoByLogin(long puid, String login) {
        UserInfo userInfo = new UserInfo();
        userInfo.setLogin(login);
        userInfo.setUid(puid);

        when(yandexTeamBlackboxClient.getUserInfoByLogin(
                eq(login),
                anySet(),
                anySet(),
                anySet()
        )).thenReturn(userInfo);
    }

    private static class Address {

        @JsonProperty("address")
        private String address;

        @JsonProperty("default")
        private boolean isDefault;

        public String getAddress() {
            return address;
        }

        public Address setAddress(String address) {
            this.address = address;
            return this;
        }

        public boolean isDefault() {
            return isDefault;
        }

        public Address setDefault(boolean aDefault) {
            isDefault = aDefault;
            return this;
        }
    }

    public static class UserProfile {

        @JsonProperty("address-list")
        private List<Address> addresses;

        private Sex sex;

        public List<Address> getAddresses() {
            return addresses;
        }

        public void setAddresses(List<Address> addresses) {
            this.addresses = addresses;
        }

        public Sex getSex() {
            return sex;
        }

        public void setSex(Sex sex) {
            this.sex = sex;
        }
    }

    private static class Response {

        @JsonProperty("users")
        private List<UserProfile> users;

        Response(List<UserProfile> users) {
            this.users = users;
        }

        public Response() {
        }

        public List<UserProfile> getUsers() {
            return users;
        }

        public void setUsers(List<UserProfile> users) {
            this.users = users;
        }
    }
}
