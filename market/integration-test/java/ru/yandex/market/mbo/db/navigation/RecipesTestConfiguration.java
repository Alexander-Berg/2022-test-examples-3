package ru.yandex.market.mbo.db.navigation;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.configs.TestConfiguration;
import ru.yandex.market.mbo.configs.audit.AuditProdConfig;
import ru.yandex.market.mbo.core.audit.AuditService;
import ru.yandex.market.mbo.core.conf.databases.NamedHikariDataSource;
import ru.yandex.market.mbo.db.TovarTreeDao;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.recipes.RecipeService;
import ru.yandex.market.mbo.db.recipes.RecipeServiceDao;
import ru.yandex.market.mbo.db.recipes.RecipeValidator;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;

import static org.mockito.ArgumentMatchers.anyLong;

/**
 * @author york
 * @since 13.08.2018
 */
@Configuration
@Import({
    TestConfiguration.class, AuditProdConfig.class, TovarTreeStubConfiguration.class
})
public class RecipesTestConfiguration {

    @Inject
    TovarTreeDao tovarTreeDao;

    @Resource(name = "auditService")
    private AuditService auditService;
    @Resource(name = "contentPgDataSource")
    private NamedHikariDataSource contentDataSource;
    @Resource(name = "contentPgNamedJdbcTemplate")
    private NamedParameterJdbcTemplate namedContentJdbcTemplate;
    @Resource(name = "contentPgTransactionTemplate")
    private TransactionTemplate contentTransactionTemplate;

    public static IParameterLoaderService parameterLoaderServiceStub() {
        IParameterLoaderService parameterLoaderService = Mockito.mock(IParameterLoaderService.class);
        Mockito.when(parameterLoaderService.loadCategoryEntitiesByHid(anyLong())).thenReturn(new CategoryEntities());
        return parameterLoaderService;
    }

    @Bean
    RecipeValidator recipeValidator() {
        return new RecipeValidator(
            parameterLoaderServiceStub(),
            tovarTreeDao,
            null);
    }

    @Bean
    RecipeService recipeService() {
       return new RecipeService(
           recipeValidator(),
           recipeServiceDao());
    }

    @Bean
    RecipeServiceDao recipeServiceDao() {
        return new RecipeServiceDao(
            namedContentJdbcTemplate,
            contentTransactionTemplate,
            auditService);
    }

}
