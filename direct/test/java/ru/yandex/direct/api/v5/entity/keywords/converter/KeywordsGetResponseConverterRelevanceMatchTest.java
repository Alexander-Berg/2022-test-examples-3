package ru.yandex.direct.api.v5.entity.keywords.converter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.general.AutotargetingCategoriesEnum;
import com.yandex.direct.api.v5.general.AutotargetingCategory;
import com.yandex.direct.api.v5.general.PriorityEnum;
import com.yandex.direct.api.v5.general.ServingStatusEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.keywords.KeywordGetItem;
import com.yandex.direct.api.v5.keywords.ObjectFactory;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.keywords.container.KeywordsGetContainer;
import ru.yandex.direct.api.v5.entity.keywords.service.StatisticService;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.api.v5.common.ConverterUtils.convertToMicros;

public class KeywordsGetResponseConverterRelevanceMatchTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1111L);
    private static final long ADGROUP_ID = 10L;

    private static final Comparator<AutotargetingCategory> AUTOTARGETING_CATEGORY_COMPARATOR =
            Comparator.comparing(AutotargetingCategory::getCategory);

    public KeywordsGetResponseConverter converter;
    private AdGroupService adGroupService;
    private static final ObjectFactory FACTORY = new ObjectFactory();

    @Before
    public void setUp() {
        adGroupService = mock(AdGroupService.class);
        when(adGroupService.getAdGroups(
                eq(CLIENT_ID), eq(Collections.singletonList(ADGROUP_ID))))
                .thenReturn(singletonList(new AdGroup()
                        .withId(ADGROUP_ID)
                        .withType(AdGroupType.BASE)
                        .withBsRarelyLoaded(true)));
        ClientService clientService = mock(ClientService.class);
        when(clientService.getClient(CLIENT_ID))
                .thenReturn(new Client().withWorkCurrency(CurrencyCode.RUB).withClientId(CLIENT_ID.asLong()));
        converter = new KeywordsGetResponseConverter(adGroupService, clientService, mock(StatisticService.class));
    }

    @Test
    public void idIsConverted() {
        Long id = 1L;
        RelevanceMatch relevanceMatch = buildRelevanceMatch().withId(id);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(relevanceMatch)), CLIENT_ID, false).get(0));
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    public void adGroupIdIsConverted() {
        Long id = 1L;
        when(adGroupService.getAdGroups(
                eq(CLIENT_ID), eq(Collections.singletonList(id))))
                .thenReturn(singletonList(new AdGroup()
                        .withId(id)
                        .withType(AdGroupType.BASE)
                        .withBsRarelyLoaded(true)));
        RelevanceMatch relevanceMatch = buildRelevanceMatch().withAdGroupId(id);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(relevanceMatch)), CLIENT_ID, false).get(0));
        assertThat(result.getAdGroupId()).isEqualTo(id);
    }

    @Test
    public void campaignIdIsConverted() {
        Long id = 1L;
        RelevanceMatch relevanceMatch = buildRelevanceMatch().withCampaignId(id);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(relevanceMatch)), CLIENT_ID, false).get(0));
        assertThat(result.getCampaignId()).isEqualTo(id);
    }

    @Test
    public void userParam1IsConverted() {
        String userParam = "smile";
        JAXBElement<String> userParam1 = FACTORY.createKeywordUpdateItemUserParam1(userParam);
        RelevanceMatch relevanceMatch = buildRelevanceMatch().withHrefParam1(userParam);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(relevanceMatch)), CLIENT_ID, false).get(0));
        assertThat(result.getUserParam1().getValue()).isEqualTo(userParam1.getValue());
    }

    @Test
    public void userParam2IsConverted() {
        String userParam = "onemoresmile";
        JAXBElement<String> userParam2 = FACTORY.createKeywordGetItemUserParam2(userParam);
        RelevanceMatch relevanceMatch = buildRelevanceMatch().withHrefParam2(userParam);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(relevanceMatch)), CLIENT_ID, false).get(0));
        assertThat(result.getUserParam2().getValue()).isEqualTo(userParam2.getValue());
    }

    @Test
    public void bidIsConverted() {
        BigDecimal price = BigDecimal.valueOf(666L);
        RelevanceMatch relevanceMatch = buildRelevanceMatch().withPrice(price);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(relevanceMatch)), CLIENT_ID, false).get(0));
        assertThat(result.getBid()).isEqualTo(convertToMicros(price));
    }

    @Test
    public void contextBidIsConverted() {
        BigDecimal price = BigDecimal.valueOf(666L);
        RelevanceMatch relevanceMatch = buildRelevanceMatch().withPriceContext(price);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(relevanceMatch)), CLIENT_ID, false).get(0));
        assertThat(result.getContextBid()).isEqualTo(convertToMicros(price));
    }

    @Test
    public void strategyPriorityIsConverted() {
        JAXBElement<PriorityEnum> priority = FACTORY.createKeywordGetItemStrategyPriority(PriorityEnum.HIGH);
        RelevanceMatch relevanceMatch = buildRelevanceMatch().withAutobudgetPriority(5);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(relevanceMatch)), CLIENT_ID, false).get(0));
        assertThat(result.getStrategyPriority().getValue()).isEqualTo(priority.getValue());
    }

    @Test
    public void servingStatusIsConverted() {
        Long id = 2L;
        when(adGroupService.getAdGroups(
                eq(CLIENT_ID), eq(Collections.singleton(id))))
                .thenReturn(singletonList(new AdGroup().withId(id).withBsRarelyLoaded(false)));
        RelevanceMatch relevanceMatch = buildRelevanceMatch();
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(relevanceMatch)), CLIENT_ID, false).get(0));
        assertThat(result.getServingStatus()).isEqualTo(ServingStatusEnum.RARELY_SERVED);
    }

    @Test
    public void autotargetingCategoriesIsConverted() {
        Set<RelevanceMatchCategory> relevanceMatchCategories = asSet(RelevanceMatchCategory.exact_mark);
        List<AutotargetingCategory> autotargetingCategories = asList(new AutotargetingCategory()
                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                        .withValue(YesNoEnum.YES),
                new AutotargetingCategory()
                        .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                        .withValue(YesNoEnum.NO),
                new AutotargetingCategory()
                        .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                        .withValue(YesNoEnum.NO),
                new AutotargetingCategory()
                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                        .withValue(YesNoEnum.NO),
                new AutotargetingCategory()
                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                        .withValue(YesNoEnum.NO));
        RelevanceMatch relevanceMatch = buildRelevanceMatch().withRelevanceMatchCategories(relevanceMatchCategories);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(relevanceMatch)), CLIENT_ID, false).get(0));
        autotargetingCategories.sort(AUTOTARGETING_CATEGORY_COMPARATOR);
        result.getAutotargetingCategories().getValue().getItems().sort(AUTOTARGETING_CATEGORY_COMPARATOR);
        MatcherAssert.assertThat(result.getAutotargetingCategories().getValue().getItems(),
                beanDiffer(autotargetingCategories));
    }

    @Test
    public void autotargetingCategoriesIsConvertedForMobileContent() {
        when(adGroupService.getAdGroups(
                eq(CLIENT_ID), eq(Collections.singletonList(ADGROUP_ID))))
                .thenReturn(singletonList(new AdGroup()
                        .withId(ADGROUP_ID)
                        .withType(AdGroupType.MOBILE_CONTENT)
                        .withBsRarelyLoaded(true)));
        Set<RelevanceMatchCategory> relevanceMatchCategories = asSet(RelevanceMatchCategory.exact_mark);
        RelevanceMatch relevanceMatch = buildRelevanceMatch().withRelevanceMatchCategories(relevanceMatchCategories);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(relevanceMatch)), CLIENT_ID, false).get(0));
        assertNull(result.getAutotargetingCategories().getValue());
    }

    //region utils
    private RelevanceMatch buildRelevanceMatch() {
        return new RelevanceMatch()
                .withId(0L)
                .withAdGroupId(ADGROUP_ID)
                .withPrice(BigDecimal.TEN)
                .withPriceContext(BigDecimal.TEN)
                .withIsSuspended(true);
    }

    private KeywordsGetContainer getContainer(RelevanceMatch relevanceMatch) {
        return KeywordsGetContainer.createItemForRelevanceMatch(relevanceMatch);
    }
    //endregion
}

