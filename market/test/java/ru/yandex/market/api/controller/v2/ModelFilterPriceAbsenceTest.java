package ru.yandex.market.api.controller.v2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v1.ModelInfoField;
import ru.yandex.market.api.domain.v2.ModelResult;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.util.concurrent.ApiDeferredResult;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by vdorogin on 18.05.17.
 */
public class ModelFilterPriceAbsenceTest extends BaseTest {

    @Inject
    ModelsControllerV2 controller;
    @Inject
    ReportTestClient reportTestClient;

    /**
     * Проверка отсутствия фильтра "Цена" в v2/models/{id}: гуру модель
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3566">MARKETAPI-3566: Выпиливание фильтра "Цена"</a>
     */
    @Test
    public void priceFilterAbsenceInGuru() {
        long id = 13953515L;
        // настройка системы
        reportTestClient.getModelInfoById(id, "modelinfo_guru_13953515.json");
        reportTestClient.getModelOffers(id, "productoffers_guru_13953515.json");
        checkPriceAbsence(id);
    }

    /**
     * Проверка отсутствия фильтра "Цена" в v2/models/{id}: групповая модель
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3566">MARKETAPI-3566: Выпиливание фильтра "Цена"</a>
     */
    @Test
    public void priceFilterAbsenceInGroup() {
        long id = 11007864L;
        // настройка системы
        reportTestClient.getModelInfoById(id, "modelinfo_group_11007864.json");
        reportTestClient.getModelModifications(id, "model_modifications_group_11007864.json");
        checkPriceAbsence(id);
    }

    /**
     * Проверка отсутствия фильтра "Цена" в v2/models/{id}: модификация
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3566">MARKETAPI-3566: Выпиливание фильтра "Цена"</a>
     */
    @Test
    public void priceFilterAbsenceInModification() {
        long id = 10972776L;
        // настройка системы
        reportTestClient.getModelInfoById(id, "modelinfo_modif_10972776.json");
        reportTestClient.getModelModifications(10972706L, "model_modifications_modif_10972776.json");
        checkPriceAbsence(id);
    }

    private void checkPriceAbsence(long id) {
        // вызов системы
        ModelResult result = ((ApiDeferredResult<ModelResult>) controller.getModel(
                id,
                Arrays.asList(ModelInfoField.FILTERS),
                Collections.emptyMap(),
                genericParams,
                false,
                null
        )).waitResult();
        // проверка утверждений
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getModel());
        Assert.assertFalse(((ModelV2) result.getModel()).getFilters().stream()
                .anyMatch(f -> "Цена".equals(f.getName())));
    }
}
