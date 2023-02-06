package ru.yandex.market.partner.content.common.csku.judge.review;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.judge.Decision;

public class ParamMergeReviewTest {

    public static long PARAM_ID = 1L;
    public static long BASE_OWNER_ID = 2L;
    public static long INCOMING_OWNER_ID = 3L;
    public static long MODEL_ID = 4L;

    @Test
    public void whenSameOwnerThenAllow() {
        ModelStorage.ParameterValue basePV = ModelStorage.ParameterValue.newBuilder()
                .setParamId(PARAM_ID)
                .setOwnerId(BASE_OWNER_ID)
                .build();
        ModelStorage.ParameterValue incomingPV = ModelStorage.ParameterValue.newBuilder()
                .setParamId(PARAM_ID)
                .build();
        ParamMergeReview review = new ParamMergeReview(basePV, BASE_OWNER_ID, incomingPV, BASE_OWNER_ID, MODEL_ID);
        Decision decision = review.conduct();
        assertThat(decision.isModify()).isTrue();
    }

    @Test
    public void whenDiffOwnerAndOnlyIncomingIsOperatorFilledThenAllow() {
        ModelStorage.ParameterValue basePV = ModelStorage.ParameterValue.newBuilder()
                .setParamId(PARAM_ID)
                .setOwnerId(BASE_OWNER_ID)
                .build();
        ModelStorage.ParameterValue incomingPV = ModelStorage.ParameterValue.newBuilder()
                .setParamId(PARAM_ID)
                .setOwnerId(INCOMING_OWNER_ID)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build();
        ParamMergeReview review = new ParamMergeReview(basePV, BASE_OWNER_ID, incomingPV, BASE_OWNER_ID, MODEL_ID);
        Decision decision = review.conduct();
        assertThat(decision.isModify()).isTrue();
    }

    @Test
    public void whenDiffOwnerAndBothAreOperatorFilledThenDeny() {
        ModelStorage.ParameterValue basePV = ModelStorage.ParameterValue.newBuilder()
                .setParamId(PARAM_ID)
                .setOwnerId(BASE_OWNER_ID)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build();
        ModelStorage.ParameterValue incomingPV = ModelStorage.ParameterValue.newBuilder()
                .setParamId(PARAM_ID)
                .setOwnerId(INCOMING_OWNER_ID)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build();
        ParamMergeReview review = new ParamMergeReview(basePV, BASE_OWNER_ID, incomingPV, BASE_OWNER_ID, MODEL_ID);
        Decision decision = review.conduct();
        assertThat(decision.isDenial()).isTrue();
    }
}
