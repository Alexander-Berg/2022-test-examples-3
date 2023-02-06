package ru.yandex.market.mbo.db.navigation;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import ru.yandex.market.mbo.db.CachedTreeService;
import ru.yandex.market.mbo.db.CategoryTaskGenInfoService;
import ru.yandex.market.mbo.db.LogTaskRuleService;
import ru.yandex.market.mbo.db.ReturnPoliciesService;
import ru.yandex.market.mbo.db.TovarTreeDao;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.category_wiki.CategoryWikiService;
import ru.yandex.market.mbo.db.params.CategoryParameterValuesService;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.guru.GuruService;
import ru.yandex.market.mbo.db.recommendations.RecommendationValidationService;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyLong;

/**
 * @author york
 * @since 13.08.2018
 */
@Profile("navigation-test")
@Configuration
@SuppressWarnings("checkstyle:magicnumber")
public class TovarTreeStubConfiguration {

    @Bean
    @Primary
    TovarTreeDao tovarTreeDao() {
        TovarTree tovarTree = createCutTovarTree();
        TovarTreeDao tovarTreeDao = Mockito.mock(TovarTreeDao.class);
        Mockito.when(tovarTreeDao.loadTovarTree()).thenReturn(tovarTree);
        Mockito.when(tovarTreeDao.loadTreeScheme()).thenReturn(tovarTree);
        Mockito.when(tovarTreeDao.loadCategoryByHid(anyLong())).then(invocationOnMock -> {
            long hid = invocationOnMock.getArgument(0);
            return tovarTree.getRoot().findAll(tc -> tc.getHid() == hid).get(0).getData();
        });
        return tovarTreeDao;
    }

    @Bean
    TovarTreeService tovarTreeService() {
        return new TovarTreeServiceMock(tovarTreeDao());
    }

    @Bean
    CachedTreeService cachedTreeService() {
        return new CachedTreeService(tovarTreeDao(), 5);
    }

    @Bean(name = "tovarTreeNameListeners")
    List<TovarTreeService.NameListener> nameListeners() {
        return null;
    }
    @Bean(name = "categoryTaskGenInfoService")
    CategoryTaskGenInfoService taskGenInfoService() {
        return null;
    }

    @Bean(name = "logTaskRuleService")
    LogTaskRuleService logTaskRuleService() {
        return null;
    }

    @Bean
    CategoryParameterValuesService categoryParameterValuesService() {
        return null;
    }

    @Bean
    CategoryWikiService categoryWikiService() {
        return null;
    }

    @Bean
    RecommendationValidationService recommendationValidationService() {
        return null;
    }

    @Bean(name = "parameterLoaderService")
    IParameterLoaderService parameterLoaderService() {
        return null;
    }

    @Bean
    GuruService guruService() {
        return null;
    }

    @Bean
    ReturnPoliciesService returnPoliciesService() {
        return null;
    }

    private TovarTree createCutTovarTree() {
        Map<Long, TovarCategoryNode> nodeMap = asMap(
            new TovarCategory("Все товары", 90401L, 0),
            new TovarCategory("Авто", 90402L, 90401L),
            new TovarCategory("Бытовая техника", 198118L, 90401L),
            new TovarCategory("Техника для красоты", 922553L, 198118L),
            new TovarCategory("Запчасти", 90435L, 90402L),
            new TovarCategory("Двигатель", 90437L, 90435L),
            new TovarCategory("Привод", 90445L, 90435L)
        );
        for (TovarCategoryNode node : nodeMap.values()) {
            if (nodeMap.containsKey(node.getParentHid())) {
                TovarCategoryNode parentNode = nodeMap.get(node.getParentHid());
                parentNode.addChild(node);
            }
        }
        TovarTree tree = new TovarTree(nodeMap.get(90401L));
        tree.getHidMap().values().forEach(tcn -> {
            tcn.getData().setPublished(true);
            //making leaf gurulight
            if (tcn.getChildren().isEmpty()) {
                tcn.getData().setVisual(true);
                tcn.getData().setShowModelTypes(Collections.singletonList(CommonModel.Source.CLUSTER));
            }
        });
        tree.levelTree();
        return tree;
    }

    private Map<Long, TovarCategoryNode> asMap(TovarCategory... tovarCategories) {
        return Arrays.stream(tovarCategories)
            .map(TovarCategoryNode::new)
            .collect(Collectors.toMap(TovarCategoryNode::getHid, Function.identity()));
    }
}
