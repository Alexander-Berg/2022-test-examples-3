package ru.yandex.market.mbo.billing.counter.model;

import org.junit.Before;
import org.mockito.Mockito;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.PaidEntryDaoMock;
import ru.yandex.market.mbo.billing.counter.base.PaidEntryQueryParams;
import ru.yandex.market.mbo.billing.tarif.TarifMultiplicatorService;
import ru.yandex.market.mbo.category.mappings.CategoryMappingService;
import ru.yandex.market.mbo.core.guru.GuruCategoryService;
import ru.yandex.market.mbo.core.kdepot.api.EntityStub;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.billing.dao.PaidEntry;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:magicnumber")
public abstract class ModelParametersBillingLoaderTestBase extends BillingLoaderTestBase {
    protected static final BigDecimal SEARCH_DIFFICULTY = new BigDecimal(4);
    protected static final BigDecimal TARIF_PRICE = new BigDecimal(2);
    protected static final BigDecimal PRICE = TARIF_PRICE.multiply(SEARCH_DIFFICULTY);

    protected AbstractModelParametersBillingLoader loader;

    protected GuruCategoryService guruCategoryService;
    protected PaidEntryDaoMock paidEntryDaoMock;
    protected EntityStub guruCategory;
    protected CategoryMappingService categoryMappingService;
    protected ParameterLoaderServiceStub parameterLoaderService;

    @Before
    public void setUp() {
        super.setUp();

        categoryMappingService = Mockito.mock(CategoryMappingService.class);
        parameterLoaderService = new ParameterLoaderServiceStub();
        parameterLoaderService.addCategoryEntities(new CategoryEntities(1L, Collections.emptyList()));

        guruCategoryService = Mockito.spy(new GuruCategoryService(null, categoryMappingService));

        guruCategory = new EntityStub();
        guruCategory.setAttribute("search_info_difficulty", SEARCH_DIFFICULTY);
        Mockito.doReturn(guruCategory).when(guruCategoryService).getGuruCategoryEntityById(Mockito.anyLong());

        TarifMultiplicatorService tarifMultiplicatorService =
            new TarifMultiplicatorService(guruCategoryService, parameterLoaderService);
        loader = new ModelParametersBillingLoader(tarifMultiplicatorService);
        loader.setAuditService(auditService);
        loader.setBillingStartDateStr("09-03-2017");

        paidEntryDaoMock = new PaidEntryDaoMock();
        loader.setPaidEntryDao(paidEntryDaoMock);
    }

    protected void insertAuditActions(List<AuditAction> auditActions) {
        auditService.writeActions(auditActions);

        auditActions.stream().map(ModelParametersBillingLoaderTestBase::fromAuditAction).forEach(params -> {
            paidEntryDaoMock.addPaidEntry(params,
                params.getPaidAction() == PaidAction.FILL_MODEL_PARAMETER
                    ? new PaidEntry(PRICE.doubleValue(), 1L)
                    : new PaidEntry(0.5 * PRICE.doubleValue(), 1L));
        });
    }

    private static PaidEntryQueryParams fromAuditAction(AuditAction auditAction) {
        return new PaidEntryQueryParams(
            auditAction.getEntityId(),
            auditAction.getUserId(),
            auditAction.getNewValue() != null
                ? PaidAction.FILL_MODEL_PARAMETER
                : PaidAction.DELETE_MODEL_PARAMETER,
            auditAction.getDate());
    }
}
