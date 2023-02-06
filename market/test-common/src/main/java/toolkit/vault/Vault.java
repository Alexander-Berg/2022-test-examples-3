package toolkit.vault;

import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.Request;
import okhttp3.Response;
import toolkit.Mapper;
import toolkit.OkClient;
import toolkit.vault.model.Version;
import toolkit.vault.model.VersionWrapper;

public class Vault {
    private Vault() {
    }

    public static Version getSecretsValues(String token, String secretId) {
        Request request = new Request.Builder()
                .header("Authorization", token)
                .url("https://vault-api.passport.yandex.net/1/versions/" + secretId)
                .build();
        Response response = new OkClient(false).makeRequest(request);
        VersionWrapper versionWrapper = Mapper.mapResponse(response, new TypeReference<>() {
        });
        assert versionWrapper != null : "Нет такого секрета";
        return versionWrapper.getVersion();
    }
}
