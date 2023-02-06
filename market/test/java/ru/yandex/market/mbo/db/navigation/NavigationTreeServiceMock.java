package ru.yandex.market.mbo.db.navigation;

import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.core.audit.AuditService;
import ru.yandex.market.mbo.db.CachedTreeService;
import ru.yandex.market.mbo.db.IdGeneratorStub;
import ru.yandex.market.mbo.db.TovarTreeDao;
import ru.yandex.market.mbo.db.TovarTreeDaoMock;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.tovartree.OutputType;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NavigationTreeServiceMock extends NavigationTreeService {

    private static final TovarTreeDao TOVAR_TREE_DAO = createTovarTreeDao();

    private static final int TREE_ID = 1;
    private static final int ROOT_NODE_ID = 2;

    private List<NavigationTree> navigationTrees;

    public NavigationTreeServiceMock() {

        JdbcTemplate jdbcTemplateMock = Mockito.mock(JdbcTemplate.class);

        Mockito.doAnswer(invocation -> 0L)
            .when(jdbcTemplateMock)
            .queryForObject(Mockito.anyString(), Mockito.same(Long.class));

        Mockito.doAnswer(invocation -> 0)
            .when(jdbcTemplateMock)
            .queryForObject(Mockito.anyString(), Mockito.same(Integer.class));

        IdGeneratorStub contentIdGenerator = new IdGeneratorStub();
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplateMock);
        TransactionTemplate transactionTemplate = Mockito.mock(TransactionTemplate.class);
        setContentIdGenerator(contentIdGenerator);
        setNavigationNamedJdbcTemplate(namedJdbcTemplate);
        setAuditService(Mockito.mock(AuditService.class));
        setNavigationJdbcTemplate(jdbcTemplateMock);
        setCachedTreeService(Mockito.mock(CachedTreeService.class));
        Mockito.doAnswer(invocation -> {
            TransactionCallback action = invocation.getArgument(0);
            action.doInTransaction(null);
            return null;
        }).when(transactionTemplate).execute(Mockito.any());
        setNavigationTransactionTemplate(transactionTemplate);
        setNavigationTreeValidator(new NavigationTreeValidator());
        setNavigationTreeFilterService(Mockito.mock(NavigationTreeFilterService.class));
        setModelListService(new ModelListService(namedJdbcTemplate, transactionTemplate, contentIdGenerator));
        setFilterConfigService(new FilterConfigService(namedJdbcTemplate, transactionTemplate, contentIdGenerator));
        setNavigationTreeTagService(new NavigationTreeTagService(jdbcTemplateMock, namedJdbcTemplate,
                transactionTemplate));
        initNavigationTrees();

    }

    @Override
    public long getTreeIdByNodeId(long nodeId) {
        return TREE_ID;
    }

    /**
     * Создает сервис товарного дерева следующей структуры:
     * Корень дерева категория с ID == 1.
     * Далее идут ее дети:
     * 1) GURU, hid = 2
     * 2) GURULIGHT, hid = 3
     * 3) MIXED, hid = 4
     * 4) SIMPLE, hid = 5
     * 5) UNDEFINED, hid = 6
     * 6) VISUAL, hid = 7
     */
    private static TovarTreeDao createTovarTreeDao() {
        int parentHid = 2;
        TovarCategoryBuilder tovarCategoryBuilder = TovarCategoryBuilder.newBuilder();
        List<OutputType> types = Stream.of(OutputType.values())
            .sorted(Comparator.comparing(Enum::name))
            .collect(Collectors.toList());

        List<TovarCategory> categories = new ArrayList<>();

        // add root
        categories.add(tovarCategoryBuilder.setHid(parentHid).setName("root category").setTovarId(-1).create());

        for (int i = 0, id = parentHid + 1; i < types.size(); i++, id++) {
            int index = i;
            TovarCategory tovarCategory = new TovarCategory() {
                @Override
                public OutputType getOutputType() {
                    return types.get(index);
                }
            };
            tovarCategory.setHid(id);
            tovarCategory.setParentHid(parentHid);
            tovarCategory.setTovarId(-1);
            tovarCategory.setName(types.get(i) + " category");

            categories.add(tovarCategory);
        }

        return new TovarTreeDaoMock(categories);
    }

    @Override
    public NavigationNode getNavigationNode(Long id) {
        return navigationTrees.stream()
            .flatMap(nt -> nt.getRoot().findAll(n -> true).stream())
            .filter(tnd -> tnd.getData().getId() == id)
            .map(TreeNode::getData)
            .findFirst()
            .orElse(null);
    }

    @Override
    public void moveNode(Long uid, NavigationNode node, long oldParentId, long newParentId) {
    }

    @Override
    public TovarTree getCachedTovarTree() {
        return TOVAR_TREE_DAO.loadTovarTree();
    }

    public List<NavigationTree> getNavigationTrees() {
        return navigationTrees;
    }

    @Override
    public void addNavigationTreeNode(Long uid, long parentId, TreeNode<NavigationNode> newNode) {
        super.addNavigationTreeNode(uid, parentId, newNode);
        navigationTrees.get(0).getRoot().addChild(newNode);
    }

    private void initNavigationTrees() {
        NavigationTree tree = new NavigationTree();
        tree.setId(TREE_ID);
        tree.setName("Tree");
        tree.setTovarTreeSyncStatus(true);

        TreeNode<NavigationNode> rootNode = new TreeNode<>(new SimpleNavigationNode());
        rootNode.getData().setId(ROOT_NODE_ID);
        rootNode.getData().setHid(new Long(ROOT_NODE_ID));
        rootNode.getData().setType(NavigationNode.Type.CATEGORY);
        rootNode.getData().setName("Root Category");
        rootNode.getData().setIsPrimary(true);
        tree.setRoot(rootNode);

        for (int i = 0, id = ROOT_NODE_ID + 1; i < OutputType.values().length; i++, id++) {
            NavigationNode node = new SimpleNavigationNode();
            node.setId(id);
            node.setParentId(ROOT_NODE_ID);
            node.setHid(new Long(id));
            node.setType(NavigationNode.Type.CATEGORY);
            node.setName("Category" + id);
            node.setIsPrimary(true);
            rootNode.addChild(node);
        }

        navigationTrees = Collections.singletonList(tree);
    }
}
