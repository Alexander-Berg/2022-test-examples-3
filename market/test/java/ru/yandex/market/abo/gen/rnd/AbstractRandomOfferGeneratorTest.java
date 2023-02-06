package ru.yandex.market.abo.gen.rnd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.calendar.db.CalendarService;
import ru.yandex.market.abo.core.dynamic.service.GeneratorRepository;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.offer.report.ReportParam;
import ru.yandex.market.abo.core.offer.report.ShopSwitchedOffException;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.shop.CommonShopInfoService;
import ru.yandex.market.abo.gen.GeneratorManager;
import ru.yandex.market.abo.gen.model.GeneratorProfile;
import ru.yandex.market.abo.gen.model.Hypothesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author Vasiliy Briginets (0x40@yandex-team.ru)
 */
public class AbstractRandomOfferGeneratorTest extends EmptyTest {

    @Autowired
    @InjectMocks
    private RandomClickedOfferGenerator randomClickedOfferGenerator;
    @Autowired
    @InjectMocks
    private RandomPopularOfferGenerator randomPopularOfferGenerator;
    @Autowired
    @InjectMocks
    private GeneratorManager generatorManager;
    @Autowired
    private GeneratorRepository generatorRepository;
    @Mock
    private CalendarService calendarService;
    @Mock
    private JdbcTemplate clickhouseJdbcTemplate;
    @Mock
    private OfferService offerService;
    @Mock
    private CommonShopInfoService shopInfoService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void init() throws ShopSwitchedOffException {
        MockitoAnnotations.openMocks(this);
        doAnswer(invocation -> new ArrayList<>(
                Collections.nCopies(
                        (Integer) invocation.getArguments()[3],
                        Hypothesis.builder(0, 0).withWareMd5("").build()
                )
        )).when(clickhouseJdbcTemplate).query(any(), Mockito.<RowMapper>any(), any());

        doAnswer(
                invocation -> {
                    String wareMd5 = (String) ((ReportParam) invocation.getArguments()[3]).getValue();
                    Offer offer = new Offer();
                    offer.setWareMd5(wareMd5);
                    return offer;
                }
        ).when(offerService).findFirstWithParams(Mockito.<ReportParam>any());

        when(shopInfoService.getShopOwnRegions(anyLong())).thenReturn(List.of((long) Regions.MOSCOW));

        GeneratorProfile profileRandom = generatorRepository.findByIdOrNull(97);
        randomClickedOfferGenerator.configure(profileRandom);
        GeneratorProfile profilePopular = generatorRepository.findByIdOrNull(99);
        randomPopularOfferGenerator.configure(profilePopular);
    }

    @Test
    public void popularGeneratorTest() {
        generatorTest(randomPopularOfferGenerator);
    }

    @Test
    public void clickedGeneratorTest() {
        generatorTest(randomClickedOfferGenerator);
    }

    private void generatorTest(AbstractRandomOfferGenerator generator) {
        int size = generator.generate().size();
        assertEquals(100, size);
    }
}
