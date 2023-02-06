package ru.yandex.market.partner.content.common.csku.judge.review;

import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.judge.Decision;
import ru.yandex.market.partner.content.common.csku.judge.DeclineType;
import ru.yandex.market.partner.content.common.csku.judge.ParameterGenerator;

import static org.assertj.core.api.Assertions.assertThat;

public class OwnershipReviewTest {

    private static final Long MODEL_SUPPLIER_ID = 3L;
    private static final Long MODEL_ID = 5L;
    private static final int OFFER_SUPPLIER_ID = 2;
    private static final Long PARAM_OWNER_ID = 1L;

    @Test
    public void whenParamOwnerDifferentFromOfferSupplierThenDeny() {
        ModelStorage.ParameterValue parameterValue = ParameterGenerator.generateEmptyParam()
                .setOwnerId(PARAM_OWNER_ID)
                .build();
        OwnershipReview review = new OwnershipReview(
                parameterValue, OFFER_SUPPLIER_ID, OFFER_SUPPLIER_ID, MODEL_ID);
        Decision decision = review.conduct();
        assertThat(decision.isConflict()).isTrue();
        assertThat(decision.getReason().getType()).isEqualTo(DeclineType.OFFER_OWNER_IS_NOT_PARAM_OWNER);
    }

    @Test
    public void whenNoParamOwnerAndModelSupplierDifferentFromOfferSupplierThenDeny() {
        ModelStorage.ParameterValue parameterValue = ParameterGenerator.generateEmptyParam()
                .build();
        OwnershipReview review = new OwnershipReview(
                parameterValue, OFFER_SUPPLIER_ID, MODEL_SUPPLIER_ID, MODEL_ID);
        Decision decision = review.conduct();
        assertThat(decision.isConflict()).isTrue();
        assertThat(decision.getReason().getType()).isEqualTo(DeclineType.OFFER_OWNER_IS_NOT_PARAM_OWNER);
    }

    @Test
    public void whenNoParamOwnerAndModelSupplierEqualsOfferSupplierThenAllow() {
        ModelStorage.ParameterValue parameterValue = ParameterGenerator.generateEmptyParam()
                .build();
        OwnershipReview review = new OwnershipReview(
                parameterValue, OFFER_SUPPLIER_ID, OFFER_SUPPLIER_ID, MODEL_ID);
        Decision decision = review.conduct();
        assertThat(decision.isDenial()).isFalse();
    }

    @Test
    public void whenParamOwnerEqualsOfferSupplierThenAllow() {
        ModelStorage.ParameterValue parameterValue = ParameterGenerator.generateEmptyParam()
                .setOwnerId(OFFER_SUPPLIER_ID)
                .build();
        OwnershipReview review = new OwnershipReview(
                parameterValue, OFFER_SUPPLIER_ID, MODEL_SUPPLIER_ID, MODEL_ID);
        Decision decision = review.conduct();
        assertThat(decision.isDenial()).isFalse();
    }
}
