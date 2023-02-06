package ru.yandex.market.logistics.iris.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PutApiControllerTest extends AbstractContextualTest {

    private final String REQUEST_ID = "TestRequestId";

    @Before
    public void init() {
        RequestContextHolder.createContext(REQUEST_ID);
    }

    /**
     * Тест на успешную проливку 2-х айтемов
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/put_reference_items/items.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/put_reference_items/items.xml", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "classpath:fixtures/expected/put_reference_items/queue_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessfullyPutTwoReferenceItems() throws Exception {
        httpOperationWithResult(
                post("/put-api/reference-items")
                        .content(extractFileContent("fixtures/controller/request/reference-items/put/1.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().isOk());
    }

    /**
     * Тест на проливку пустого списка айтемов
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/put_reference_items/items.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/put_reference_items/items.xml", assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotPutReferenceItemsIfInputListEmpty() throws Exception {
        httpOperationWithResult(
                post("/put-api/reference-items")
                        .content(extractFileContent("fixtures/controller/request/reference-items/put/2.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().isOk());
    }

    /**
     * Тест на проливку списка айтемов с дубликатами
     * - Дубликаты должны схопнуться
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/put_reference_items/items.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/put_reference_items/items.xml", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "classpath:fixtures/expected/put_reference_items/queue_tasks.xml", assertionMode = NON_STRICT_UNORDERED)
    public void shouldPutReferenceItemsWithoutDuplicate() throws Exception {
        httpOperationWithResult(
                post("/put-api/reference-items")
                        .content(extractFileContent("fixtures/controller/request/reference-items/put/3.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().isOk());
    }
}
