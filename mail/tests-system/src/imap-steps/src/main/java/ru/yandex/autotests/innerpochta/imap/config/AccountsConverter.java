package ru.yandex.autotests.innerpochta.imap.config;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;

import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 22.05.13
 * Time: 18:31
 */
public class AccountsConverter implements Converter {

    @Override
    public Object convert(Class aClass, Object o) {
        if (!(o instanceof String)) {
            return new HashMap<String, Account>();
        }
        String path = (String) o;
        try {
            String json = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(path));
            return new GsonBuilder().registerTypeAdapter(Account.class, new AccountDeserializer()).create()
                    .fromJson(json, new TypeToken<Map<String, Account>>() {
                    }.getType());
        } catch (IOException e) {
            LogManager.getLogger(this.getClass())
                    .warn("Can't load file, use default, because of: " + e.getMessage(), e);

            return new HashMap<String, Account>() {{
                put("default", new Account("default", "default"));
            }};
        }
    }

    private class AccountDeserializer implements JsonDeserializer<Account> {
        @Override
        public Account deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            Account account = new Account(
                    json.getAsJsonObject().get("login").getAsString(),
                    json.getAsJsonObject().get("pwd").getAsString()
            );

            if (json.getAsJsonObject().has("domain")) {
                account.domain(json.getAsJsonObject().get("domain").getAsString());
            }
            return account;
        }
    }
}
