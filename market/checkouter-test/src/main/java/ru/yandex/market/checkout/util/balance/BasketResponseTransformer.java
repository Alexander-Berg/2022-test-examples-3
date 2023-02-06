package ru.yandex.market.checkout.util.balance;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.checkout.checkouter.balance.trust.model.BalancePaymentStatus;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public class BasketResponseTransformer extends ResponseDefinitionTransformer {

    public static final String BASKET_STATE_RESPONSE = "basketStateResponse";
    public static final String RESPONSE_STATUS = "status"; //default status if not defined in config
    public static final String CONFIG = "StubConfig";

    public static final long DEFAULT_TERMINAL_ID = 123789456;
    private static final Logger LOG = LoggerFactory.getLogger(BasketResponseTransformer.class);


    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("dd-MM-yyyy HH:mm:ss")
            .setPrettyPrinting().create();


    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files,
                                        Parameters parameters) {
        CheckBasketParams trustConfig = (CheckBasketParams) parameters.get(CONFIG);
        if (trustConfig == null) {
            LOG.warn("{} in transformerParams is null", CONFIG);
            trustConfig = new CheckBasketParams();
        }

        UriComponents uri = UriComponentsBuilder.fromHttpUrl(request.getAbsoluteUrl()).build();
        String purchaseToken = StringUtils.substringBefore(StringUtils.substringAfterLast(uri.getPath(),
                "/payments/"), "/");

        purchaseToken = defaultIfBlank(purchaseToken, TrustMockConfigurer.generateRandomTrustId());
        BalancePaymentStatus status = trustConfig.getBasketStatus() == null ?
                BalancePaymentStatus.valueOf(parameters.getString(RESPONSE_STATUS)) :
                trustConfig.getBasketStatus();


        return new ResponseDefinitionBuilder()
                .withStatus(responseDefinition.getStatus())
                .withBody(GSON.toJson(buildResponse(trustConfig, status, purchaseToken)))
                .withHeader("Content-type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .build();
    }

    private JsonObject buildResponse(CheckBasketParams trustConfig, BalancePaymentStatus status, String purchaseToken) {
        // можно сделать предварительное чтение из файла
        JsonObject object = new JsonObject();
        object.addProperty("status", "success");
        object.addProperty("payment_status", status.name());
        object.addProperty("payment_resp_code", trustConfig.getStatusCode());
        object.addProperty("payment_resp_desc", trustConfig.getStatusDesc());

        object.addProperty("purchase_token", purchaseToken);
        object.addProperty("trust_payment_id", defaultIfBlank(trustConfig.getTrustPaymentId(),
                TrustMockConfigurer.generateRandomTrustId()));
        object.addProperty("payment_method", trustConfig.getPayMethod());
        object.addProperty("paymethod_id", trustConfig.getPayMethodId());
        object.addProperty("user_account", defaultIfBlank(trustConfig.getBankCard(), "500000****0009"));
        if (status == BalancePaymentStatus.cleared || status == BalancePaymentStatus.refunded) {
            object.addProperty("clear_real_ts", "1480012966674");
        }
        if (status == BalancePaymentStatus.authorized) {
            object.addProperty("payment_ts",
                    trustConfig.getHoldTimestamp() != null ? trustConfig.getHoldTimestamp() : "1480012966675");
        }
        if (!trustConfig.isEmptyPaymentUrl()) {
            object.addProperty("payment_url", "https://trust.yandex" +
                    ".ru/web/payment?purchase_token=290dcf726200095c35628d152a9e0f5b");
        }
        if (trustConfig.getReversalId() != null) {
            object.addProperty("reversal_id", trustConfig.getReversalId());
        }
        object.add("orders", buildOrders(trustConfig));
        object.add("refunds", buildRefunds(trustConfig));
        object.add("terminal", buildTerminal(trustConfig));
        object.add("primary_terminal", buildTerminal(trustConfig));
        if (trustConfig.getUnholdTimestamp() != null) {
            object.addProperty("unhold_ts", trustConfig.getUnholdTimestamp());
            object.addProperty("unhold_real_ts", trustConfig.getUnholdTimestamp());
        }
        if (trustConfig.getCashbackAmount() != null) {
            object.addProperty("cashback_amount", trustConfig.getCashbackAmount());
        }
        String cardType = trustConfig.getCardType();
        if (StringUtils.isNotBlank(cardType)) {
            object.addProperty("card_type", cardType);
        }
        return object;
    }

    private JsonObject buildTerminal(CheckBasketParams trustConfig) {
        JsonObject element = new JsonObject();
        if (trustConfig.getTerminal() == null) {
            element.addProperty("id", DEFAULT_TERMINAL_ID);
        } else {
            element.addProperty("id", trustConfig.getTerminal().getId());
        }
        return element;
    }

    private JsonArray buildRefunds(CheckBasketParams trustConfig) {
        if (trustConfig.getRefunds() == null) {
            return null;
        }

        JsonArray array = new JsonArray();
        trustConfig.getRefunds().forEach(l -> array.add(transformLine(l)));
        return array;
    }

    private JsonArray buildOrders(CheckBasketParams trustConfig) {
        if (trustConfig.getLines() == null) {
            return null;
        }

        JsonArray array = new JsonArray();
        trustConfig.getLines().forEach(l -> array.add(transformLine(l)));
        return array;
    }

    private JsonObject transformLine(CheckBasketParams.BasketLineState l) {
        JsonObject element = new JsonObject();
        element.addProperty("order_id", l.getOrderId());
        element.addProperty("current_qty", l.getQuantity());
        element.addProperty("paid_amount", l.getAmount());
        if (l.getDeliveryReceiptId() != null) {
            element.addProperty("delivery_id", l.getDeliveryReceiptId());
        }
        return element;
    }

    private JsonObject transformLine(CheckBasketParams.BasketRefund l) {
        JsonObject element = new JsonObject();

        element.addProperty("trust_refund_id", defaultIfBlank(l.getRefundId(),
                TrustMockConfigurer.generateRandomTrustId()));
        element.addProperty("amount", l.getAmount());
        if (l.isCancelled()) {
            element.addProperty("cancel_ts", "1480012966674");
        }
        if (l.isConfirmed()) {
            element.addProperty("confirm_ts", "1480012966674");
        }
        return element;
    }

    @Override
    public String getName() {
        return BASKET_STATE_RESPONSE;
    }
}
