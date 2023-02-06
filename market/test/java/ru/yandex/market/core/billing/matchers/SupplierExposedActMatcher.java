package ru.yandex.market.core.billing.matchers;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hamcrest.Matcher;

import ru.yandex.market.core.supplier.PartnerContractType;
import ru.yandex.market.core.supplier.model.ProductId;
import ru.yandex.market.core.supplier.model.SupplierExposedAct;
import ru.yandex.market.core.supplier.model.SupplierExposedActStatus;
import ru.yandex.market.mbi.util.MbiMatchers;

public class SupplierExposedActMatcher {

    public static Matcher<SupplierExposedAct> hasSupplierId(long expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getSupplierId, expectedValue, "supplier_id")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasActId(long expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getActId, expectedValue, "act id")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasExternalId(String expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getExternalId, expectedValue, "external id")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasActDate(LocalDate expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getActDate, expectedValue, "act date")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasContractId(long expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getContractId, expectedValue, "contract id")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasContractType(PartnerContractType expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getContractType, expectedValue, "contract type")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasContractEid(String expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getContractEid, expectedValue, "contract eid")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasAccountId(Long expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getAccountId, expectedValue, "account id")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasDeadlineDate(LocalDate expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getDeadlineDate, expectedValue, "deadline date")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasStatus(SupplierExposedActStatus expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getStatus, expectedValue, "status")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasPaidAmtRur(BigDecimal expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getPaidAmtRur, expectedValue, "paid amt rur")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasAmtRur(BigDecimal expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getAmtRur, expectedValue, "amt rur")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasAmtRurWithNds(BigDecimal expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getAmtRurWithNds, expectedValue, "amt rur with nds")
                .build();
    }

    public static Matcher<SupplierExposedAct> hasProductId(ProductId expectedValue) {
        return MbiMatchers.<SupplierExposedAct>newAllOfBuilder()
                .add(SupplierExposedAct::getProductId, expectedValue, "product id")
                .build();
    }
}
