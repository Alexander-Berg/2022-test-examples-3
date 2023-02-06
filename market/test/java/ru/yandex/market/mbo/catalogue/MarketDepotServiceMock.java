package ru.yandex.market.mbo.catalogue;

import org.apache.commons.lang3.NotImplementedException;
import ru.yandex.common.framework.pager.Pager;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.catalogue.errors.ValidationError;
import ru.yandex.market.mbo.catalogue.model.CustomAttribute;
import ru.yandex.market.mbo.catalogue.model.MarketEntity;
import ru.yandex.market.mbo.catalogue.model.MarketEntityVisitor;
import ru.yandex.market.mbo.catalogue.model.PublishLevel;
import ru.yandex.market.mbo.catalogue.templates.MarketPropertyTemplate;
import ru.yandex.market.mbo.core.kdepot.api.Entity;
import ru.yandex.market.mbo.core.kdepot.api.JoinCondition;
import ru.yandex.market.mbo.core.kdepot.api.KnowledgeDepotService;
import ru.yandex.market.mbo.core.kdepot.api.KnowledgeDepotServiceMock;
import ru.yandex.market.mbo.core.kdepot.api.Link;
import ru.yandex.market.mbo.core.kdepot.api.SearchCondition;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author s-ermakov
 */
public class MarketDepotServiceMock implements MarketDepotService {

    private final KnowledgeDepotService knowledgeDepotService;

    public MarketDepotServiceMock() {
        this(new KnowledgeDepotServiceMock());
    }

    public MarketDepotServiceMock(KnowledgeDepotService knowledgeDepotService) {
        this.knowledgeDepotService = knowledgeDepotService;
    }

    @Override
    public List<String> getAttribute(long entityId, String name, long viewType) {
        List<String> attributes = knowledgeDepotService.getAttribute(entityId, name);

        // like the original method do
        if (attributes.isEmpty()) {
            attributes = new ArrayList<>();
            attributes.add("");
        }
        return attributes;
    }

    @Override
    public Map<String, List<String>> getAttributes(long entityId, long viewType) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<CustomAttribute> getCustomAttributes(long entityId, long viewType) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<ValidationError> setAttributes(long userId, long entityId, long viewType,
                                               Map<String, List<String>> values, AuditAction.BillingOptions options) {
        for (String key : values.keySet()) {
            knowledgeDepotService.setAttribute(entityId, key, values.get(key));
        }
        return Collections.emptyList();
    }

    @Override
    public List<ValidationError> setAttributes(long userId, long entityId,
                                               Map<String, MarketPropertyTemplate> templates,
                                               Map<String, List<String>> values) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<ValidationError> setAttributes(long userId, long entityId,
                                               Map<String, MarketPropertyTemplate> templates,
                                               Map<String, List<String>> values, AuditAction.BillingOptions options) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public long createEntity(long userId, long entityTypeId, long viewType, Map<String, List<String>> values) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public long createEntity(long userId, long entityTypeId, long parentId, long linkType, long viewType,
                             Map<String, List<String>> values) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public long createEntity(long userId, long entityTypeId, long parentId, long linkType, long viewType,
                             Map<String, List<String>> values, Consumer<Long> newEntityHandler) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public long createEntity(long userId, long entityTypeId, List<Pair<Long, Long>> linkTypeToParentIdList,
                             long viewTypeId, Map<String, List<String>> attributes, Consumer<Long> newEntityHandler) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public void deleteEntity(long userId, long entityId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public Map<String, MarketPropertyTemplate> getPropertyTemplates(Long entityTypeId, Long viewType) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public Map<String, MarketPropertyTemplate> getCreationPropertyTemplates(Long entityTypeId, Long viewType) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public String getFormBlock(long entityTypeId, long viewTypeId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public MarketEntity getEntity(long entityId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public long getEntityType(long entityId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public void setPublishLevel(long userId, long entityId, PublishLevel level) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public void setPublished(long userId, long entityId, boolean published) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<MarketEntity> search(List<SearchCondition> conditions) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<MarketEntity> search(List<SearchCondition> conditions, List<JoinCondition> joins) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<MarketEntity> search(SearchCondition... conditions) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<MarketEntity> searchWithJoin(List<SearchCondition> conditions, List<JoinCondition> joins,
                                             String orderBy, boolean ascending, Pager pager) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<MarketEntity> searchModelsByCategory(long categoryId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public void visitModelsByCategory(long categoryId, MarketEntityVisitor visitor) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<MarketEntity> searchVendorsByCategory(long categoryId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public SearchCondition getCategoryTreeCondition(long categoryId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public boolean isEntityExists(long entityId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public long getCategoryIdByModelId(long modelId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<Entity> getLinkedEntitiesUsingFrom(long fromId, long linkTypeId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<Entity> getLinkedEntitiesUsingTo(long toId, long linkTypeId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<Link> getLinksFrom(long fromId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<Link> getLinksTo(long toId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<Long> getLinksFrom(long fromId, long linkTypeId) {
        throw new NotImplementedException("method not implemented");
    }

    @Override
    public List<Long> getLinksTo(long toId, long linkTypeId) {
        throw new NotImplementedException("method not implemented");
    }
}
