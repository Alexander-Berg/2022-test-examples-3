package ru.yandex.direct.core.entity.retargeting.service.validation2;

import ru.yandex.direct.core.entity.retargeting.service.validation2.cpmprice.RetargetingConditionsCpmPriceValidationData;
import ru.yandex.direct.core.entity.retargeting.service.validation2.cpmprice.RetargetingConditionsCpmPriceValidationDataFactory;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockServices {

    static RetargetingConditionsCpmPriceValidationDataFactory emptyRetargetingConditionsCpmPriceValidationDataFactory() {
        RetargetingConditionsCpmPriceValidationDataFactory dataFactory =
                mock(RetargetingConditionsCpmPriceValidationDataFactory.class);
        RetargetingConditionsCpmPriceValidationData data =
                new RetargetingConditionsCpmPriceValidationData(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap());

        when(dataFactory.createForAddRetargetingsWithNonExistentAdGroups(anyInt(), any())).thenReturn(data);
        when(dataFactory.createForUpdateRetargetingConditions(anyInt(), any())).thenReturn(data);
        when(dataFactory.createForAddRetargetingsWithExistentAdGroups(anyInt(), any())).thenReturn(data);
        when(dataFactory.createForUpdateRetargetings(anyInt(), any())).thenReturn(data);

        return dataFactory;
    }
}
