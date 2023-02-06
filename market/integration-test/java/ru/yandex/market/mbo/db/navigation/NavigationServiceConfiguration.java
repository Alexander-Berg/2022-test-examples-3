package ru.yandex.market.mbo.db.navigation;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.db.IdGenerator;
import ru.yandex.market.mbo.configs.AutoUserConfig;
import ru.yandex.market.mbo.core.audit.AuditService;
import ru.yandex.market.mbo.db.CachedTreeService;
import ru.yandex.market.mbo.db.TovarTreeDao;
import ru.yandex.market.mbo.db.modelstorage.IndexedModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.recipes.RecipeService;
import ru.yandex.market.mbo.gwt.models.model_list.ModelListValidator;
import ru.yandex.market.mbo.gwt.models.navigation.FilterConfigValidator;
import ru.yandex.market.mbo.utils.db.TransactionChainCall;

/**
 * @author york
 * @since 13.08.2018
 */
@Configuration
@Import({
    AutoUserConfig.class,
    RecipesTestConfiguration.class
})
@SuppressWarnings("checkstyle:magicnumber")
public class NavigationServiceConfiguration {

    @Inject
    private RecipeService recipeService;

    @Inject
    private AuditService auditService;

    @Inject
    private TovarTreeDao tovarTreeDao;

    @Inject
    private CachedTreeService cachedTreeService;

    @Resource(name = "kdepotIdGenerator")
    private IdGenerator contentIdGenerator;

    @Bean(name = "navigationTreeServiceDraft")
    NavigationTreeService navigationTreeServiceDraft(
        @Qualifier("contentDraftPgNamedJdbcTemplate") NamedParameterJdbcTemplate namedContentJdbcTemplate,
        @Qualifier("contentDraftPgJdbcTemplate") JdbcTemplate contentJdbcTemplate,
        @Qualifier("contentDraftPgTransactionTemplate") TransactionTemplate navigationTransactionTemplate
    ) {
        NavigationTreeService navigationTreeService = new NavigationTreeService();
        navigationTreeService.setCopyService(navigationTreeCopyService());
        navigationTreeService.setRecipeService(recipeService);
        navigationTreeService.setCachedTreeService(cachedTreeService);
        navigationTreeService.setAuditService(auditService);
        injectServices(
            navigationTreeService, namedContentJdbcTemplate, contentJdbcTemplate, navigationTransactionTemplate
        );
        navigationTreeService.setIntegrityService(navigationTreeIntegrityService());
        navigationTreeService.setNavigationJdbcTemplate(contentJdbcTemplate);
        navigationTreeService.setNavigationNamedJdbcTemplate(namedContentJdbcTemplate);
        navigationTreeService.setNavigationTransactionTemplate(navigationTransactionTemplate);

        return navigationTreeService;
    }

    @Bean(name = "navigationTreeService")
    NavigationTreeService navigationTreeService(
        @Qualifier("contentPgNamedJdbcTemplate") NamedParameterJdbcTemplate namedContentJdbcTemplate,
        @Qualifier("contentPgJdbcTemplate") JdbcTemplate contentJdbcTemplate,
        @Qualifier("contentPgTransactionTemplate") TransactionTemplate navigationTransactionTemplate
    ) {
        NavigationTreeService navigationTreeService = new NavigationTreeService();
        navigationTreeService.setCopyService(navigationTreeCopyService());
        navigationTreeService.setRecipeService(recipeService);
        navigationTreeService.setCachedTreeService(cachedTreeService);
        navigationTreeService.setAuditService(auditService);
        injectServices(
            navigationTreeService, namedContentJdbcTemplate, contentJdbcTemplate, navigationTransactionTemplate
        );
        navigationTreeService.setIntegrityService(navigationTreeIntegrityService());
        navigationTreeService.setNavigationJdbcTemplate(contentJdbcTemplate);
        navigationTreeService.setNavigationNamedJdbcTemplate(namedContentJdbcTemplate);
        navigationTreeService.setNavigationTransactionTemplate(navigationTransactionTemplate);

        return navigationTreeService;
    }

    private void injectServices(
        NavigationTreeService navigationTreeService,
        NamedParameterJdbcTemplate namedContentJdbcTemplate,
        JdbcTemplate contentJdbcTemplate,
        TransactionTemplate navigationTransactionTemplate
    ) {
        NavigationMenuService navigationMenuService = new NavigationMenuService(namedContentJdbcTemplate,
                                                                                navigationTransactionTemplate,
                                                                                contentIdGenerator);
        navigationTreeService.setNavigationMenuService(
            navigationMenuService
        );
        NavigationTreeFilterService navigationTreeFilterService =
            new NavigationTreeFilterService(namedContentJdbcTemplate, contentJdbcTemplate, contentIdGenerator);
        navigationTreeService.setNavigationTreeFilterService(
            navigationTreeFilterService
        );
        ModelListService modelListService = new ModelListService(namedContentJdbcTemplate,
                                                                 navigationTransactionTemplate, contentIdGenerator);
        navigationTreeService.setModelListService(
            modelListService
        );
        FilterConfigService filterConfigService = new FilterConfigService(namedContentJdbcTemplate,
                                                                          navigationTransactionTemplate,
                                                                          contentIdGenerator);
        navigationTreeService.setFilterConfigService(
            filterConfigService
        );
        NavigationTreeTagService navigationTreeTagService = new NavigationTreeTagService(contentJdbcTemplate,
                                                                                         namedContentJdbcTemplate,
                                                                                         navigationTransactionTemplate);
        navigationTreeService.setNavigationTreeTagService(
            navigationTreeTagService
        );
        FastFilterService fastFilterService = new FastFilterService(namedContentJdbcTemplate,
                                                                    navigationTransactionTemplate);
        navigationTreeService.setFastFilterService(
            fastFilterService
        );
    }

    @Bean
    NavigationTreePublishService navigationTreePublishService(
        @Qualifier("siteCatalogPgNamedJdbcTemplate") NamedParameterJdbcTemplate namedScatJdbcTemplate,
        @Qualifier("siteCatalogPgTransactionTemplate") TransactionTemplate scatTransactionTemplate,
        @Qualifier("navigationTreeService") NavigationTreeService navigationTreeService,
        @Qualifier("navigationTreeServiceDraft") NavigationTreeService navigationTreeServiceDraft,
        NavigationTreeValidator navigationTreeValidator,
        NavigationTreeNodeRedirectService navigationTreeNodeRedirectService
    ) {
        NavigationTreePublishService navigationTreePublishService = new NavigationTreePublishService();
        navigationTreePublishService.setSiteCatalogNamedJdbcTemplate(namedScatJdbcTemplate);
        navigationTreePublishService.setSiteCatalogTransactionTemplate(scatTransactionTemplate);
        navigationTreePublishService.setAuditService(auditService);
        navigationTreePublishService.setNavigationTreeService(navigationTreeService);
        navigationTreePublishService.setNavigationTreeServiceDraft(navigationTreeServiceDraft);
        navigationTreePublishService.setNavigationTreeValidator(navigationTreeValidator);
        navigationTreePublishService.setNavigationNodeRedirectService(navigationTreeNodeRedirectService);
        return navigationTreePublishService;
    }

    @Bean
    NavigationTreeCopyService navigationTreeCopyService() {
        NavigationTreeCopyService navigationTreeCopyService = new NavigationTreeCopyService();
        navigationTreeCopyService.setTovarTreeDao(tovarTreeDao);
        return navigationTreeCopyService;
    }

    @Bean
    NavigationTreeIntegrityService navigationTreeIntegrityService() {
        return new NavigationTreeIntegrityService();
    }

    @Bean
    NavigationTreeValidator navigationTreeValidator() {
        return new NavigationTreeValidator();
    }

    @Bean
    public ModelListValidator modelListValidator() {
        return new ModelListValidator();
    }

    @Bean
    public FilterConfigValidator filterConfigValidator() {
        return new FilterConfigValidator();
    }

    @Bean
    public IndexedModelStorageService modelStorageService() {
        return new ModelStorageServiceStub();
    }

    @Bean
    NavigationTreeConverter navigationTreeConverter() {
        return new NavigationTreeConverter();
    }

    @Bean
    MainNidConstraintsValidator mainNidConstraintsValidator() {
        return new MainNidConstraintsValidator();
    }

    @Bean
    NavigationTreeNodeRedirectService navigationTreeNodeRedirectService() {
        return new NavigationTreeNodeRedirectService();
    }

    @Bean
    TransactionChainCall navigationTransactionChain() {
        return new TransactionChainCall();
    }

    @Bean
    public NavigationTreeFinder navigationTreeFinder(@Qualifier("contentPgNamedJdbcTemplate")
                                                             NamedParameterJdbcTemplate namedContentJdbcTemplate,
                                                     NavigationTreeService navigationTreeService) {
        return new NavigationTreeFinder(namedContentJdbcTemplate, navigationTreeService);
    }
}
