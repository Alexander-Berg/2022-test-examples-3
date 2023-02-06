package ru.yandex.market.psku.postprocessor.health;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.MskuFromPskuGenResultStatus;

import java.util.HashSet;
import java.util.Set;

public class PskuProcessingTypeTest {

    @Test
    public void fromResultStatusParsesAllResultStatuses() {
        Set<PskuProcessingType> results = new HashSet<>();
        for (MskuFromPskuGenResultStatus status : MskuFromPskuGenResultStatus.values()) {
            results.add(PskuProcessingType.fromResultStatus(status));
        }
        Assertions.assertThat(results).doesNotContain(PskuProcessingType.UNRECOGNIZED_TYPE);
    }
}