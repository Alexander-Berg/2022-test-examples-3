package ru.yandex.market.mbo.db.navigation.stubs;

import ru.yandex.market.mbo.db.navigation.CopySubTreeNodesData;
import ru.yandex.market.mbo.db.navigation.NavigationTreePublishService;
import ru.yandex.market.mbo.db.navigation.NavigationTreeService;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author york
 * @since 17.08.2018
 */
public class NavigationTreePublishServiceStub extends NavigationTreePublishService {

    private Map<String, Set<Long>> inheritedEmpty = new HashMap<>();
    private Map<String, Set<Long>> inheritedAsNormal = new HashMap<>();
    private Map<String, Set<Long>> copied = new HashMap<>();
    private Map<String, Set<Long>> removed = new HashMap<>();
    private Map<String, Set<Long>> removedOutgoingRedirects = new HashMap<>();

    private Map<String, NavigationTreeServiceStub> navigationTreePublishServices = new HashMap<>();

    void reset() {
        inheritedEmpty.clear();
        inheritedAsNormal.clear();
        copied.clear();
        removed.clear();
    }

    @Override
    protected void doCopyInheritedEmpty(String fromSchemaName, String toSchemaName, Set<Long> addInheritedEmpty) {
        inheritedEmpty.put(toSchemaName, addInheritedEmpty);
    }

    @Override
    protected void doCopyInheritedAsNormal(String fromSchemaName, String toSchemaName, Set<Long> addInheritedAsNormal) {
        inheritedAsNormal.put(toSchemaName, addInheritedAsNormal);
    }

    @Override
    protected void doCopyNavigationNodes(String fromSchemaName, String toSchemaName, Set<Long> fromIds) {
        NavigationTreeServiceStub fromService = navigationTreePublishServices.get(fromSchemaName);
        NavigationTreeServiceStub toService = navigationTreePublishServices.get(toSchemaName);

        fromService.getNodeMap().entrySet().stream()
            .filter(e -> fromIds.contains(e.getKey()))
            .forEach(e -> toService.getNodeMap().put(e.getKey(), e.getValue()));

        copied.put(toSchemaName, fromIds);
    }

    @Override
    protected Set<Long> copyTree(CopySubTreeNodesData data) {
        doCopyTreeStub(data);
        return data.getAffectedTreeIds();
    }

     private void doCopyTreeStub(CopySubTreeNodesData data) {
        NavigationTreeServiceStub fromService = navigationTreePublishServices.get(
            data.getFromSchemaName()
        );
        NavigationTreeServiceStub toService = navigationTreePublishServices.get(
            data.getToSchemaName()
        );
        toService.getTreeMap().put(
            data.getTreeId(),
            fromService.getTreeMap().get(data.getTreeId())
        );
    }

    @Override
    protected void removeOutgoingRedirects(NavigationTree tree,
                                           Set<Long> treeNodesToRemoveOutgoingRedirects) {
        removedOutgoingRedirects.put(
            tree.getCode(), treeNodesToRemoveOutgoingRedirects);
    }

    @Override
    protected void doRemoveNavigationNodes(String toSchemaName, Set<Long> toIds) {
        removed.put(toSchemaName, toIds);
    }

    @Override
    protected NavigationTreeService getServiceForScheme(String fromSchemaName) {
        return navigationTreePublishServices.get(fromSchemaName);
    }

    @Override
    public void setNavigationTreeService(NavigationTreeService navigationTreeService) {
        super.setNavigationTreeService(navigationTreeService);
        navigationTreePublishServices.put(PUBLISHED_SCHEME_NAME, (NavigationTreeServiceStub) navigationTreeService);
    }

    @Override
    public void setNavigationTreeServiceDraft(NavigationTreeService navigationTreeServiceDraft) {
        super.setNavigationTreeServiceDraft(navigationTreeServiceDraft);
        navigationTreePublishServices.put(DRAFT_SCHEME_NAME, (NavigationTreeServiceStub) navigationTreeServiceDraft);
    }

    public Set<Long> getRemovedOutgoingRedirects(String code) {
        return removedOutgoingRedirects.get(code);
    }

    public Set<Long> getInheritedEmpty(String schema) {
        return inheritedEmpty.get(schema);
    }

    public Set<Long> getInheritedAsNormal(String schema) {
        return inheritedAsNormal.get(schema);
    }

    public Set<Long> getCopied(String schema) {
        return copied.get(schema);
    }

    public Set<Long> getRemoved(String schema) {
        return removed.get(schema);
    }
}
