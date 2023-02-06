package ru.yandex.market.mbo.db.params;

import com.google.common.collect.ImmutableMap;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinitionBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.core.audit.AuditServiceMock;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction.ActionType;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction.BillingMode;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction.EntityType;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction.Source;
import ru.yandex.market.mbo.gwt.models.audit.AuditFilter;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueTestHelper;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * @author amaslak
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CategoryParameterValuesServiceTest {

    private TovarCategory category;

    private NamedParameterJdbcTemplate jdbcTemplate;

    private CategoryParameterValuesService service;

    private AuditServiceMock auditService;

    @Before
    public void setUp() {
        auditService = new AuditServiceMock();
        String dbName = getClass().getSimpleName() + UUID.randomUUID().toString();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(
            "jdbc:h2:mem:" + dbName +
                ";INIT=RUNSCRIPT FROM 'classpath:ru/yandex/market/mbo/db/params/category_parameter_values.sql'" +
                ";MODE=Oracle"
        );

        JdbcTemplate jdbcTemplateMock = Mockito.spy(new JdbcTemplate(dataSource));
        Mockito.doNothing().when(jdbcTemplateMock).execute(Mockito.startsWith("lock"));

        jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplateMock);

        TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        IParameterLoaderService parameterLoaderService = Mockito.mock(IParameterLoaderService.class);
        Mockito.when(parameterLoaderService.loadGlobalEntitiesWithoutValues()).thenReturn(new CategoryEntities());

        service = new CategoryParameterValuesService(new DefaultLobHandler(), transactionTemplate, jdbcTemplate,
            auditService);
        service.setParameterLoaderService(parameterLoaderService);

        TovarCategory globalCategory = new TovarCategory(-1);
        globalCategory.setHid(KnownIds.GLOBAL_CATEGORY_ID);
        addCategory(globalCategory);

        this.category = new TovarCategory(1);
        this.category.setHid(42);
        addCategory(this.category);
    }

    @Test
    public void testInitialValuesEmpty() {
        List<ParameterValues> initialValues = service.loadDefaultCategoryParameterValues();
        Assert.assertTrue(initialValues.isEmpty());

        List<ParameterValues> initialCategoryValues = service.loadCategoryParameterValues(42);
        Assert.assertTrue(initialCategoryValues.isEmpty());

        Map<Long, List<ParameterValues>> initialAllCategoryValues = service.loadAllCategoryParameterValues();
        Assert.assertEquals(1, initialAllCategoryValues.size());
        Assert.assertEquals(Collections.emptyList(), initialAllCategoryValues.get(KnownIds.GLOBAL_CATEGORY_ID));
        Assert.assertEquals(0, auditService.getAuditCount(new AuditFilter()));
    }

    @Test
    public void testNumericParam() {
        ParameterValue pv = ParameterValueTestHelper.numeric(2, "numeric_something", 542);
        addParameter(pv);

        List<ParameterValues> sourceParameterValues = Collections.singletonList(ParameterValues.of(pv));

        service.saveCategoryParameterValues(0L, sourceParameterValues, 0);
        service.saveCategoryParameterValues(category.getHid(), sourceParameterValues, 0);
        List<ParameterValues> parameterValues = service.loadCategoryParameterValues(category.getHid());

        Assert.assertEquals(sourceParameterValues, parameterValues);
        Assert.assertEquals(1, auditService.getAuditCount(new AuditFilter()));
        AuditAction auditAction = auditService.loadAudit(0, 1, new AuditFilter()).get(0);

        Assert.assertEquals(KnownIds.GLOBAL_CATEGORY_ID,    (long) auditAction.getCategoryId());
        Assert.assertEquals(2L,                             (long) auditAction.getParameterId());
        Assert.assertEquals(BillingMode.BILLING_MODE_NONE,  auditAction.getBillingMode());
        Assert.assertEquals(Source.MBO,                     auditAction.getSource());
        Assert.assertEquals(ActionType.CREATE,              auditAction.getActionType());
        Assert.assertEquals(EntityType.CATEGORY_PARAM,      auditAction.getEntityType());
        Assert.assertEquals("numeric_something",            auditAction.getPropertyName());
        Assert.assertEquals("",                           auditAction.getOldValue());
        Assert.assertEquals(String.valueOf(542),            auditAction.getNewValue());
    }

    @Test
    public void testBooleanParam() {
        ParameterValue pvTrue = ParameterValueTestHelper.bool(1, "is_something", true, 1);
        ParameterValue pvFalse = ParameterValueTestHelper.bool(pvTrue.getParamId(), pvTrue.getXslName(), false, 2);
        addParameter(pvTrue);
        addBooleanOption(pvTrue);
        addBooleanOption(pvFalse);

        List<ParameterValues> sourceParameterValues = Collections.singletonList(ParameterValues.of(pvTrue));

        service.saveCategoryParameterValues(0L, sourceParameterValues, 0);
        service.saveCategoryParameterValues(category.getHid(), sourceParameterValues, 0);
        List<ParameterValues> parameterValues = service.loadCategoryParameterValues(category.getHid());

        Assert.assertEquals(sourceParameterValues, parameterValues);
        Assert.assertEquals(1, auditService.getAuditCount(new AuditFilter()));
        AuditAction auditAction = auditService.loadAudit(0, 1, new AuditFilter()).get(0);

        Assert.assertEquals(KnownIds.GLOBAL_CATEGORY_ID,    (long) auditAction.getCategoryId());
        Assert.assertEquals(1L,                             (long) auditAction.getParameterId());
        Assert.assertEquals(BillingMode.BILLING_MODE_NONE,  auditAction.getBillingMode());
        Assert.assertEquals(Source.MBO,                     auditAction.getSource());
        Assert.assertEquals(ActionType.CREATE,              auditAction.getActionType());
        Assert.assertEquals(EntityType.CATEGORY_PARAM,      auditAction.getEntityType());
        Assert.assertEquals("is_something",                 auditAction.getPropertyName());
        Assert.assertEquals("",                           auditAction.getOldValue());
        Assert.assertEquals(String.valueOf(true),           auditAction.getNewValue());
    }

    @Test
    public void testEnumParam() {
        ParameterValue pv1 = ParameterValueTestHelper
            .enumValue(3, "enum_something", 1, WordUtil.defaultWords("value_1"));
        addParameter(pv1);
        addOption(pv1);

        ParameterValue pv2 = ParameterValueTestHelper
            .enumValue(pv1.getParamId(), "enum_something", 2, WordUtil.defaultWords("value_2"));
        addOption(pv2);

        List<ParameterValues> sourceParameterValues = Collections.singletonList(
            ParameterValues.of(Arrays.asList(pv1, pv2))
        );

        service.saveCategoryParameterValues(0L, sourceParameterValues, 0);
        service.saveCategoryParameterValues(category.getHid(), sourceParameterValues, 0);
        List<ParameterValues> parameterValues = service.loadCategoryParameterValues(category.getHid());

        Assert.assertEquals(sourceParameterValues, parameterValues);
        Assert.assertEquals(1, auditService.getAuditCount(new AuditFilter()));
        AuditAction auditAction = auditService.loadAudit(0, 1, new AuditFilter()).get(0);

        Assert.assertEquals(KnownIds.GLOBAL_CATEGORY_ID,    (long) auditAction.getCategoryId());
        Assert.assertEquals(3L,                             (long) auditAction.getParameterId());
        Assert.assertEquals(BillingMode.BILLING_MODE_NONE,  auditAction.getBillingMode());
        Assert.assertEquals(Source.MBO,                     auditAction.getSource());
        Assert.assertEquals(ActionType.CREATE,              auditAction.getActionType());
        Assert.assertEquals(EntityType.CATEGORY_PARAM,      auditAction.getEntityType());
        Assert.assertEquals("enum_something",               auditAction.getPropertyName());
        Assert.assertEquals("",                           auditAction.getOldValue());
        Assert.assertEquals("1, 2",                         auditAction.getNewValue());
    }

    @Test
    public void testDefaultParam() {
        ParameterValue pv = ParameterValueTestHelper.numeric(1, "xsl", 1);
        addParameter(pv);

        List<ParameterValues> sourceParameterValues = Collections.singletonList(ParameterValues.of(pv));

        service.saveCategoryParameterValues(category.getHid(), sourceParameterValues, 0);
        Assert.assertTrue(
            "Parameters without default value must be ignored",
            service.loadCategoryParameterValues(category.getHid()).isEmpty()
        );

        service.saveCategoryParameterValues(0L, sourceParameterValues, 0);
        List<ParameterValues> parameterValues = service.loadCategoryParameterValues(category.getHid());

        Assert.assertEquals(sourceParameterValues, parameterValues);
    }

    @Test
    public void testUpdatedParam() {
        ParameterValue pv = ParameterValueTestHelper.numeric(1, "xsl", 1);
        addParameter(pv);

        List<ParameterValues> sourceParameterValues = Collections.singletonList(ParameterValues.of(pv));

        service.saveCategoryParameterValues(0L, sourceParameterValues, 0);
        List<ParameterValues> parameterValues = service.loadCategoryParameterValues(category.getHid());

        Assert.assertEquals(sourceParameterValues, parameterValues);

        ParameterValue updatedPv = ParameterValueTestHelper.numeric(
            pv.getParamId(), pv.getXslName(), pv.getNumericValue().intValue() + 100
        );
        List<ParameterValues> updatedParameterValues = Collections.singletonList(ParameterValues.of(updatedPv));

        auditService.clearActions();
        service.saveCategoryParameterValues(category.getHid(), updatedParameterValues, 0);
        parameterValues = service.loadCategoryParameterValues(category.getHid());

        Assert.assertEquals(updatedParameterValues, parameterValues);
        Assert.assertEquals(1, auditService.getAuditCount(new AuditFilter()));
        AuditAction auditAction = auditService.loadAudit(0, 1, new AuditFilter()).get(0);

        Assert.assertEquals(category.getHid(),              (long) auditAction.getCategoryId());
        Assert.assertEquals(1L,                             (long) auditAction.getParameterId());
        Assert.assertEquals(BillingMode.BILLING_MODE_NONE,  auditAction.getBillingMode());
        Assert.assertEquals(Source.MBO,                     auditAction.getSource());
        Assert.assertEquals(ActionType.UPDATE,              auditAction.getActionType());
        Assert.assertEquals(EntityType.CATEGORY_PARAM,      auditAction.getEntityType());
        Assert.assertEquals("xsl",                          auditAction.getPropertyName());
        Assert.assertEquals("1",                            auditAction.getOldValue());
        Assert.assertEquals("101",                          auditAction.getNewValue());
    }

    @Test
    public void testBatchLoading() {
        ParameterValue pv1 = ParameterValueTestHelper
            .enumValue(3, "enum_something", 1, WordUtil.defaultWords("value_1"));
        addParameter(pv1);
        addOption(pv1);

        List<ParameterValues> sourceParameterValues = Collections.singletonList(
            ParameterValues.of(Arrays.asList(pv1))
        );

        service.saveCategoryParameterValues(0L, sourceParameterValues, 0);
        Map<Long, List<ParameterValues>> allCategoryParameterValues = service.loadAllCategoryParameterValues();
        Assert.assertEquals(1, allCategoryParameterValues.size());
        Assert.assertEquals(sourceParameterValues, allCategoryParameterValues.get(KnownIds.GLOBAL_CATEGORY_ID));

        ParameterValue pv2 = ParameterValueTestHelper
            .enumValue(pv1.getParamId(), "enum_something", 2, WordUtil.defaultWords("value_2"));
        addOption(pv2);

        List<ParameterValues> categoryParameterValues = Collections.singletonList(
            ParameterValues.of(Arrays.asList(pv1, pv2))
        );

        service.saveCategoryParameterValues(category.getHid(), categoryParameterValues, 0);
        allCategoryParameterValues = service.loadAllCategoryParameterValues();
        Assert.assertEquals(2, allCategoryParameterValues.size());
        Assert.assertEquals(categoryParameterValues, allCategoryParameterValues.get(category.getHid()));
    }

    @Test
    public void testByteConverter() {
        EnhancedRandom enhancedRandom = new EnhancedRandomBuilder()
            .seed(0)
            .collectionSizeRange(0, 10)
            .stringLengthRange(0, 100)
            .exclude(FieldDefinitionBuilder.field().inClass(Word.class).named("id").get())
            .randomize(Long.class, (Supplier<Long>) () -> (long) ThreadLocalRandom.current().nextInt())
            .randomize(BigDecimal.class, (Supplier<BigDecimal>) () ->
                BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble())
                    .setScale(16, BigDecimal.ROUND_HALF_UP)
            )
            .build();

        for (int i = 0; i < 1_000; i++) {
            ParameterValue pv = enhancedRandom.nextObject(ParameterValue.class);
            byte[] bytes = CategoryParameterValuesService.packParameterValue(pv);
            ParameterValue unpacked = CategoryParameterValuesService.unpackParameterValue(bytes);
            Assert.assertEquals(pv, unpacked);
        }
    }

    private void addCategory(TovarCategory category) {
        jdbcTemplate.update("insert into category(hyper_id) values(:hid)", ImmutableMap.of(
            "hid", category.getHid())
        );
    }

    private void addParameter(ParameterValue pv) {
        jdbcTemplate.update("insert into parameter(id, type) values(:param_id, :type)", ImmutableMap.of(
            "param_id", pv.getParamId(),
            "type", pv.getType().name()
        ));
    }

    private void addOption(ParameterValue pv) {
        String stringValue = Objects.requireNonNull(WordUtil.getDefaultWord(pv.getStringValue()));
        jdbcTemplate.update("insert into enum_option(id, param_id, name) values(:id, :param_id, :name)",
            ImmutableMap.of(
                "id", pv.getOptionId(),
                "param_id", pv.getParamId(),
                "name", stringValue
            )
        );
    }

    private void addBooleanOption(ParameterValue pv) {
        jdbcTemplate.update("insert into boolean_value(id, param_id, value) values(:id, :param_id, :value)",
            ImmutableMap.of(
                "id", pv.getOptionId(),
                "param_id", pv.getParamId(),
                "value", pv.getBooleanValue()
            )
        );
    }
}
