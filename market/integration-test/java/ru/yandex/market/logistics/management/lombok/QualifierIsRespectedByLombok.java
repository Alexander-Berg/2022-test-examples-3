package ru.yandex.market.logistics.management.lombok;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;

public class QualifierIsRespectedByLombok extends AbstractContextualTest {

    @Autowired
    DuplicatingClassConfig.EnclosingClass enclosingClass;

    @Test
    void notSame() {
        softly.assertThat(enclosingClass.getInstanceA().property).isEqualTo("primary");
        softly.assertThat(enclosingClass.getInstanceB().property).isEqualTo("qualifier");
    }


}
