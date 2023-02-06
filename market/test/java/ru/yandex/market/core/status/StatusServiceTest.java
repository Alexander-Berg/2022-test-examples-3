package ru.yandex.market.core.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;

import static org.assertj.core.api.Assertions.assertThat;

class StatusServiceTest {
    private static final long SHOP_ID = 774L;

    /**
     * Проверяем, что когда
     * <ul>
     * <li>У магазина должен работать CPC, т. е нет катофов, т. е. (IS_ENABLED=true)
     * <li>Магазин загружен в индекс
     * <li>Магазин не отключён динамиком от всех программ
     * </ul>
     * то статус включён
     */
    @Test
    void testEnabled() {
        MultiMap<ParamType, ParamValue> params = new MultiMap<>();
        params.append(ParamType.IS_CPC_ENABLED, new BooleanParamValue(ParamType.IS_CPC_ENABLED, SHOP_ID, true));
        params.append(ParamType.IS_IN_INDEX, new BooleanParamValue(ParamType.IS_IN_INDEX, SHOP_ID, true));

        var rootStates = StatusServiceImpl.getCampaignStates(params, Map.of(), null);

        assertThat(rootStates).containsExactly(1); // включён
    }

    /**
     * Проверяем, что когда
     * <ul>
     * <li>У магазина должен работать CPC, т. е нет катофов, т. е. (IS_ENABLED=true)
     * <li>Магазин не загружен в индекс
     * <li>Магазин не отключён динамиком от всех программ
     * </ul>
     * то статус включается
     */
    @Test
    void testEnablingWhenIsNotInIndex() {
        MultiMap<ParamType, ParamValue> params = new MultiMap<>();
        params.append(ParamType.IS_CPC_ENABLED, new BooleanParamValue(ParamType.IS_CPC_ENABLED, SHOP_ID, true));
        params.append(ParamType.IS_IN_INDEX, new BooleanParamValue(ParamType.IS_IN_INDEX, SHOP_ID, false));

        var rootStates = StatusServiceImpl.getCampaignStates(params, Map.of(), null);

        assertThat(rootStates).containsExactly(3); // включается
    }

    /**
     * Проверяем, что когда
     * <ul>
     * <li>У магазина не должен работать CPC, т. е есть катофы, т. е. (IS_ENABLED=false)
     * <li>Магазин загружен в индекс
     * <li>Магазин не отключён динамиком от всех программ
     * <li>Магазин не отключён динамиком от CPC программ
     * </ul>
     * то статус выключается
     */
    @Test
    void testDisablingWhenNotFilteredFromAll() {
        MultiMap<ParamType, ParamValue> params = new MultiMap<>();
        params.append(ParamType.IS_CPC_ENABLED, new BooleanParamValue(ParamType.IS_CPC_ENABLED, SHOP_ID, false));
        params.append(ParamType.IS_IN_INDEX, new BooleanParamValue(ParamType.IS_IN_INDEX, SHOP_ID, true));

        var rootStates = StatusServiceImpl.getCampaignStates(params, Map.of(), null);

        assertThat(rootStates).containsExactly(4); // выключается
    }

    /**
     * Проверяем, что когда
     * <ul>
     * <li>У магазина не должен работать CPC, т. е есть катофы, т. е. (IS_ENABLED=false)
     * <li>Магазин загружен в индекс
     * <li>Магазин не отключён динамиком от всех программ
     * <li>Магазин отключён динамиком от CPC
     * </ul>
     * то статус выключен
     */
    @Test
    void testDisabledWhenFilteredFromCPC() {
        MultiMap<ParamType, ParamValue> params = new MultiMap<>();
        params.append(ParamType.IS_CPC_ENABLED, new BooleanParamValue(ParamType.IS_CPC_ENABLED, SHOP_ID, false));
        params.append(ParamType.IS_IN_INDEX, new BooleanParamValue(ParamType.IS_IN_INDEX, SHOP_ID, true));

        var rootStates = StatusServiceImpl.getCampaignStates(params, Map.of(
                CutoffType.MODIFIED, new CutoffInfo(111, CutoffType.MODIFIED)
        ), null);

        assertThat(rootStates).containsExactly(2); // выключен
    }

    /**
     * Проверяем, что когда
     * <ul>
     * <li>У магазина не должен работать CPC, есть катоф CPC_FINANCE_LIMIT, т. е. (IS_ENABLED=false)
     * <li>Магазин загружен в индекс
     * <li>Магазин не отключён динамиком от всех программ
     * <li>Магазин отключён динамиком от CPC
     * </ul>
     * то статус выключен
     */
    @Test
    void testDisabledFilteredFromCPC() {
        MultiMap<ParamType, ParamValue> params = new MultiMap<>();
        params.append(ParamType.IS_CPC_ENABLED, new BooleanParamValue(ParamType.IS_CPC_ENABLED, SHOP_ID, false));
        params.append(ParamType.IS_IN_INDEX, new BooleanParamValue(ParamType.IS_IN_INDEX, SHOP_ID, true));

        var rootStates = StatusServiceImpl.getCampaignStates(params, Map.of(
                CutoffType.CPC_FINANCE_LIMIT, new CutoffInfo(111, CutoffType.CPC_FINANCE_LIMIT)
        ), null);

        assertThat(rootStates).containsExactlyInAnyOrder(
                2, // выключен
                26 // есть катоф CPC_FINANCE_LIMIT
        );
    }
}
