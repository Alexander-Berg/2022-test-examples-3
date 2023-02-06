package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.DecreaseConsolidatedSlotRequest;
import ru.yandex.market.logistics.calendaring.client.dto.DecreaseSlotRequest;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.util.FileContentUtils.getFileContent;

/**
 * Интеграционный тест для {@link RequestItemController}.
 *
 * @author avetokhin 17/01/18.
 */
class RequestItemControllerTest extends MvcIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String SUPPLIER_ID = "1";

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findItems() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/requests/1/items")
                .param("article", "abc")
                .param("page", "0")
                .param("size", "2")
                .param("sort", "id,asc")
        ).andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        assertResultExpected(result, "find-by-filter.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findItemsWithDefects() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/requests/1/items")
                .param("hasDefects", "true")
        ).andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        assertResultExpected(result, "find-with-defects.json");
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:controller/request-item-api/items.xml"),
            @DatabaseSetup("classpath:controller/request-item-api/zeroify-defect-counts.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml",
            assertionMode = NON_STRICT)
    void findItemsWithDefectsZeroify() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/requests/1/items")
                .param("hasDefects", "true")
        ).andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        assertResultExpected(result, "find-with-defects.json");
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:controller/request-item-api/items-with-anomaly.xml"),
            @DatabaseSetup("classpath:controller/request-item-api/zeroify-defect-counts.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items-with-anomaly.xml",
            assertionMode = NON_STRICT)
    void findItemsWithDefectsZeroifyAnomaly() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/requests/1/items")
                .param("hasDefects", "true")
        ).andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        assertResultExpected(result, "find-with-defects-zeroified.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findItemsWithShortage() throws Exception {
        MvcResult result = mockMvc.perform(
            get(("/suppliers/" + SUPPLIER_ID) + "/requests/1/items")
                .param("hasShortage", "1")
        ).andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        assertResultExpected(result, "find-with-shortage.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findItemsWithPlanOrFact() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests/2/items")
                        .param("hasPlanOrFact", "1")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertResultExpected(result, "find-with-plan-or-fact.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findSupplierItemsWithShortage() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/requests/1/items")
                .param("hasShortage", "true")
        ).andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        assertResultExpected(result, "find-supplier-items-with-shortage.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findAlienItemsWithDefects() throws Exception {
        mockMvc.perform(
            get("/suppliers/2/requests/1/items")
                .param("hasDefects", "true")
        ).andDo(print())
            .andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findItemsWithSurplus() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/suppliers/1/requests/1/items")
                .param("hasSurplus", "true")
        ).andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        assertResultExpected(result, "find-with-surplus.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findItemsWithAllProblems() throws Exception {
        MvcResult result = mockMvc.perform(
            get("/requests/1/items")
                .param("hasShortage", "true")
                .param("hasSurplus", "true")
                .param("hasDefects", "1")
        ).andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        assertResultExpected(result, "find-with-all-problems.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findItemsWithValidationErrors() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/items")
            .param("hasValidationErrors", "true"))
            .andReturn();
        assertResultExpected(result, "validation_error_items.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findByFullArticle() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/items")
            .param("article", "abcdefg"))
            .andReturn();
        assertResultExpected(result, "find-by-full-article.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findByFullArticleInDifferentCase() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/items")
            .param("article", "abcDefG"))
            .andReturn();
        assertResultExpected(result, "find-by-full-article.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findByPrefixOfArticle() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/items")
            .param("article", "abcde"))
            .andReturn();
        assertResultExpected(result, "find-by-prefix-of-article.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findByPrefixOfArticleInDifferentCase() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/items")
            .param("article", "aBcDE"))
            .andReturn();
        assertResultExpected(result, "find-by-prefix-of-article.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findBySuffixOfArticle() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/items")
            .param("article", "efg"))
            .andReturn();
        assertResultExpected(result, "find-by-suffix-of-article.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findBySuffixOfArticleInDifferentCase() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/items")
            .param("article", "EFG"))
            .andReturn();
        assertResultExpected(result, "find-by-suffix-of-article.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findBySubstringOfArticle() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/items")
            .param("article", "cd"))
            .andReturn();
        assertResultExpected(result, "find-by-substring-of-article.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findBySubstringOfArticleInDifferentCase() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/items")
            .param("article", "cD"))
            .andReturn();
        assertResultExpected(result, "find-by-substring-of-article.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items.xml", assertionMode = NON_STRICT)
    void findBySubstringOfArticleWithNullDefectCount() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/items")
                .param("article", "xyz"))
                .andReturn();
        assertResultExpected(result, "find-with-null-defects.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items-with-logistic-unit.xml")
    @ExpectedDatabase(value = "classpath:controller/request-item-api/items-with-logistic-unit.xml",
            assertionMode = NON_STRICT)
    void findItemsWithOrderFromLogisticUnit() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests/1/items")
        ).andExpect(status().isOk())
                .andReturn();
        assertResultExpected(result, "find-with-logistic-unit.json");
    }

    /**
     * В БД 3 айтема для заявки с артиклями: aaa, bbb, ccc
     *
     * В запросе указываем только 2 айтема:
     * - aaa с обновленным кол-вом
     * - bbb абсолютно идентичный исходному
     *
     * Проверяем, что:
     * - один айтем обновлен
     * - один айтем удален
     * - квота срезана
     * - окно срезано на пол часа
     * - shopRequest.totalCount обновлено
     */
    @Test
    @DatabaseSetup("classpath:controller/request-item-api/draft-before-update.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/draft-after-update-with-element-deleted.xml",
            assertionMode = NON_STRICT
    )
    void validUpdateWithDeletingItem() throws Exception {
        var result = mockMvc.perform(
                put("/requests/1/items")
                        .content(getRelativeFileContent("update-request-with-deleting-item.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertResultExpected(result, "successful-update-response.json");
        verifyNoMoreInteractions(csClient);
    }

    /**
     * В БД 3 айтема для заявки с артиклями: aaa, bbb, ccc
     *
     * В запросе указываем 3 айтема:
     * - aaa с обновленными кол-вом и ценой
     * - bbb абсолютно идентичный исходному
     * - ccc с обновленной ценой
     *
     * Проверяем, что:
     * - 2 айтема обновлены - aaa, ccc
     * - квота срезана
     * - окно осталось не тронутым
     * - shopRequest.totalCount обновлено
     */
    @Test
    @DatabaseSetup("classpath:controller/request-item-api/draft-before-update.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/draft-after-update-without-slot-updating.xml",
            assertionMode = NON_STRICT
    )
    void validUpdateWithoutChangingSlot() throws Exception {
        var result = mockMvc.perform(
                put("/requests/1/items")
                        .content(getRelativeFileContent("update-request-without-slot-changing.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertResultExpected(result, "successful-update-response.json");
        verifyZeroInteractions(csClient);
    }

    /**
     * В БД 3 айтема для заявки с артиклями:
     * - aaa, bbb, ccc
     * - нет ни квот/ни окна
     *
     * В запросе указываем 3 айтема:
     * - aaa с обновленными кол-вом и ценой
     * - bbb абсолютно идентичный исходному
     * - ccc с обновленной ценой
     *
     * Проверяем, что:
     * - 2 айтема обновлены - aaa, ccc
     * - shopRequest.totalCount обновлено
     */
    @Test
    @DatabaseSetup("classpath:controller/request-item-api/draft-before-update-without-taken-quota.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/draft-after-update-without-taken-quota.xml",
            assertionMode = NON_STRICT
    )
    void validUpdateBeforeSlotBooking() throws Exception {
        var result = mockMvc.perform(
                put("/requests/1/items")
                        .content(getRelativeFileContent("update-request-without-slot-changing.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertResultExpected(result, "successful-update-response.json");
        verifyZeroInteractions(csClient);
    }

    /**
     * Проверяем, что если в запрос адейта передать текущий стейт, то кроме shopRequest.updated_at ничего не изменится.
     */
    @Test
    @DatabaseSetup("classpath:controller/request-item-api/draft-before-update.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/draft-after-update-with-modified-updating-date.xml",
            assertionMode = NON_STRICT
    )
    void validUpdateWithoutAnyModifications() throws Exception {
        var result = mockMvc.perform(
                put("/requests/1/items")
                        .content(getRelativeFileContent("update-request-without-any-change.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertResultExpected(result, "successful-update-response.json");
        verifyZeroInteractions(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/draft-before-update-with-cs-booking.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/draft-after-update-with-cs-booking.xml",
            assertionMode = NON_STRICT
    )
    void validUpdateWithCsBookingAndChangingSlot() throws Exception {

        when(csClient.getSlot(200L)).thenReturn(new BookingResponseV2(
                200L,
            "FFWF",
                "1",
                null,
                1L,
                ZonedDateTime.of(2018, 1, 6, 9, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                BookingStatus.ACTIVE,
                LocalDateTime.of(1999, 9, 9, 1, 0, 0),
                1
        ));

        var result = mockMvc.perform(
                put("/requests/1/items")
                        .content(getRelativeFileContent("update-request-with-slot-changing.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertResultExpected(result, "successful-update-response.json");
        ArgumentCaptor<DecreaseSlotRequest> requestCaptor = ArgumentCaptor.forClass(DecreaseSlotRequest.class);
        verify(csClient).decreaseSlot(requestCaptor.capture());
        DecreaseSlotRequest value = requestCaptor.getValue();
        assertions.assertThat(value.getBookingId()).isEqualTo(200L);
        assertions.assertThat(value.getFrom()).isEqualTo(LocalDateTime.of(2018, 1, 6, 9, 0, 0));
        assertions.assertThat(value.getTo()).isEqualTo(LocalDateTime.of(2018, 1, 6, 9, 30, 0));
    }

    /**
     * В БД 3 айтема для заявки с артиклями: aaa, bbb, ccc
     *
     * В запросе указываем 3 айтема:
     * - aaa c кол-вом большим заданного
     * - ccc с обновленной ценой
     * - zzz - неизвестный для этой заявки
     *
     * Проверяем, что:
     * - заявка никак не изменилась
     * - в ответе список из 2 ошибок
     */
    @Test
    @DatabaseSetup("classpath:controller/request-item-api/draft-before-update.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/draft-before-update.xml",
            assertionMode = NON_STRICT
    )
    void invalidUpdate() throws Exception {
        var result = mockMvc.perform(
                put("/requests/1/items")
                        .content(getRelativeFileContent("invalid-update-request.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertResultExpected(result, "invalid-update-response.json");
        verifyZeroInteractions(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/before-update-with-different-request-types.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/before-update-with-different-request-types.xml",
            assertionMode = NON_STRICT
    )
    void tryToUpdateItemsForNonShadowSupplyRequestType() throws Exception {
        var result =  mockMvc.perform(
                put("/requests/2/items")
                        .content(getRelativeFileContent("update-request-without-slot-changing.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(result.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Request items updating is allowed only for SHADOW_SUPPLY request type.\"}"));
        verifyZeroInteractions(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/before-update-with-different-request-types.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/before-update-with-different-request-types.xml",
            assertionMode = NON_STRICT
    )
    void tryToUpdateItemsForShadowSupplyInCreatedStatus() throws Exception {
        var result =  mockMvc.perform(
                put("/requests/3/items")
                        .content(getRelativeFileContent("update-request-without-slot-changing.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(result.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Request items updating is prohibited for status: CREATED\"}"));
        verifyZeroInteractions(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/draft-before-update.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/draft-before-update.xml",
            assertionMode = NON_STRICT
    )
    void tryToUpdateItemsForShadowSupplyWithZeroItemCountAndPrice() throws Exception {
        var result =  mockMvc.perform(
                put("/requests/1/items")
                        .content(getRelativeFileContent("update-request-with-zero-item-count.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(result.getResponse().getContentAsString(),
                equalTo("{\"message\":\"items[1].count value must be greater than zero; " +
                        "items[2].price value must be greater than zero\"}"));
        verifyZeroInteractions(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/updating-request-before-valid.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/updating-request-after-valid.xml",
            assertionMode = NON_STRICT
    )
    void validUpdatingRequest() throws Exception {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 3");
        jdbcTemplate.execute("alter sequence request_item_id_seq restart with 5");

        var result =  mockMvc.perform(
                put("/requests/1/items-update")
                        .content(getRelativeFileContent("updating-request-valid-request.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        assertResultExpected(result, "updating-request-valid-response.json");
        verifyZeroInteractions(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/updating-request-before-valid.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/updating-request-after-items-to-delete.xml",
            assertionMode = NON_STRICT
    )
    void validUpdatingRequestItemsToDelete() throws Exception {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 3");
        jdbcTemplate.execute("alter sequence request_item_id_seq restart with 5");

        var result =  mockMvc.perform(
                put("/requests/1/items-update")
                        .content(getRelativeFileContent("updating-request-minimal-request.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        assertResultExpected(result, "updating-request-to-delete-response.json");
        verifyZeroInteractions(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/updating-request-before-with-completed.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/updating-request-after-with-completed.xml",
            assertionMode = NON_STRICT
    )
    void validUpdatingRequestWithCompleted() throws Exception {
        jdbcTemplate.execute("alter sequence shop_request_id_seq restart with 4");
        jdbcTemplate.execute("alter sequence request_item_id_seq restart with 9");
        jdbcTemplate.execute("alter sequence unit_identifier_id_seq restart with 7");

        var result =  mockMvc.perform(
                put("/requests/1/items-update")
                        .content(getRelativeFileContent("updating-request-valid-with-completed.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        assertResultExpected(result, "updating-request-with-completed-response.json");
        verifyZeroInteractions(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/updating-request-before-invalid-type.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/updating-request-before-invalid-type.xml",
            assertionMode = NON_STRICT
    )
    void invalidTypeUpdatingRequest() throws Exception {
        mockMvc.perform(
                put("/requests/1/items-update")
                        .content(getRelativeFileContent("updating-request-minimal-request.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
        verifyZeroInteractions(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/updating-request-before-invalid-status.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/updating-request-before-invalid-status.xml",
            assertionMode = NON_STRICT
    )
    void invalidStatusUpdatingRequest() throws Exception {
        var result =  mockMvc.perform(
                put("/requests/1/items-update")
                        .content(getRelativeFileContent("updating-request-minimal-request.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertResultExpected(result, "updating-request-invalid-status-response.json");
        verifyZeroInteractions(csClient);
    }


    @Test
    @DatabaseSetup("classpath:controller/request-item-api/updating-request-before-invalid-other-active.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/updating-request-before-invalid-other-active.xml",
            assertionMode = NON_STRICT
    )
    void invalidOtherActiveUpdatingRequest() throws Exception {
        var result =  mockMvc.perform(
                put("/requests/1/items-update")
                        .content(getRelativeFileContent("updating-request-minimal-request.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertResultExpected(result, "updating-request-invalid-other-request-response.json");
        verifyZeroInteractions(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/updating-request-before-valid.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/updating-request-before-valid.xml",
            assertionMode = NON_STRICT
    )
    void invalidItemCountUpdatingRequest() throws Exception {
        var result =  mockMvc.perform(
                put("/requests/1/items-update")
                        .content(getRelativeFileContent("updating-request-invalid-count-request.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertResultExpected(result, "updating-request-invalid-count-response.json");
        verifyZeroInteractions(csClient);
    }
    @Test
    @DatabaseSetup("classpath:controller/request-item-api/updating-request-before-valid.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/updating-request-before-valid.xml",
            assertionMode = NON_STRICT
    )
    void invalidItemUpdatingRequest() throws Exception {
        var result =  mockMvc.perform(
                put("/requests/1/items-update")
                        .content(getRelativeFileContent("updating-request-invalid-item-request.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertResultExpected(result, "updating-request-invalid-item-response.json");
        verifyZeroInteractions(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items-with-assortment.xml")
    void findItemsWithoutAssortment() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests/1/items")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertResultExpected(result, "find-with-assortment.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/consolidated-shipping-before-update.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/" +
                    "consolidated-shipping-after-update-without-slot-updating.xml",
            assertionMode = NON_STRICT
    )
    void validConsolidatedShippingUpdateWithoutChangingSlot() throws Exception {
        when(csClient.getBookingsByIdsV2(Set.of(201L, 202L, 203L), BookingStatus.ACTIVE))
                .thenReturn(new BookingListResponseV2(List.of(
                        new BookingResponseV2(
                                201,
                                "FFWF",
                                "1",
                                null,
                                10,
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 11, 0), ZoneId.of("+04:00")),
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 12, 0), ZoneId.of("+04:00")),
                                BookingStatus.ACTIVE,
                                LocalDateTime.of(2021, 1, 1, 9, 0, 0), 100L
                        ),
                        new BookingResponseV2(
                                202,
                                "FFWF",
                                "1",
                                null,
                                10,
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 11, 0), ZoneId.of("+04:00")),
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 12, 0), ZoneId.of("+04:00")),
                                BookingStatus.ACTIVE,
                                LocalDateTime.of(2021, 1, 1, 9, 0, 0), 100L
                        ),
                        new BookingResponseV2(
                                203,
                                "FFWF",
                                "1",
                                null,
                                10,
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 11, 0), ZoneId.of("+04:00")),
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 12, 0), ZoneId.of("+04:00")),
                                BookingStatus.ACTIVE,
                                LocalDateTime.of(2021, 1, 1, 9, 0, 0), 100L
                        )
                )));
        var result = mockMvc.perform(
                put("/requests/1/items")
                        .content(getRelativeFileContent("update-consolidated-shipping-without-slot-changes.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertResultExpected(result, "successful-update-response.json");
        Mockito.verify(csClient, never()).decreaseConsolidatedSlot(any());
        Mockito.verify(csClient, never()).decreaseSlot(any());
        Mockito.verify(csClient, never()).getSlot(anyLong());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/consolidated-shipping-before-update.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-item-api/consolidated-shipping-after-update-with-slot-updating.xml",
            assertionMode = NON_STRICT
    )
    void validConsolidatedShippingUpdateWithChangingSlot() throws Exception {
        when(csClient.getBookingsByIdsV2(Set.of(201L, 202L, 203L), BookingStatus.ACTIVE))
                .thenReturn(new BookingListResponseV2(List.of(
                        new BookingResponseV2(
                                201,
                                "FFWF",
                                "1",
                                null,
                                10,
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 11, 0), ZoneId.of("+04:00")),
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 12, 0), ZoneId.of("+04:00")),
                                BookingStatus.ACTIVE,
                                LocalDateTime.of(2021, 1, 1, 9, 0, 0), 100L
                        ),
                        new BookingResponseV2(
                                202,
                                "FFWF",
                                "1",
                                null,
                                10,
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 11, 0), ZoneId.of("+04:00")),
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 12, 0), ZoneId.of("+04:00")),
                                BookingStatus.ACTIVE,
                                LocalDateTime.of(2021, 1, 1, 9, 0, 0), 100L
                        ),
                        new BookingResponseV2(
                                203,
                                "FFWF",
                                "1",
                                null,
                                10,
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 11, 0), ZoneId.of("+04:00")),
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 12, 0), ZoneId.of("+04:00")),
                                BookingStatus.ACTIVE,
                                LocalDateTime.of(2021, 1, 1, 9, 0, 0), 100L
                        )
                )));
        var result = mockMvc.perform(
                put("/requests/1/items")
                        .content(getRelativeFileContent("update-consolidated-shipping-with-slot-changes.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertResultExpected(result, "successful-update-response.json");
        ArgumentCaptor<DecreaseConsolidatedSlotRequest> captor =
                ArgumentCaptor.forClass(DecreaseConsolidatedSlotRequest.class);

        Mockito.verify(csClient).decreaseConsolidatedSlot(captor.capture());
        assertions.assertThat(captor.getValue().getFrom()).isEqualTo(LocalDateTime.of(2018, 1, 6, 11, 0));
        assertions.assertThat(captor.getValue().getTo()).isEqualTo(LocalDateTime.of(2018, 1, 6, 11, 30));
        Mockito.verify(csClient, never()).decreaseSlot(any());
        Mockito.verify(csClient, never()).getSlot(anyLong());
    }

    private void assertResultExpected(MvcResult result, String filename) throws IOException {
        String expected = getRelativeFileContent(filename);
        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    private String getRelativeFileContent(String filename) throws IOException {
        return getFileContent("controller/request-item-api/" + filename);
    }
}
