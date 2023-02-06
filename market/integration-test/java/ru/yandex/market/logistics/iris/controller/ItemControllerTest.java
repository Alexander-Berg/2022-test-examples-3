package ru.yandex.market.logistics.iris.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ItemControllerTest extends AbstractContextualTest {
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_controller/1.xml")
    public void getItemThatExists() throws Exception {
        httpOperationWithResult(
            get("/item")
                .param("partner_id", "partner_id")
                .param("partner_sku", "partner_sku")
                .param("source_type", "admin")
                .param("source_id", "1"),
            status().isOk(),
            content().json(extractFileContent("fixtures/controller/response/item/get-item.json"))
        );
    }

    @Test
    public void getItemThatDoesNotExist() throws Exception {
        httpOperationWithResult(
            get("/item")
                .param("partner_id", "partner_id")
                .param("partner_sku", "partner_sku")
                .param("source_type", "admin")
                .param("source_id", "1"),
            status().isNotFound(),
            content().string(isEmptyString())
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_controller/2.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_controller/2.xml", assertionMode = NON_STRICT_UNORDERED)
    public void setFieldSuccess() throws Exception {
        httpOperationWithResult(
            patch("/item/1/lifetime")
                .content("{\"value\": 100,\"utcTimestamp\": \"2016-01-23T12:34:56\"}")
                .contentType(MediaType.APPLICATION_JSON),
            status().isOk()
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_controller/2.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_controller/3.xml", assertionMode = NON_STRICT_UNORDERED)
    public void setNullableFieldSuccess() throws Exception {
        httpOperationWithResult(
            patch("/item/1/yummy")
                .content("{\"value\": null,\"utcTimestamp\": \"2016-01-23T12:34:56\"}")
                .contentType(MediaType.APPLICATION_JSON),
            status().isOk()
        );
    }

    @Test
    public void setFieldToNonExistingItem() throws Exception {
        httpOperationWithResult(
            patch("/item/1/lifetime")
                .content("{\"value\": 100}")
                .contentType(MediaType.APPLICATION_JSON),
            status().isNotFound()
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_controller/3.xml")
    public void setFieldToDifferentSource() throws Exception {
        httpOperationWithResult(
            patch("/item/1/lifetime")
                .content("{\"value\": 100}")
                .contentType(MediaType.APPLICATION_JSON),
            status().isForbidden()
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_controller/2.xml")
    public void setFieldThatIsNotInPredefinedFieldsMap() throws Exception {
        httpOperationWithResult(
            patch("/item/1/unknownFieldName")
                .content("{\"value\": 100}")
                .contentType(MediaType.APPLICATION_JSON),
            status().isBadRequest(),
            content().string("Field [unknownFieldName] cannot be set")
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_controller/2.xml")
    public void setEmptyValue() throws Exception {
        httpOperationWithResult(
            patch("/item/1/lifetime")
                .content("")
                .contentType(MediaType.APPLICATION_JSON),
            status().isBadRequest(),
            content().string(startsWith("Failed to create field value object from json []"))
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_controller/4.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_controller/4.xml", assertionMode = NON_STRICT_UNORDERED)
    public void deleteFieldSuccess() throws Exception {
        httpOperationWithResult(
            delete("/item/1/lifetime"),
            status().isOk()
        );
    }

    @Test
    public void deleteFieldOfNonExistingItem() throws Exception {
        httpOperationWithResult(
            delete("/item/1/lifetime"),
            status().isNotFound()
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_controller/3.xml")
    public void deleteFieldOfDifferentSource() throws Exception {
        httpOperationWithResult(
            delete("/item/1/lifetime"),
            status().isForbidden()
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_controller/4.xml")
    public void deleteFieldThatIsNotInPredefinedFieldsMap() throws Exception {
        httpOperationWithResult(
            delete("/item/1/unknownFieldName"),
            status().isBadRequest(),
            content().string("Field [unknownFieldName] cannot be set")
        );
    }

    @Test
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_controller/5.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createNewItem() throws Exception {
        httpOperationWithResult(
            post("/item")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("fixtures/controller/request/item/create-item.json")),
            status().isOk(),
            content().json(extractFileContent("fixtures/controller/response/item/create-item.json"))
        );
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_controller/5.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_controller/5.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createItemThatAlreadyExists() throws Exception {
        httpOperationWithResult(
            post("/item")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("fixtures/controller/request/item/create-item.json")),
            status().isOk(),
            content().json(extractFileContent("fixtures/controller/response/item/create-item.json"))
        );
    }
}
