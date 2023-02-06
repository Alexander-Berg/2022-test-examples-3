package ru.yandex.market.logistics.iris.controller;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.index.field.Field;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFieldProvider;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TrustworthyInfoControllerTest extends AbstractContextualTest {

    private final static String URL = "/trustworthy_values";

    @MockBean
    private PredefinedFieldProvider provider;

    @Before
    public void prepare() {
        mockPredefinedFieldProvider();
    }

    /**
     * Тест проверяет сохранение семантики null/отсутствующего значения атрибутов.
     * В бд имеются 2 пары товаров, пара - один и тот же товар (partner_id и partner_sku) на разных складах.
     * <p>
     * Проверяются кейсы:
     * - nullable поле, с null в наиболее приоритетном источнике - будет взят null
     * - у поля разные значения для разных источников - будет взято более приоритетное
     * - не nullable поле, у приоритетного null, у неприоритетного - не null, будет взято менее приоритетное значение
     * - nullable поле остуствтует на обоих источниках - отсутствуепт в ответе
     * - nullable поле отсутствует на приоритетном и null на неприоритетном - будет null
     * - не nullable поле имеет null у приоритетного склада и отсутствует у другого - отсутствует в ответе
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/trustworthy_info/1.xml")
    public void accessHandleForMultipleItems() throws Exception {
        httpOperationWithResult(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "fixtures/controller/request/trustworthy/get-dummy-trustworthy-info-request.json")),
            status().isOk(),
            content().json(extractFileContent(
                "fixtures/controller/response/trustworthy/get-dummy-trustworthy-info-response.json")));
    }

    /**
     * Тест проверяет корректность работы отдачи полей размеров и веса брутто при разных кейсах:
     * - (sku1) у приоритетного источника нет length - должно взяться из другого, значение для веса - null,
     * также берется из другого
     * - (sku3) у первого источника есть отрицательное значение размеров, у второго - нулевое - не должно попасть в результат.
     * Отрицательный вес первого источника также не должен там оказаться.
     * - (sku5) проверяем, что приоритеты работают
     *
     * @throws Exception
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/trustworthy_info/2.xml")
    public void dimensionsArePresentFor() throws Exception {
        httpOperationWithResult(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "fixtures/controller/request/trustworthy/get-with-dimensions-trustworthy-info-request.json")),
            status().isOk(),
            content().json(extractFileContent(
                "fixtures/controller/response/trustworthy/get-with-dimensions-trustworthy-info-response.json"), true));
    }

    /**
     * Тест проверяет, что возвращаются данные только по запрошенным полям.
     *
     * @throws Exception
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/trustworthy_info/3.xml")
    public void onlyRequestedFieldsAreReturned() throws Exception {
        httpOperationWithResult(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "fixtures/controller/request/trustworthy/get-with-not-empty-fields-trustworthy-info-request.json")),
            status().isOk(),
            content().json(extractFileContent(
                "fixtures/controller/response/trustworthy/get-with-not-empty-fields-trustworthy-info-response.json")));
    }

    /**
     * Тест проверяет, что если не указаны запрашиваемые поля, то возвращаются данные по всем возможным полям.
     *
     * @throws Exception
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/trustworthy_info/3.xml")
    public void allFieldsAreReturnedForEmptyFieldArray() throws Exception {
        httpOperationWithResult(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "fixtures/controller/request/trustworthy/get-with-empty-fields-trustworthy-info-request.json")),
            status().isOk(),
            content().json(extractFileContent(
                "fixtures/controller/response/trustworthy/get-with-empty-fields-trustworthy-info-response.json")));
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/trustworthy_info/4.xml")
    public void hasLifetimeReturnedIfContentDataIsNewerThanMdm() throws Exception {
        httpOperationWithResult(
                post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extractFileContent(
                                "fixtures/controller/request/trustworthy/get-with-empty-fields-trustworthy-info-request.json")),
                status().isOk(),
                content().json(extractFileContent(
                        "fixtures/controller/response/trustworthy/get-with-has-lifetime-field-returned.json")));
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/trustworthy_info/5.xml")
    public void hasLifetimeNotReturnedIfMdmDataIsNewerThanContent() throws Exception {
        httpOperationWithResult(
                post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extractFileContent(
                                "fixtures/controller/request/trustworthy/get-with-empty-fields-trustworthy-info-request.json")),
                status().isOk(),
                content().json(extractFileContent(
                        "fixtures/controller/response/trustworthy/get-with-empty-fields-trustworthy-info-response.json")));
    }


    private void mockPredefinedFieldProvider() {

        when(provider.find("dimensions")).thenReturn(Optional.of(PredefinedFields.DIMENSIONS));
        when(provider.find("weight_gross")).thenReturn(Optional.of(PredefinedFields.WEIGHT_GROSS));
        when(provider.find("barcodes")).thenReturn(Optional.of(PredefinedFields.BARCODES));
        when(provider.find("box_count")).thenReturn(Optional.of(PredefinedFields.BOX_COUNT_FIELD));
        when(provider.find("box_capacity")).thenReturn(Optional.of(PredefinedFields.BOX_CAPACITY_FIELD));
        when(provider.find("has_lifetime")).thenReturn(Optional.of(PredefinedFields.HAS_LIFETIME_FIELD));

        when(provider.get("dimensions")).then(invocation -> PredefinedFields.DIMENSIONS);
        when(provider.get("weight_gross")).then(invocation -> PredefinedFields.WEIGHT_GROSS);
        when(provider.get("barcodes")).then(invocation -> PredefinedFields.BARCODES);
        when(provider.get("box_count")).then(invocation -> PredefinedFields.BOX_COUNT_FIELD);
        when(provider.get("box_capacity")).then(invocation -> PredefinedFields.BOX_CAPACITY_FIELD);
        when(provider.get("has_lifetime")).then(invocation -> PredefinedFields.HAS_LIFETIME_FIELD);

        ImmutableMap<String, Field<?>> allFields = ImmutableMap.<String, Field<?>>builder()
                .put("dimensions", PredefinedFields.DIMENSIONS)
                .put("weight_gross", PredefinedFields.WEIGHT_GROSS)
                .put("barcodes", PredefinedFields.BARCODES)
                .put("box_count", PredefinedFields.BOX_COUNT_FIELD)
                .put("box_capacity", PredefinedFields.BOX_CAPACITY_FIELD)
                .put("has_lifetime", PredefinedFields.HAS_LIFETIME_FIELD)
                .build();
        when(provider.getFields()).thenReturn(allFields);
    }
}
