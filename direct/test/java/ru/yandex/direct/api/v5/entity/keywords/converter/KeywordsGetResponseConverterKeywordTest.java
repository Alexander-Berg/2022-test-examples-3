package ru.yandex.direct.api.v5.entity.keywords.converter;

import java.math.BigDecimal;
import java.util.Collections;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.general.PriorityEnum;
import com.yandex.direct.api.v5.general.ServingStatusEnum;
import com.yandex.direct.api.v5.general.StateEnum;
import com.yandex.direct.api.v5.general.StatusEnum;
import com.yandex.direct.api.v5.keywords.KeywordGetItem;
import com.yandex.direct.api.v5.keywords.ObjectFactory;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.keywords.container.KeywordsGetContainer;
import ru.yandex.direct.api.v5.entity.keywords.service.StatisticService;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.common.ConverterUtils.convertToMicros;

public class KeywordsGetResponseConverterKeywordTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1111L);
    private static final long ADGROUP_ID = 10L;
    private static final ObjectFactory FACTORY = new ObjectFactory();

    public KeywordsGetResponseConverter converter;
    private AdGroupService adGroupService;

    @Before
    public void setUp() {
        ClientService clientService = mock(ClientService.class);
        when(clientService.getClient(CLIENT_ID))
                .thenReturn(new Client().withWorkCurrency(CurrencyCode.RUB).withClientId(CLIENT_ID.asLong()));
        adGroupService = mock(AdGroupService.class);
        when(adGroupService.getAdGroups(
                eq(CLIENT_ID), eq(Collections.singletonList(ADGROUP_ID))))
                .thenReturn(singletonList(new AdGroup().withId(ADGROUP_ID).withBsRarelyLoaded(true)));
        converter = new KeywordsGetResponseConverter(adGroupService, clientService, mock(StatisticService.class));
    }

    @Test
    public void idIsConverted() {
        Long id = 1L;
        Keyword keyword = buildKeyword().withId(id);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    public void adGroupIdIsConverted() {
        Long id = 1L;
        when(adGroupService.getAdGroups(
                eq(CLIENT_ID), eq(Collections.singletonList(id))))
                .thenReturn(singletonList(new AdGroup().withId(id).withBsRarelyLoaded(true)));
        Keyword keyword = buildKeyword().withAdGroupId(id);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getAdGroupId()).isEqualTo(id);
    }

    @Test
    public void campaignIdIsConverted() {
        Long id = 1L;
        Keyword keyword = buildKeyword().withCampaignId(id);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getCampaignId()).isEqualTo(id);
    }

    @Test
    public void keywordIsConverted() {
        String phrase = "phrase";
        Keyword keyword = buildKeyword().withPhrase(phrase);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getKeyword()).isEqualTo(phrase);
    }

    @Test
    public void userParam1IsConverted() {
        String userParam = "smile";
        JAXBElement<String> userParam1 = FACTORY.createKeywordUpdateItemUserParam1(userParam);
        Keyword keyword = buildKeyword().withHrefParam1(userParam);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getUserParam1().getValue()).isEqualTo(userParam1.getValue());
    }

    @Test
    public void userParam2IsConverted() {
        String userParam = "onemoresmile";
        JAXBElement<String> userParam2 = FACTORY.createKeywordGetItemUserParam2(userParam);
        Keyword keyword = buildKeyword().withHrefParam2(userParam);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getUserParam2().getValue()).isEqualTo(userParam2.getValue());
    }

    @Test
    public void bidIsConverted() {
        BigDecimal price = BigDecimal.valueOf(666L);
        Keyword keyword = buildKeyword().withPrice(price);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getBid()).isEqualTo(convertToMicros(price));
    }

    @Test
    public void contextBidIsConverted() {
        BigDecimal price = BigDecimal.valueOf(666L);
        Keyword keyword = buildKeyword().withPriceContext(price);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getContextBid()).isEqualTo(convertToMicros(price));
    }

    @Test
    public void strategyPriorityIsConverted() {
        JAXBElement<PriorityEnum> priority = FACTORY.createKeywordGetItemStrategyPriority(PriorityEnum.HIGH);
        Keyword keyword = buildKeyword().withAutobudgetPriority(5);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getStrategyPriority().getValue()).isEqualTo(priority.getValue());
    }

    @Test
    public void stateIsConverted() {
        Keyword keyword = buildKeyword().withIsSuspended(true);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getState()).isEqualTo(StateEnum.SUSPENDED);
    }

    @Test
    public void statusIsConverted() {
        Keyword keyword = buildKeyword().withStatusModerate(StatusModerate.NEW);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getStatus()).isEqualTo(StatusEnum.DRAFT);
    }

    @Test
    public void servingStatusIsConverted() {
        Long id = 2L;
        when(adGroupService.getAdGroups(
                eq(CLIENT_ID), eq(Collections.singletonList(id))))
                .thenReturn(singletonList(new AdGroup().withId(id).withBsRarelyLoaded(false)));
        Keyword keyword = buildKeyword().withStatusModerate(StatusModerate.NEW);
        KeywordGetItem result =
                checkNotNull(converter.convert(singletonList(getContainer(keyword)), CLIENT_ID, false).get(0));
        assertThat(result.getServingStatus()).isEqualTo(ServingStatusEnum.RARELY_SERVED);
    }

    //region utils
    private Keyword buildKeyword() {
        return new Keyword()
                .withId(0L)
                .withAdGroupId(ADGROUP_ID)
                .withPrice(BigDecimal.TEN)
                .withPriceContext(BigDecimal.TEN)
                .withIsSuspended(true)
                .withStatusModerate(StatusModerate.NEW);
    }

    private KeywordsGetContainer getContainer(Keyword keyword) {
        return KeywordsGetContainer.createItemForKeyword(keyword);
    }

    //endregion
}
