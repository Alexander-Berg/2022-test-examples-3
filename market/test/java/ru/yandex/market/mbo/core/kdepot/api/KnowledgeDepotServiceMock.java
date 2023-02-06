package ru.yandex.market.mbo.core.kdepot.api;

import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import ru.yandex.common.framework.pager.Pager;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.core.kdepot.impl.EntityInternalIterator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * @author s-ermakov
 */
public class KnowledgeDepotServiceMock implements KnowledgeDepotService {

    private Map<Long, EntityStub> entitiesMap = new HashMap<>();

    @Override
    public List<String> getAttribute(long entityId, String name) {
        return Optional.ofNullable(entitiesMap.get(entityId))
            .map(e -> e.getAttribute(name))
            .orElse(Collections.emptyList());
    }

    @Override
    public void addAttribute(long entityId, String name, String value) {

    }

    @Override
    public void addAttribute(long entityId, String name, String... value) {

    }

    @Override
    public void addAttribute(long entityId, String name, List<String> value) {

    }

    @Override
    public void setAttribute(long entityId, String name, String... value) {
        setAttribute(entityId, name, Arrays.asList(value));
    }

    @Override
    public void setAttribute(long entityId, String name, List<String> value) {
        EntityStub entity = entitiesMap.computeIfAbsent(entityId, k -> new EntityStub());
        entity.setAttributes(name, value);
    }

    @Override
    public void removeAttribute(long entityId, String name) {

    }

    @Override
    public void removeAttribute(long entityId, String... name) {

    }

    @Override
    public void removeAttribute(long entityId, Collection<String> name) {

    }

    @Override
    public void setModifiedTs(long entityId, String ts) {

    }

    @Override
    public String getModifiedTs(long entityId) {
        return null;
    }

    @Override
    public long createEntity(long entityType, Map<String, List<String>> content) {
        return 0;
    }

    @Override
    public void createEntityType(long entityTypeId, String name) {

    }

    @Override
    public long createEntityWithKnownId(long entityType, long id, Map<String, List<String>> content) {
        return 0;
    }

    @Override
    public long createEntity(long entityType) {
        return 0;
    }

    @Override
    public void removeEntity(long entityId) {

    }

    @Override
    public void removeEntities(List<Long> entityIds) {

    }

    @Override
    public void removeEntityWithReplace(long removeId, long replaceId) {

    }

    @Override
    public boolean hasAttribute(long entityId, String name) {
        return false;
    }

    @Override
    public Entity getEntity(long entityId) throws NoSuchElementException {
        return null;
    }

    @Override
    public long getRealEntityId(long entityId) {
        return 0;
    }

    @Override
    public boolean exists(long entityId) {
        return false;
    }

    @Override
    public Link createLink(long linkTypeId, long fromId, long toId) {
        return null;
    }

    @Override
    public void createLinkType(long linkTypeId, String code, String name, String description,
                               Collection<Pair<Long, Long>> allowableLinkType) {

    }

    @Override
    public List<Link> getLinksFrom(long fromId) {
        return null;
    }

    @Override
    public List<Link> getLinksTo(long toId) {
        return null;
    }

    @Override
    public List<Entity> getLinkedEntitiesUsingFrom(long fromId, long linkTypeId) {
        return null;
    }

    @Override
    public List<Entity> getLinkedEntitiesUsingTo(long toId, long linkTypeId) {
        return null;
    }

    @Override
    public Map<Long, Collection<Entity>> getLinkedEntitiesUsingFrom(long fromId) {
        return null;
    }

    @Override
    public Map<Long, Collection<Entity>> getLinkedEntitiesUsingTo(long toId) {
        return null;
    }

    @Override
    public void removeLink(Long linkType, Long from, Long to) {

    }

    @Override
    public List<EntityType> getEntityTypes() {
        return null;
    }

    @Override
    public Map<Long, EntityType> getEntityTypesMap() {
        return null;
    }

    @Override
    public List<LinkType> getLinkTypes() {
        return null;
    }

    @Override
    public Map<Long, LinkType> getLinkTypesMap() {
        return null;
    }

    @Override
    public List<LinkTypeAllowable> getLinkAllowableTypes(LinkType link) {
        return null;
    }

    @Override
    public List<LinkTypeAllowable> getLinkAllowableTypes(Long linkTypeId) {
        return null;
    }

    @Override
    public List<LinkType> getAllowableLinkTypesUsingFrom(long fromEntityTypeId) {
        return null;
    }

    @Override
    public List<LinkType> getAllowableLinkTypesUsingTo(long toEntityTypeId) {
        return null;
    }

    @Override
    public Entity getRootEntity() {
        return null;
    }

    @Override
    public List<Long> getRootEntities() {
        return null;
    }

    @Override
    public List<Long> getLinksFrom(long fromId, long linkTypeId) {
        return null;
    }

    @Override
    public List<Long> getLinksTo(long toId, long linkTypeId) {
        return null;
    }

    @Override
    public Long getEntityTypeIdByEntityId(long id) {
        return null;
    }

    @Override
    public EntityType getEntityTypeByEntityId(long id) {
        return null;
    }

    @Override
    public boolean linkExists(long linkTypeId, long fromId, long toId) {
        return false;
    }

    @Override
    public boolean isLinkAllowed(long linkTypeId, Long fromType, Long toType) {
        return false;
    }

    @Override
    public BulkService getBulkService() {
        return null;
    }

    @Override
    public List<Entity> searchEntities(Map<String, String> params, Long entityTypeId) {
        return null;
    }

    @Override
    public List<Entity> searchEntities(List<SearchCondition> conditions) {
        return null;
    }

    @Override
    public void visitEntities(List<SearchCondition> conditions, EntityVisitor visitor) {

    }

    @Override
    public List<Entity> searchEntities(List<SearchCondition> conditions, List<JoinCondition> joins) {
        return null;
    }

    @Override
    public List<Entity> searchEntities(SearchCondition... conditions) {
        return null;
    }

    @Override
    public List<Entity> searchOrdered(String orderBy, boolean ascending, List<SearchCondition> conditions) {
        return null;
    }

    @Override
    public List<Entity> searchOrderedPage(String orderBy, boolean ascending, Pager pager,
                                          List<SearchCondition> conditions, List<JoinCondition> joins) {
        return null;
    }

    @Override
    public void visitEntityAndItsChildren(long parentId, int linkType, EntityVisitor visitor) {

    }

    @Override
    public Object doInKdepotTransaction(TransactionCallback transactionCallback) {
        return transactionCallback.doInTransaction(new SimpleTransactionStatus());
    }

    @Override
    public EntityInternalIterator getEntityInternalIterator() {
        return null;
    }

    @Override
    public void removeLinksUsingTo(long linkTypeId, long toId) {

    }

    @Override
    public void removeLinksUsingFrom(long linkTypeId, long fromId) {

    }

    @Override
    public void removeAllLinks(long entityId) {

    }
}
