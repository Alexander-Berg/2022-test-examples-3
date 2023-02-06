package ru.yandex.direct.grid.processing.service.cashback;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.cashback.model.CashbackCardsProgram;
import ru.yandex.direct.core.entity.cashback.model.CashbackCategory;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgram;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgramCategory;
import ru.yandex.direct.core.entity.cashback.repository.CashbackCategoriesRepository;
import ru.yandex.direct.core.entity.cashback.repository.CashbackProgramsCategoriesRepository;
import ru.yandex.direct.core.entity.cashback.repository.CashbackProgramsRepository;
import ru.yandex.direct.core.entity.cashback.service.CashbackClientsService;
import ru.yandex.direct.core.entity.cashback.service.CashbackProgramsService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.cashback.GdCashbackCardsInfo;
import ru.yandex.direct.grid.processing.model.cashback.GdCashbackCategoryInfo;
import ru.yandex.direct.grid.processing.model.cashback.GdCashbackProgramInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class CashbackInfoServiceTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final String LANGUAGE_RU = "RU";

    @Mock
    private CashbackClientsService cashbackClientsService;

    @Mock
    private ClientService clientService;

    @Mock
    private CashbackProgramsCategoriesRepository cashbackProgramsCategoriesRepository;

    @Mock
    private CashbackProgramsRepository cashbackProgramsRepository;

    @Mock
    private CashbackCategoriesRepository cashbackCategoriesRepository;

    @Mock
    private CashbackProgramsService cashbackProgramsService;

    private CashbackInfoService service;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

        var defaultClient = new Client()
                .withClientId(CLIENT_ID.asLong())
                .withCashBackBonus(BigDecimal.TEN)
                .withCashBackAwaitingBonus(BigDecimal.ONE)
                .withWorkCurrency(CurrencyCode.RUB);
        doReturn(defaultClient).when(clientService).getClient(any());
        doReturn(CurrencyRub.getInstance()).when(clientService).getWorkCurrency(any());

        doReturn(Map.of(1L, List.of(PROGRAM_CATEORY_1), 2L, List.of(PROGRAM_CATEORY_2)))
                .when(cashbackProgramsCategoriesRepository).getCategoriesByPrograms(List.of(1L, 2L));

        doReturn(Map.of(1L, CARDS_PROGRAM_1, 2L, CARDS_PROGRAM_2))
                .when(cashbackProgramsRepository).getCardsProgramsByIds(List.of(1L, 2L));

        doReturn(CATEGORY_1).when(cashbackCategoriesRepository).get(1L);
        doReturn(CATEGORY_2).when(cashbackCategoriesRepository).get(2L);

        doReturn(List.of(PROGRAM_1, PROGRAM_2)).when(cashbackProgramsService).getClientPrograms(any());


        service = new CashbackInfoService(cashbackClientsService, clientService, cashbackProgramsCategoriesRepository,
                cashbackProgramsRepository, cashbackCategoriesRepository, cashbackProgramsService);
    }

    @Test
    public void testCashbackCardsInfo() {
        var result = service.getCashbackCardsInfo(CLIENT_ID, LANGUAGE_RU);
        var expected = new GdCashbackCardsInfo()
                .withMaxCategoryPercent(15)
                .withMaxCategoryPercentDecimal(BigDecimal.valueOf(0.15))
                .withCategories(List.of(
                        new GdCashbackCategoryInfo()
                                .withCategoryId(CATEGORY_1.getId())
                                .withName(CATEGORY_1.getNameRu())
                                .withDescription(CATEGORY_1.getDescriptionRu())
                                .withIsTechnical(false)
                                .withIsNew(false)
                                .withIsGeneral(false)
                                .withMaxPercent(10)
                                .withMaxPercentDecimal(BigDecimal.valueOf(0.1))
                                .withPrograms(List.of(new GdCashbackProgramInfo()
                                        .withName(CARDS_PROGRAM_1.getNameRu())
                                        .withProgramId(CARDS_PROGRAM_1.getId())
                                        .withPercent(10)
                                        .withPercentDecimal(BigDecimal.valueOf(0.1))
                                        .withOrder(PROGRAM_CATEORY_1.getOrder()))),
                        new GdCashbackCategoryInfo()
                                .withCategoryId(CATEGORY_2.getId())
                                .withName(CATEGORY_2.getNameRu())
                                .withDescription(CATEGORY_2.getDescriptionRu())
                                .withIsTechnical(false)
                                .withIsNew(false)
                                .withIsGeneral(true)
                                .withMaxPercent(15)
                                .withMaxPercentDecimal(BigDecimal.valueOf(0.15))
                                .withPrograms(List.of(new GdCashbackProgramInfo()
                                        .withName(CARDS_PROGRAM_2.getNameRu())
                                        .withProgramId(CARDS_PROGRAM_2.getId())
                                        .withPercent(15)
                                        .withPercentDecimal(BigDecimal.valueOf(0.15))
                                        .withOrder(PROGRAM_CATEORY_2.getOrder())))));
        assertThat(result).is(matchedBy(beanDiffer(expected)));
    }

    private static final CashbackProgram PROGRAM_1 = new CashbackProgram()
            .withId(1L);
    private static final CashbackProgram PROGRAM_2 = new CashbackProgram()
            .withId(2L);
    private static final CashbackCategory CATEGORY_1 = new CashbackCategory()
            .withId(1L)
            .withNameRu("CATEGORY_1")
            .withDescriptionRu("desrc1");
    private static final CashbackCategory CATEGORY_2 = new CashbackCategory()
            .withId(2L)
            .withNameRu("CATEGORY_2")
            .withDescriptionRu("desrc2");
    private static final CashbackProgramCategory PROGRAM_CATEORY_1 = new CashbackProgramCategory()
            .withProgramId(PROGRAM_1.getId())
            .withCategoryId(CATEGORY_1.getId())
            .withId(1L)
            .withOrder(1L);
    private static final CashbackProgramCategory PROGRAM_CATEORY_2 = new CashbackProgramCategory()
            .withProgramId(PROGRAM_2.getId())
            .withCategoryId(CATEGORY_2.getId())
            .withId(2L)
            .withOrder(1L);
    private static final CashbackCardsProgram CARDS_PROGRAM_1 = new CashbackCardsProgram()
            .withId(1L)
            .withNameRu("PROGRAM_1")
            .withIsTechnical(false)
            .withIsGeneral(false)
            .withIsNew(false)
            .withPercent(BigDecimal.valueOf(0.1));
    private static final CashbackCardsProgram CARDS_PROGRAM_2 = new CashbackCardsProgram()
            .withId(2L)
            .withNameRu("PROGRAM_2")
            .withIsTechnical(false)
            .withIsGeneral(true)
            .withIsNew(false)
            .withPercent(BigDecimal.valueOf(0.15));
}
