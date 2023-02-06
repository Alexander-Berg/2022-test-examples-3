package ru.yandex.autotests.innerpochta.util.props;

import com.google.gson.Gson;
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * User: lanwen
 * Date: 29.08.13
 * Time: 15:37
 */
public class GroupAccountsConverter implements Converter {

    @Override
    public Object convert(Class aClass, Object o) {
        if (!(o instanceof String)) throw new IllegalStateException("Property with account files is not string!");

        List<String> paths = split((String) o);
        Map<String, Map<String, Account>> accGroups = new HashMap<>();

        for (String path : paths) {
            try {
                if (this.getClass().getClassLoader().getResourceAsStream(path) != null) {
                    String json = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(path));
                    Gson gson = new GsonBuilder().registerTypeAdapter(Account.class, new AccountDeserializer())
                        .create();
                    Type type = new TypeToken<Map<String, Account>>() {}.getType();
                    Map<String, Account> nextMap = gson.fromJson(json, type);
                    accGroups.put(path, nextMap);
                }
            } catch (IOException e) {
                LogManager.getLogger(this.getClass())
                    .warn("Can't load file, use default, because of: " + e.getMessage(), e);
                accGroups.put(path, new HashMap<String, Account>() {{
                    put("default", new Account("default", "default"));
                }});
            }
        }
        return accGroups;
    }

    private List<String> split(String paths) {
        return asList(paths.split(","));
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
