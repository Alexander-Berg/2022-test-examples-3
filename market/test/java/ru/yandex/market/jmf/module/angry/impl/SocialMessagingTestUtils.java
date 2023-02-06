package ru.yandex.market.jmf.module.angry.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.angry.SmmAccount;
import ru.yandex.market.jmf.module.angry.SmmObject;
import ru.yandex.market.jmf.module.angry.SocialMessagingComment;
import ru.yandex.market.jmf.module.angry.controller.v1.model.AccountObject;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;

import static ru.yandex.market.jmf.utils.serialize.SerializationConfiguration.JMF_OBJECT_MAPPER;

@Service
public class SocialMessagingTestUtils {
    private final ObjectMapper objectMapper;
    private final EntityStorageService storage;
    private final BcpService bcpService;
    private final TxService txService;

    public SocialMessagingTestUtils(@Named(JMF_OBJECT_MAPPER) ObjectMapper objectMapper,
                                    EntityStorageService storage,
                                    BcpService bcpService,
                                    TxService txService) {
        this.objectMapper = objectMapper;
        this.storage = storage;
        this.bcpService = bcpService;
        this.txService = txService;
    }

    public List<JsonNode> getSmmObjectsListFromFile(String path) throws IOException {
        var messagesData = ResourceHelpers.getResource(path);

        var tree = objectMapper.readTree(messagesData);
        if (!tree.isArray()) {
            return List.of(tree);
        }
        var list = new ArrayList<JsonNode>();
        for (JsonNode node : tree) {
            list.add(node);
        }

        return list;
    }


    public JsonNode getFirstSmmObjectFromFile(String path) throws IOException {
        return getSmmObjectsListFromFile(path).get(0);
    }

    @Transactional
    public SmmAccount getAccount(long accountId) {
        return storage.getByNaturalId(SmmAccount.FQN, accountId);
    }

    @Transactional
    public SmmAccount createSmmAccount(AccountObject accountObject, MockAngrySpaceClient mockAngrySpaceClient) {
        mockAngrySpaceClient.setupGetAccount(accountObject);
        // поля должны заполниться @fillSmmAccountFields
        SmmAccount createdSmmAccount = bcpService.create(SmmAccount.FQN, Maps.of(
                SmmAccount.ACCOUNT_ID, accountObject.getId()));

        Assertions.assertEquals(accountObject.getId(), createdSmmAccount.getAccountId());
        Assertions.assertEquals(accountObject.getProvider().name(), createdSmmAccount.getProvider().getCode());
        if (accountObject.getUrl() == null) {
            Assertions.assertNull(createdSmmAccount.getGroupPageUrl());
        } else {
            Assertions.assertEquals(accountObject.getUrl(), createdSmmAccount.getGroupPageUrl().getHref());
        }
        Assertions.assertEquals(accountObject.getName(), createdSmmAccount.getTitle());

        return createdSmmAccount;
    }

    @Transactional
    public List<SmmObject> getCreatedSmmObjects(Fqn fqn) {
        Query q = Query.of(fqn)
                .withSortingOrder(SortingOrder.desc(SmmObject.CREATION_TIME));
        return storage.list(q);
    }

    @Transactional
    public SocialMessagingComment getComment(SmmObject smmObject) {
        List<SocialMessagingComment> comments = storage.list(Query.of(SocialMessagingComment.FQN)
                .withFilters(Filters.eq(SocialMessagingComment.SMM_OBJECT, smmObject)));
        Assertions.assertEquals(1, comments.size());
        return comments.get(0);
    }


}
