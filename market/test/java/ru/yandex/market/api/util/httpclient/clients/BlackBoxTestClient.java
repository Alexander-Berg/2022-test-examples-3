package ru.yandex.market.api.util.httpclient.clients;

import com.google.common.base.Strings;
import org.springframework.stereotype.Service;

import ru.yandex.market.api.util.ApiStrings;
import ru.yandex.market.api.util.Urls;

/**
 * Created by tesseract on 09.03.17.
 */
@Service
public class BlackBoxTestClient extends AbstractFixedConfigurationTestClient {

    public BlackBoxTestClient() {
        super("BlackBox");
    }

    public void getUsersInfo(long yandexUid, String resource) {
        configure(x -> x
            .get()
            .param("method", "userinfo")
            .param("uid", String.valueOf(yandexUid)))
            .ok()
            .body(resource);
    }

    public void postUserByOAuth(String oauth, String resource) {
        configure(x -> x
            .post()
            .param("method", "oauth")
            .header("Authorization", "OAuth " + oauth)
        )
            .ok()
            .body(resource);
    }

    public void postUserBySessionId(String host, String sessionId, String resource) {
        configure(
            x -> x.post()
                .param("method", "sessionid")
                .param("host", host)
                .body(b -> checkUrlDecoded(b, "sessionid", sessionId),
                    "sessionid is'" + sessionId + "'")
        ).ok().body(resource);
    }

    //TODO обобщить и вынести из TestClient
    private boolean checkUrlDecoded(byte [] input, String name, String value) {
        String url = ApiStrings.valueOf(input);
        if (Strings.isNullOrEmpty(url)) {
            return false;
        }

        //Цикл по параметрам
        for (String kv: url.split("&")) {
            if (!kv.startsWith(name + "=")) {
                //Неподходящее имя параметра
                continue;
            }

            String v = kv.substring(name.length() + 1);
            String decoded = Urls.decodeSafe(v);

            if (value.equals(decoded)) {
                //Имя и значения совпали
                return true;
            }
        }

        return false;
    }
}
