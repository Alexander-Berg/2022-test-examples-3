package ru.yandex.market.cashier.mocks.trust.checkers;

import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.Matcher;

import ru.yandex.market.cashier.model.PassParams;

public class CreateTrustOrderParams {
    private final Integer agencyCommission;
    private final String serviceProductId;
    private final String developerPayload;
    private final PassParams passParams;
    private final String serviceOrderId;
    private final Date orderCreateDate;

    public CreateTrustOrderParams(
            @Nullable Integer agencyCommission,
            @Nonnull String serviceProductId,
            @Nonnull String developerPayload,
            @Nonnull PassParams passParams,
            @Nonnull String serviceOrderId,
            @Nullable Date orderCreateDate) {
        this.agencyCommission = agencyCommission;
        this.serviceProductId = serviceProductId;
        this.developerPayload = developerPayload;
        this.passParams = passParams;
        this.serviceOrderId = serviceOrderId;
        this.orderCreateDate = orderCreateDate;
    }

    public Matcher<JsonElement> toJsonMatcher() {
        return new AbstractMatcher(this.toString()) {
            @Override
            protected boolean matchObjectFields(JsonObject object) {
                return matchPropertiesSize(object, agencyCommission, serviceOrderId, developerPayload, serviceProductId, orderCreateDate)
                        && matchValue(object, "commission_category", agencyCommission)
                        && matchValue(object, "product_id", serviceProductId)
                        && matchValue(object, "developer_payload", developerPayload)
                        && matchValue(object, "pass_params", passParams)
                        && matchValue(object, "order_id", serviceOrderId)
                        && matchDateValue(object, "start_ts", orderCreateDate);
            }
        };
    }

    @Override
    public String toString() {
        return "CreateTrustOrderParams{" +
                "agencyCommission=" + agencyCommission +
                ", serviceProductId='" + serviceProductId + '\'' +
                ", developerPayload='" + developerPayload + '\'' +
                ", passParams='" + passParams + '\'' +
                ", serviceOrderId='" + serviceOrderId + '\'' +
                ", orderCreateDate=" + orderCreateDate +
                '}';
    }
}
