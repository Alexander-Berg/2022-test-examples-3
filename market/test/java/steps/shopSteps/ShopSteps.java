package steps.shopSteps;

import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.mbi.api.client.entity.shops.ProgramState;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;
import ru.yandex.market.mbi.api.client.entity.shops.ShopOrgInfo;

public class ShopSteps {

    private static final long ID = 123L;
    private static final String NAME = "name";
    private static final long LOCAL_DELIVERY_REGION_ID = 213L;
    private static final String PHONE_NUMBER = "+71234567890";
    private static final ProgramState CPC = ProgramState.OFF;
    private static final ProgramState CPA = ProgramState.OFF;
    private static final boolean IS_GLOBAL = false;
    private static final boolean ALLOWED_TO_START_CONVERSATION = true;
    private static final Shop.CampaignDetails CAMPAIGN_DETAILS = new Shop.CampaignDetails(
        1000553068L,
        16931555L,
        1015L
    );
    private static final String LOCAL_DELIVERY_SOURCE = "WEB";

    private ShopSteps() {
    }

    public static JSONObject getShopJson() throws JSONException {
        JSONObject shopJson = new JSONObject();

        shopJson.put("id", ID);
        shopJson.put("name", NAME);
        shopJson.put("shopName", NAME);
        shopJson.put("localDeliveryRegionId", LOCAL_DELIVERY_REGION_ID);
        shopJson.put("phoneNumber", PHONE_NUMBER);
        shopJson.put("marketDeliveryServiceSettingsList", Collections.emptyList());
        shopJson.put("organizationInfos", OrganizationInfoSteps.getOrganizationInfosJson());
        shopJson.put("cpc", CPC);
        shopJson.put("cpa", CPA);
        shopJson.put("cpaState", CPA);
        shopJson.put("global", IS_GLOBAL);
        shopJson.put("allowedToStartConversation", ALLOWED_TO_START_CONVERSATION);
        shopJson.put("isGlobal", IS_GLOBAL);
        shopJson.put("paymentStatus", PaymentStatusSteps.getPaymentStatusJson());
        shopJson.put("paymentInfo", PaymentStatusSteps.getPaymentStatusJson());
        shopJson.put("campaignDetails", getCampaignDetailsJson());
        shopJson.put("localDeliverySource", LOCAL_DELIVERY_SOURCE);

        return shopJson;
    }

    private static JSONObject getCampaignDetailsJson() throws JSONException {
        JSONObject object = new JSONObject();

        object.put("id", 1000553068L);
        object.put("clientId", 16931555L);
        object.put("tariffId", 1015L);

        return object;
    }

    public static Shop getDefaultShop() {
        return getDefaultShop(ID);
    }

    public static Shop getDefaultShop(long id) {
        return getDefaultShop(id, LOCAL_DELIVERY_REGION_ID);
    }

    public static Shop getDefaultShop(long id, Long localDeliveryRegionId) {
        return getDefaultShop(
            id,
            OrganizationInfoSteps.getOrganizationInfo(),
            PaymentStatusSteps.getPaymentStatus(),
            localDeliveryRegionId
        );
    }

    public static Shop getDefaultShop(
        long id,
        List<ShopOrgInfo> orgInfos,
        Shop.PaymentStatus paymentStatus
    ) {
        return getDefaultShop(id, orgInfos, paymentStatus, LOCAL_DELIVERY_REGION_ID);
    }

    public static Shop getDefaultShop(
        long id,
        List<ShopOrgInfo> orgInfos,
        Shop.PaymentStatus paymentStatus,
        Long localDeliveryRegionId
    ) {
        return new Shop(
            id,
            NAME,
            NAME,
            localDeliveryRegionId,
            PHONE_NUMBER,
            orgInfos,
            CPC,
            CPA,
            IS_GLOBAL,
            ALLOWED_TO_START_CONVERSATION,
            paymentStatus,
            CAMPAIGN_DETAILS,
            LOCAL_DELIVERY_SOURCE,
            false
        );
    }
}
