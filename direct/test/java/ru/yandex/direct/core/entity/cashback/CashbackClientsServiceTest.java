package ru.yandex.direct.core.entity.cashback;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.cashback.model.CashbackClientInfo;
import ru.yandex.direct.core.entity.cashback.model.CashbackClientProgram;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgram;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgramDetails;
import ru.yandex.direct.core.entity.cashback.model.CashbackRewardsDetails;
import ru.yandex.direct.core.entity.cashback.repository.CashbackClientsRepository;
import ru.yandex.direct.core.entity.cashback.service.CashbackClientsService;
import ru.yandex.direct.core.entity.cashback.service.CashbackProgramsService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.ClientNds;
import ru.yandex.direct.core.entity.client.service.ClientNdsService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.math.RoundingMode.DOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.steps.CashbackSteps.getTechnicalProgram;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class CashbackClientsServiceTest {
    private static final LocalDate NOW = LocalDate.now();
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final BigDecimal TEN_WITHOUT_NDS = new BigDecimal("8.33");
    private static final BigDecimal ONE_WITHOUT_NDS = new BigDecimal("0.83");

    private static final CashbackProgramDetails DETAILS_1 = new CashbackProgramDetails()
            .withProgramId(getTechnicalProgram().getId())
            .withReward(BigDecimal.TEN)
            .withRewardWithoutNds(TEN_WITHOUT_NDS)
            .withDate(LocalDate.now());

    private static final CashbackProgramDetails DETAILS_2 = new CashbackProgramDetails()
            .withProgramId(getTechnicalProgram().getId())
            .withReward(BigDecimal.TEN)
            .withRewardWithoutNds(TEN_WITHOUT_NDS)
            .withDate(LocalDate.now().minusMonths(1L));

    @Mock
    private ClientNdsService clientNdsService;

    @Mock
    private ClientService clientService;

    @Mock
    private CashbackProgramsService cashbackProgramsService;

    @Mock
    private CashbackClientsRepository cashbackClientsRepository;

    @Mock
    private TranslationService translationService;

    private CashbackClientsService service;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        var defaultClient = new Client()
                .withClientId(CLIENT_ID.asLong())
                .withCashBackBonus(BigDecimal.TEN)
                .withCashBackAwaitingBonus(BigDecimal.ONE)
                .withWorkCurrency(CurrencyCode.RUB);
        doReturn(defaultClient).when(clientService).getClient(any());
        doReturn(CurrencyRub.getInstance()).when(clientService).getWorkCurrency(any());

        var technicalProgram = getTechnicalProgram();
        doReturn(List.of()).when(cashbackProgramsService).getClientPrograms(any());
        doReturn(Map.of(technicalProgram.getId(), true)).when(cashbackClientsRepository).getClientProgramStates(any());

        doReturn(List.of(DETAILS_1, DETAILS_2)).when(cashbackClientsRepository).getProgramDetails(CLIENT_ID,
                NOW.minusMonths(2L), NOW);

        var previousMonthDate = NOW.minusMonths(1L);
        var dateFrom = previousMonthDate.withDayOfMonth(1);
        var dateTo = previousMonthDate.withDayOfMonth(previousMonthDate.lengthOfMonth());
        doReturn(List.of(DETAILS_1)).when(cashbackClientsRepository).getProgramDetails(CLIENT_ID, dateFrom, dateTo);

        doReturn(Locale.forLanguageTag("ru")).when(translationService).getLocale();

        var clientNds = new ClientNds().withNds(Percent.fromPercent(new BigDecimal(20)));
        doReturn(clientNds).when(clientNdsService).getClientNds(any());

        service = new CashbackClientsService(clientNdsService, clientService, cashbackProgramsService,
                cashbackClientsRepository, translationService);
    }

    @Test
    public void testGetClientCashbackInfo() {
        var result = service.getClientCashbackInfo(CLIENT_ID);

        var expected = getDefaultInfo();
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetClientCashbackInfo_noBonuses() {
        var defaultClient = new Client()
                .withClientId(CLIENT_ID.asLong())
                .withCashBackBonus(null)
                .withCashBackAwaitingBonus(null)
                .withWorkCurrency(CurrencyCode.RUB);
        doReturn(defaultClient).when(clientService).getClient(any());

        var result = service.getClientCashbackInfo(CLIENT_ID);

        var expected = getDefaultInfo()
                .withTotalCashback(BigDecimal.ZERO)
                .withAwaitingCashback(BigDecimal.ZERO)
                .withAwaitingCashbackWithoutNds(BigDecimal.ZERO.setScale(2, DOWN))
                .withTotalCashbackWithoutNds(BigDecimal.ZERO.setScale(2, DOWN));
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetClientCashbackInfo_disabledProgram() {
        var technicalProgram = getTechnicalProgram();
        var disabledProgram = getProgram().withIsEnabled(false);
        doReturn(List.of(disabledProgram)).when(cashbackProgramsService).getClientPrograms(any());
        Map<Long, Boolean> states = Map.of(
                technicalProgram.getId(), true,
                disabledProgram.getId(), true);
        doReturn(states).when(cashbackClientsRepository).getClientProgramStates(any());

        var result = service.getClientCashbackInfo(CLIENT_ID);

        var expected = getDefaultInfo().withPrograms(List.of(
                getClientProgram().withEnabled(false)
        ));
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetClientCashbackInfo_hasRewardByProgram() {
        var technicalProgram = getTechnicalProgram();
        var program = getProgram();
        doReturn(List.of(program)).when(cashbackProgramsService).getClientPrograms(any());
        Map<Long, Boolean> states = Map.of(
                technicalProgram.getId(), true,
                program.getId(), true);
        doReturn(states).when(cashbackClientsRepository).getClientProgramStates(any());
        doReturn(Map.of(program.getId(), BigDecimal.TEN))
                .when(cashbackClientsRepository).getRewardsSumByPrograms(CLIENT_ID, List.of(program.getId()));

        var result = service.getClientCashbackInfo(CLIENT_ID);

        var expectedPrograms = List.of(getClientProgram().withHasRewards(true));
        var expected = getDefaultInfo().withPrograms(expectedPrograms);
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetClientCashbackInfo_disabledForClientProgram() {
        var technicalProgram = getTechnicalProgram();
        var program = getProgram();
        doReturn(List.of(program)).when(cashbackProgramsService).getClientPrograms(any());
        Map<Long, Boolean> states = Map.of(
                technicalProgram.getId(), true,
                program.getId(), false);
        doReturn(states).when(cashbackClientsRepository).getClientProgramStates(any());

        var result = service.getClientCashbackInfo(CLIENT_ID);

        var expected = getDefaultInfo()
                .withPrograms(List.of(getClientProgram().withEnabled(false)));
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetClientCashbackInfo_noStatePublicProgram() {
        doReturn(Map.of()).when(cashbackClientsRepository).getClientProgramStates(any());

        var result = service.getClientCashbackInfo(CLIENT_ID);

        var expected = getDefaultInfo();
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetClientCashbackInfo_noStatePrivateProgram() {
        var program = getProgram();
        doReturn(List.of(program)).when(cashbackProgramsService).getClientPrograms(any());
        doReturn(Map.of()).when(cashbackClientsRepository).getClientProgramStates(any());

        var result = service.getClientCashbackInfo(CLIENT_ID);

        var expected = getDefaultInfo()
                .withPrograms(List.of(getClientProgram().withEnabled(false)));
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetClientCashbackInfo_outOfCashbacks() {
        doReturn(Map.of(getTechnicalProgram().getId(), false)).when(cashbackClientsRepository).getClientProgramStates(any());

        var result = service.getClientCashbackInfo(CLIENT_ID);

        var expected = getDefaultInfo().withCashbacksEnabled(false);
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetClientCashbackInfo_nonRussianLanguage() {
        doReturn(Locale.forLanguageTag("en")).when(translationService).getLocale();

        var technicalProgram = getTechnicalProgram();
        var program = getProgram();
        doReturn(List.of(program)).when(cashbackProgramsService).getClientPrograms(any());
        Map<Long, Boolean> states = Map.of(
                technicalProgram.getId(), true,
                program.getId(), true);
        doReturn(states).when(cashbackClientsRepository).getClientProgramStates(any());

        var result = service.getClientCashbackInfo(CLIENT_ID);

        var expected = getDefaultInfo()
                .withPrograms(List.of(
                        getClientProgram()
                                .withName(program.getCategoryNameEn())
                                .withDescription(program.getCategoryDescriptionEn())
                                .withEnabled(true)
                ));
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetClientCashbackRewardsDetails() {
        var result = service.getClientCashbackRewardsDetails(CLIENT_ID, 2);

        var expected = new CashbackRewardsDetails()
                .withTotalCashback(BigDecimal.TEN)
                .withTotalCashbackWithoutNds(TEN_WITHOUT_NDS)
                .withTotalByPrograms(List.of(DETAILS_1, DETAILS_2));
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetClientCashbackRewardsDetails_noRewards() {
        doReturn(List.of()).when(cashbackClientsRepository).getProgramDetails(CLIENT_ID, NOW.minusMonths(2L), NOW);

        var result = service.getClientCashbackRewardsDetails(CLIENT_ID, 2);

        var expected = new CashbackRewardsDetails()
                .withTotalCashback(BigDecimal.TEN)
                .withTotalCashbackWithoutNds(TEN_WITHOUT_NDS)
                .withTotalByPrograms(List.of());
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetClientCashbackRewardsDetails_singleMonth() {
        doReturn(List.of(DETAILS_1)).when(cashbackClientsRepository).getProgramDetails(CLIENT_ID, NOW.minusMonths(1L),
                NOW);

        var result = service.getClientCashbackRewardsDetails(CLIENT_ID, 1);

        var expected = new CashbackRewardsDetails()
                .withTotalCashback(BigDecimal.TEN)
                .withTotalCashbackWithoutNds(TEN_WITHOUT_NDS)
                .withTotalByPrograms(List.of(DETAILS_1));
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    private static CashbackClientProgram getDefaultClientProgram() {
        var technicalProgram = getTechnicalProgram();
        return new CashbackClientProgram()
                .withProgramId(technicalProgram.getId())
                .withName(technicalProgram.getCategoryNameRu())
                .withDescription(technicalProgram.getCategoryDescriptionRu())
                .withPercent(technicalProgram.getPercent())
                .withEnabled(true)
                .withHasRewards(false);
    }

    private static CashbackProgram getProgram() {
        return new CashbackProgram()
                .withId(2L)
                .withCategoryId(2L)
                .withPercent(BigDecimal.TEN)
                .withCategoryNameRu("Категория")
                .withCategoryDescriptionEn("Category")
                .withCategoryDescriptionRu("Категория")
                .withCategoryDescriptionEn("Category")
                .withIsPublic(false)
                .withIsEnabled(true);
    }

    private static CashbackClientProgram getClientProgram() {
        var program = getProgram();
        return new CashbackClientProgram()
                .withProgramId(program.getId())
                .withName(program.getCategoryNameRu())
                .withDescription(program.getCategoryDescriptionRu())
                .withPercent(program.getPercent())
                .withEnabled(true)
                .withHasRewards(false);
    }

    private static CashbackClientInfo getDefaultInfo() {
        return new CashbackClientInfo()
                .withCashbacksEnabled(true)
                .withTotalCashback(BigDecimal.TEN)
                .withAwaitingCashback(BigDecimal.ONE)
                .withLastMonthCashback(BigDecimal.TEN)
                .withLastMonthCashbackWithoutNds(TEN_WITHOUT_NDS)
                .withAwaitingCashbackWithoutNds(ONE_WITHOUT_NDS)
                .withTotalCashbackWithoutNds(TEN_WITHOUT_NDS)
                .withPrograms(List.of());
    }
}
