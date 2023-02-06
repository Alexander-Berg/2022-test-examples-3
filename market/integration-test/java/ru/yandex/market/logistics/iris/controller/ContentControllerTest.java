package ru.yandex.market.logistics.iris.controller;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.jobs.consumers.sync.ContentSyncService;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ContentControllerTest extends AbstractContextualTest {

    @MockBean
    private ContentSyncService contentSyncService;


    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/content/sync/1.xml")
    public void syncExistingItem() throws Exception {
        when(contentSyncService.doSyncManually(anySet())).thenAnswer(
                (Answer<Set<ItemIdentifier>>) invocation -> invocation.getArgument(0));

        httpOperationWithResult(
                post("/content/sync")
                        .content(extractFileContent("fixtures/controller/request/content/sync/ok.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().isOk(),
                content().json(extractFileContent("fixtures/controller/response/content/sync/ok.json")));
    }

    @Test
    public void invalidRequest() throws Exception {
        httpOperationWithResult(
                post("/content/sync")
                        .content(extractFileContent("fixtures/controller/request/content/sync/empty_items.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().is4xxClientError());

        httpOperationWithResult(
                post("/content/sync")
                        .content(extractFileContent(
                                "fixtures/controller/request/content/sync/invalid_item_reference.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().is4xxClientError());
    }

    @Test
    public void syncNonExistingItem() throws Exception {
        when(contentSyncService.doSyncManually(anySet())).thenAnswer(
                (Answer<Set<ItemIdentifier>>) invocation -> invocation.getArgument(0));

        httpOperationWithResult(
                post("/content/sync")
                        .content(extractFileContent("fixtures/controller/request/content/sync/ok.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().isOk(),
                content().json(extractFileContent("fixtures/controller/response/content/sync/item_does_not_exist.json")));
    }
}
