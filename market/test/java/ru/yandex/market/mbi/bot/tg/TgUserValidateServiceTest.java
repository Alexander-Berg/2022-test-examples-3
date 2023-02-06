package ru.yandex.market.mbi.bot.tg;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.bot.FunctionalTest;
import ru.yandex.market.mbi.bot.tg.service.TgUserValidateService;
import ru.yandex.market.notification.telegram.bot.model.dto.ValidateUserAuthInfoResponse;

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class TgUserValidateServiceTest extends FunctionalTest {

    private static final Map<String, String> AUTH_VALUES = new HashMap<>();

    @Autowired
    private TgUserValidateService userValidateService;

    @BeforeAll
    public static void beforeClass() {
        AUTH_VALUES.put("id", "100500");
        AUTH_VALUES.put("first_name", "First$name");
        AUTH_VALUES.put("last_name", "Фамилия");
        AUTH_VALUES.put("username", "user_name");
        AUTH_VALUES.put("photo_url", "https://t.me/i/userpic/320/somephoto.jpg");
        AUTH_VALUES.put("auth_date", "1584621280");
        // новые поля
        AUTH_VALUES.put("my_chat_member", "member");
        AUTH_VALUES.put("new_chat_participant", "participant");
    }

    @Test
    public void testAuthWithAllFields() {
        HashMap<String, String> params = new HashMap<>(AUTH_VALUES);
        params.put("hash", "a79477fae1078f3e72e673bbd8b704ea4f8f6fe1ad810ec16d376fc5eb4871aa");

        String template = "{ " +
                "    id: $(id), " +
                "    first_name: '$(first_name)', " +
                "    last_name: '$(last_name)', " +
                "    username: '$(username)', " +
                "    photo_url: '$(photo_url)', " +
                "    auth_date: $(auth_date), " +
                "    hash: '$(hash)' " +
                "} ";

        String json = StrSubstitutor.replace(template, params, "$(", ")");

        ValidateUserAuthInfoResponse tgAccount = userValidateService.validateAuthInfo(json).orElseThrow();

        assertThat(String.valueOf(tgAccount.getTelegramId()), is(AUTH_VALUES.get("id")));
        assertThat(tgAccount.getFirstName(), is(AUTH_VALUES.get("first_name")));
        assertThat(tgAccount.getLastName(), is(AUTH_VALUES.get("last_name")));
        assertThat(tgAccount.getUsername(), is(AUTH_VALUES.get("username")));
        assertThat(tgAccount.getPhotoUrl(), is(AUTH_VALUES.get("photo_url")));
    }

    @Test
    public void testAuthWithoutOptionalFields() {
        HashMap<String, String> params = new HashMap<>(AUTH_VALUES);
        params.put("hash", "102ca1c521a1a18e05cf92466e3492171eab895d03c84563d66b76e427da8ae4");

        String template = "{ " +
                "    id: $(id), " +
                "    auth_date: $(auth_date), " +
                "    hash: '$(hash)' " +
                "} ";

        String json = StrSubstitutor.replace(template, params, "$(", ")");

        ValidateUserAuthInfoResponse tgAccount = userValidateService.validateAuthInfo(json).orElseThrow();

        assertThat(String.valueOf(tgAccount.getTelegramId()), is(AUTH_VALUES.get("id")));
        assertNull(tgAccount.getFirstName());
        assertNull(tgAccount.getLastName());
        assertNull(tgAccount.getUsername());
        assertNull(tgAccount.getPhotoUrl());
    }

    @Test
    public void testAuthWithoutFirstName() {
        HashMap<String, String> params = new HashMap<>(AUTH_VALUES);
        params.put("hash", "ddb4966d253c4fa089ad731399ba32da16040e90ddb8bcc4b0a87270dada6af3");

        String template = "{ " +
                "    id: $(id), " +
                "    last_name: '$(last_name)', " +
                "    username: '$(username)', " +
                "    photo_url: '$(photo_url)', " +
                "    auth_date: $(auth_date), " +
                "    hash: '$(hash)' " +
                "} ";

        String json = StrSubstitutor.replace(template, params, "$(", ")");

        ValidateUserAuthInfoResponse tgAccount = userValidateService.validateAuthInfo(json).orElseThrow();

        assertThat(String.valueOf(tgAccount.getTelegramId()), is(AUTH_VALUES.get("id")));
        assertNull(tgAccount.getFirstName());
        assertThat(tgAccount.getLastName(), is(AUTH_VALUES.get("last_name")));
        assertThat(tgAccount.getUsername(), is(AUTH_VALUES.get("username")));
        assertThat(tgAccount.getPhotoUrl(), is(AUTH_VALUES.get("photo_url")));
    }

    @Test
    public void testAuthWithoutLastName() {
        HashMap<String, String> params = new HashMap<>(AUTH_VALUES);
        params.put("hash", "e53b1b4d8ace873be38435003616f96d213ff7e91ce89b0d1bdcff6a66cd2783");

        String template = "{ " +
                "    id: $(id), " +
                "    first_name: '$(first_name)', " +
                "    username: '$(username)', " +
                "    photo_url: '$(photo_url)', " +
                "    auth_date: $(auth_date), " +
                "    hash: '$(hash)' " +
                "} ";

        String json = StrSubstitutor.replace(template, params, "$(", ")");

        ValidateUserAuthInfoResponse tgAccount = userValidateService.validateAuthInfo(json).orElseThrow();

        assertThat(String.valueOf(tgAccount.getTelegramId()), is(AUTH_VALUES.get("id")));
        assertThat(tgAccount.getFirstName(), is(AUTH_VALUES.get("first_name")));
        assertNull(tgAccount.getLastName());
        assertThat(tgAccount.getUsername(), is(AUTH_VALUES.get("username")));
        assertThat(tgAccount.getPhotoUrl(), is(AUTH_VALUES.get("photo_url")));
    }

    @Test
    public void testAuthWithoutUsername() {
        HashMap<String, String> params = new HashMap<>(AUTH_VALUES);
        params.put("hash", "cd5a5dc40efa1a8a2d2a498b85fe880fabaa4e9d23f73b16f1287e74ed274ae9");

        String template = "{ " +
                "    id: $(id), " +
                "    first_name: '$(first_name)', " +
                "    last_name: '$(last_name)', " +
                "    photo_url: '$(photo_url)', " +
                "    auth_date: $(auth_date), " +
                "    hash: '$(hash)' " +
                "} ";

        String json = StrSubstitutor.replace(template, params, "$(", ")");

        ValidateUserAuthInfoResponse tgAccount = userValidateService.validateAuthInfo(json).orElseThrow();

        assertThat(String.valueOf(tgAccount.getTelegramId()), is(AUTH_VALUES.get("id")));
        assertThat(tgAccount.getFirstName(), is(AUTH_VALUES.get("first_name")));
        assertThat(tgAccount.getLastName(), is(AUTH_VALUES.get("last_name")));
        assertNull(tgAccount.getUsername());
        assertThat(tgAccount.getPhotoUrl(), is(AUTH_VALUES.get("photo_url")));
    }

    @Test
    public void testAuthWithoutPhotoUrl() {
        HashMap<String, String> params = new HashMap<>(AUTH_VALUES);
        params.put("hash", "c7cdb2c83fdd099bb60c1111638374d58611ce3b72ff107d3bbccb3754bc919f");

        String template = "{ " +
                "    id: $(id), " +
                "    first_name: '$(first_name)', " +
                "    last_name: '$(last_name)', " +
                "    username: '$(username)', " +
                "    auth_date: $(auth_date), " +
                "    hash: '$(hash)' " +
                "} ";

        String json = StrSubstitutor.replace(template, params, "$(", ")");

        ValidateUserAuthInfoResponse tgAccount = userValidateService.validateAuthInfo(json).orElseThrow();

        assertThat(String.valueOf(tgAccount.getTelegramId()), is(AUTH_VALUES.get("id")));
        assertThat(tgAccount.getFirstName(), is(AUTH_VALUES.get("first_name")));
        assertThat(tgAccount.getLastName(), is(AUTH_VALUES.get("last_name")));
        assertThat(tgAccount.getUsername(), is(AUTH_VALUES.get("username")));
        assertNull(tgAccount.getPhotoUrl());
    }
}
