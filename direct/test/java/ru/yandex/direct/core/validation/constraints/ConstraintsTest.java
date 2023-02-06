package ru.yandex.direct.core.validation.constraints;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;

import static ru.yandex.direct.core.validation.constraints.Constraints.validClientId;

public class ConstraintsTest {

    private static final Defect<Void> VALID_ID_DEFECT = CommonDefects.validId();

    @Test
    public void validClientId_success() {
        SoftAssertions.assertSoftly(soft -> {
            // Valid
            soft.assertThat(validClientId().apply(null))   // null - не дефект, на него отдельный констреинт
                    .as(("null -> null")).isEqualTo(null);
            soft.assertThat(validClientId().apply(ClientId.fromLong(1L)))
                    .as(("1L -> null")).isEqualTo(null);
            soft.assertThat(validClientId().apply(ClientId.fromLong(Long.MAX_VALUE)))
                    .as(("Long.MAX_VALUE -> null")).isEqualTo(null);

            // Not valid
            soft.assertThat(validClientId().apply(ClientId.fromLong(0)))
                    .as(("0 -> defect")).isEqualTo(VALID_ID_DEFECT);
            soft.assertThat(validClientId().apply(ClientId.fromLong(-1L)))
                    .as(("-1L -> defect")).isEqualTo(VALID_ID_DEFECT);
            soft.assertThat(validClientId().apply(ClientId.fromLong(Long.MIN_VALUE)))
                    .as(("Long.MIN_VALUE -> defect")).isEqualTo(VALID_ID_DEFECT);
        });
    }

}
