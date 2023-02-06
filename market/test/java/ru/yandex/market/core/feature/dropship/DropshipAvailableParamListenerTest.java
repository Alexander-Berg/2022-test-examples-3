package ru.yandex.market.core.feature.dropship;

import java.util.Optional;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

class DropshipAvailableParamListenerTest extends FunctionalTest {

    @Autowired
    private ParamService paramService;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void before() {
        Mockito.when(lmsClient.getPartner(Mockito.anyLong())).thenReturn(
                Optional.of(EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class).build()));
    }

    /**
     * Фича должна выключиться и стать в DONT_WANT.
     */
    @Test
    @DbUnitDataSet(before = "dropshipAvailableTest.before.csv", after = "dropshipAvailableTest.after.csv")
    void onChangeToFalseParamValue() {
        BooleanParamValue newParamValue = new BooleanParamValue(ParamType.DROPSHIP_AVAILABLE, 100L, false);
        paramService.setParam(newParamValue, 111L);
    }

    /**
     * Ничего не должно поменяться, дефолтный статус DONT_WANT.
     */
    @Test
    @DbUnitDataSet(before = "dropshipAvailableTest.before.csv", after = "dropshipAvailableToTrueTest.after.csv")
    void onChangeToTrueParamValue() {
        BooleanParamValue newParamValue = new BooleanParamValue(ParamType.DROPSHIP_AVAILABLE, 101L, true);
        paramService.setParam(newParamValue, 111L);
    }

    /**
     * Feature должна стать SUCCESS, открывается катоф эксперимента.
     * Параметр {@link ParamType#DROPSHIP_AVAILABLE} устанавлен в true.
     */
    @Test
    @DbUnitDataSet(before = "dropshipAvailableTest.before.csv", after = "onChangeNewToTrueParamValue.after.csv")
    void onChangeNewToTrueParamValue() {
        BooleanParamValue newParamValue = new BooleanParamValue(ParamType.DROPSHIP_AVAILABLE, 102L, true);
        paramService.setParam(newParamValue, 111L);
    }

    /**
     * Магазину нельзя включить фичу, должен стать статус фичи FAIL.
     */
    @Test
    @DbUnitDataSet(before = "dropshipAvailableShopTest.before.csv", after = "dropshipAvailableShopTest.after.csv")
    void onChangeShopParamValue() {
        BooleanParamValue newParamValue = new BooleanParamValue(ParamType.DROPSHIP_AVAILABLE, 103L, true);
        paramService.setParam(newParamValue, 111L);
    }
}
