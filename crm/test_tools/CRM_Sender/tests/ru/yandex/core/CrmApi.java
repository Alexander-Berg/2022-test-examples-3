package ru.yandex.core;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import ru.yandex.core.crm.UserRow;
import ru.yandex.core.crm.UserStorage;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nasyrov on 18.04.2016.
 */
public class CrmApi {

    private String _authLogin;
    private UserRow _user;
    public static String lastJson;

    public CrmApi() {
        this._authLogin = Settings.get("auth.testUser");
    }

    public CrmApi(String _authLogin) {
        this._authLogin = _authLogin;
    }

    public UserRow user() throws Exception {
        if (_user == null) {
            _user = UserStorage.getUser(_authLogin);
        }
        return _user;
    }

    private static String buildPath(String path) {
        String uri = Settings.get("app.host");
        if (!uri.endsWith("/")) uri += "/";
        if (path.startsWith("/")) path = path.substring(1);
        return uri + path;
    }

    public String getString(String path, Map<String, Object> parameters) throws Exception {
        String json = Unirest
                .get(buildPath(path))
                .header("Cookie", "devcrm=" + _authLogin)
                .queryString(parameters)
                .asString()
                .getBody();
        //System.out.print(json);
        lastJson = json;

        return json;
    }

    public DocumentContext getData(String path, Map<String, Object> parameters) throws Exception {
        return JsonPath.parse(getString(path, parameters));
    }

    public DocumentContext getData(String path, Object... args) throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        for (int i = 0; i < args.length; i+=2) {
            parameters.put(args[i].toString(), args[i+1]);
        }
        return getData(path, parameters);
    }
}
