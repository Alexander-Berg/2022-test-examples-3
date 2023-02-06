package steps.shopSteps;

import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.mbi.api.client.entity.shops.ShopOrgInfo;

public class OrganizationInfoSteps {
    private static final String TYPE = "other";
    private static final String OGRN = "1023500000160";
    private static final String URL = "url";
    private static final String NAME = "name";
    private static final String FACT_ADDRESS = "FACT_ADDRESS";
    private static final String JURIDICAL_ADDRESS = "JURIDICAL_ADDRESS";
    private static final String INFO_SOURCE = "INFO_SOURCE";
    private static final String REGISTRATION_NUMBER = "REGISTRATION_NUMBER";
    private static final String INFO_URL = "INFO_URL";

    private OrganizationInfoSteps() {
    }

    public static List<ShopOrgInfo> getOrganizationInfo() {
        return Collections.singletonList(new ShopOrgInfo(
            TYPE,
            OGRN,
            NAME,
            FACT_ADDRESS,
            JURIDICAL_ADDRESS,
            INFO_SOURCE,
            REGISTRATION_NUMBER,
            INFO_URL
        ));
    }

    static JSONArray getOrganizationInfosJson() throws JSONException {
        JSONArray organizationArray = new JSONArray();
        JSONObject organizationInfos = new JSONObject();

        organizationInfos.put("type", TYPE);
        organizationInfos.put("ogrn", OGRN);
        organizationInfos.put("url", URL);
        organizationInfos.put("name", NAME);
        organizationInfos.put("factAddress", FACT_ADDRESS);
        organizationInfos.put("juridicalAddress", JURIDICAL_ADDRESS);
        organizationInfos.put("infoSource", INFO_SOURCE);
        organizationInfos.put("registrationNumber", REGISTRATION_NUMBER);
        organizationInfos.put("infoUrl", INFO_URL);

        organizationArray.put(organizationInfos);
        return organizationArray;
    }
}
