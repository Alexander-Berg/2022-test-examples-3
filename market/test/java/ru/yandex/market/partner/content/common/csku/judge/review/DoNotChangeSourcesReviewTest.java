package ru.yandex.market.partner.content.common.csku.judge.review;

import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.judge.Decision;
import ru.yandex.market.partner.content.common.csku.judge.DeclineType;
import ru.yandex.market.partner.content.common.csku.judge.ParameterGenerator;

import static org.assertj.core.api.Assertions.assertThat;

public class DoNotChangeSourcesReviewTest {

    @Test
    public void whenOneOfDoNotChangeSourcesThenDeny() {
        ModelStorage.ParameterValue parameterValue = ParameterGenerator.generateEmptyParam()
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build();
        DoNotChangeSourcesReview review = new DoNotChangeSourcesReview(
                parameterValue, 11L
        );
        Decision decision = review.conduct();
        assertThat(decision.isDenial()).isTrue();
        assertThat(decision.getReason().getType()).isEqualTo(DeclineType.UNMODIFIABLE_SOURCE);
    }

    @Test
    public void whenNoneOfDoNotChangeSourcesThenAllow() {
        ModelStorage.ParameterValue parameterValue = ParameterGenerator.generateEmptyParam()
                .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                .build();
        DoNotChangeSourcesReview review = new DoNotChangeSourcesReview(
                parameterValue, 11L
        );
        Decision decision = review.conduct();
        assertThat(decision.isDenial()).isFalse();
        assertThat(decision.getReason()).isNull();
    }

    @Test
    public void whenNoSourceThenAllow() {
        ModelStorage.ParameterValue parameterValue = ParameterGenerator.generateEmptyParam()
                .build();
        DoNotChangeSourcesReview review = new DoNotChangeSourcesReview(
                parameterValue, 11L
        );
        Decision decision = review.conduct();
        assertThat(decision.isDenial()).isFalse();
        assertThat(decision.getReason()).isNull();
    }
}
