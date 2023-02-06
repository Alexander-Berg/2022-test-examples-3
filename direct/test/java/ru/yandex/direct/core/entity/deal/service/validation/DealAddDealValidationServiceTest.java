package ru.yandex.direct.core.entity.deal.service.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matcher;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealBase;
import ru.yandex.direct.core.entity.deal.model.StatusDirect;
import ru.yandex.direct.core.entity.deal.repository.DealRepository;
import ru.yandex.direct.core.entity.deal.service.DealTransitionsService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.core.entity.deal.service.validation.DealDefects.dealCurrencyShouldMatchClient;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.concat;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class DealAddDealValidationServiceTest {

    private DealValidationService dealValidationService;

    private long dealId1 = 11L;
    private CurrencyCode defaultCurrencyCode = CurrencyCode.RUB;

    private Map<Long, DealBase> existingDeals = existingDeals();

    private int shard = 1;
    private ClientId agencyId = ClientId.fromLong(111L);

    @Before
    public void setUp() {
        RbacService rbacService = mock(RbacService.class);
        DealRepository dealRepository = mock(DealRepository.class);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        DealTransitionsService dealTransitionsService = new DealTransitionsService();
        ClientService clientService = mock(ClientService.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        DslContextProvider dslContextProvider = mock(DslContextProvider.class);

        dealValidationService = new DealValidationService(rbacService, dealRepository, campaignRepository,
                dealTransitionsService, clientService, shardHelper, dslContextProvider);

        dealValidationService = spy(dealValidationService);

        doReturn(mock(DSLContext.class))
                .when(dslContextProvider).ppc(anyInt());

        doReturn(existingDeals)
                .when(dealValidationService).getExistingDeals(eq(shard), eq(agencyId));

        doReturn(new Client()
                .withId(agencyId.asLong())
                .withWorkCurrency(defaultCurrencyCode)
        ).when(clientService).getClient(agencyId);
    }

    @Test
    public void validateAddDeal_validateEmptyDeal() {
        ValidationResult<List<Deal>, Defect> vr =
                dealValidationService.validateAddDeal(agencyId, singletonList(new Deal()));

        Path elementPath = path(index(0));
        assertManyDefects(vr, asList(
                defectInfoMatcher(elementPath, Deal.ID, notNull()),
                defectInfoMatcher(elementPath, Deal.CLIENT_ID, notNull()),
                defectInfoMatcher(elementPath, Deal.ADFOX_STATUS, notNull()),
                defectInfoMatcher(elementPath, Deal.DEAL_TYPE, notNull()),
                defectInfoMatcher(elementPath, Deal.CURRENCY_CODE, notNull()),
                defectInfoMatcher(elementPath, Deal.ADFOX_NAME, notNull()),
                defectInfoMatcher(elementPath, Deal.DATE_START, notNull()),
                defectInfoMatcher(elementPath, Deal.DATE_END, notNull()),
                defectInfoMatcher(elementPath, Deal.CPM, notNull()),
                defectInfoMatcher(elementPath, Deal.DEAL_JSON, notNull())
        ));
    }

    @Test
    @Ignore("todo maxlog: разигнорить после фикса в DealValidationService")
    public void validateAddDeal_wrongCurrency() {
        Deal deal = new Deal();
        deal.withCurrencyCode(CurrencyCode.BYN);
        ValidationResult<List<Deal>, Defect> vr =
                dealValidationService.validateAddDeal(agencyId, singletonList(deal));

        Path elementPath = path(index(0));
        assertManyDefects(vr, singletonList(
                defectInfoMatcher(elementPath, Deal.CURRENCY_CODE, dealCurrencyShouldMatchClient(defaultCurrencyCode))
        ));
    }


    private static Matcher<DefectInfo<Defect>> defectInfoMatcher(
            Path parentPath,
            ModelProperty<?, ?> prop,
            Defect<?> defect) {
        return validationError(concat(parentPath, path(propField(prop))), defect);
    }

    private static void assertManyDefects(ValidationResult<?, Defect> vr,
                                          Collection<Matcher<DefectInfo<Defect>>> defectMatchers) {
        SoftAssertions softly = new SoftAssertions();

        for (Matcher<DefectInfo<Defect>> defectMatcher : defectMatchers) {
            softly.assertThat(vr).is(matchedBy(hasDefectDefinitionWith(defectMatcher)));
        }

        softly.assertAll();
    }

    private static PathNode.Field propField(ModelProperty<?, ?> prop) {
        return field(prop.name());
    }

    private Map<Long, DealBase> existingDeals() {
        Map<Long, DealBase> result = new HashMap<>();
        Deal deal = defaultDeal(dealId1);
        result.put(deal.getId(), deal);

        return result;
    }

    private Deal defaultDeal(Long dealId) {
        Deal deal = new Deal();
        deal.withId(dealId)
                .withDirectStatus(StatusDirect.ACTIVE)
                .withCurrencyCode(defaultCurrencyCode);
        return deal;
    }
}
