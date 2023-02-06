package ru.yandex.market.mbo.db;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.gwt.models.gurulight.OfferData;

import java.util.Collections;
import java.util.List;

public class OfferServiceTest {

    private NamedParameterJdbcTemplate scatJdbcTemplateMock;
    private OfferService offerService;
    private NamedParameterJdbcTemplate scLogScatJdbcTemplate;
    private NamedParameterJdbcTemplate chytNamedJdbcTemplateMock;

    @Value("${mbo.yt.offers.path}/recent/medium_log")
    private String ytMediumLogTable;

    @Before
    public void setUp() {
        scatJdbcTemplateMock = Mockito.mock(NamedParameterJdbcTemplate.class);
        scLogScatJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        chytNamedJdbcTemplateMock = Mockito.mock(NamedParameterJdbcTemplate.class);
        offerService = new OfferService(scatJdbcTemplateMock, null, chytNamedJdbcTemplateMock).
            setYtMediumLogTable(ytMediumLogTable);
    }

    @Test
    public void whenPassingCorrectUrlShouldConvertIt() {
        Assertions.assertThat(OfferService.convertUrl("http://www.correct-url.com/pic1"))
            .isEqualTo("http://www.correct-url.com/pic1");
    }

    @Test
    public void whenPassingMultipleUrlsShouldReturnFirst() {
        Assertions.assertThat(OfferService
            .convertUrl("http://www.correct-url.com/pic1 www.pic2.com www.pic3.com"))
            .isEqualTo("http://www.correct-url.com/pic1");
    }

    @Test
    public void whenPassingNullOrEmptyUrlShouldReturnNull() {
        Assertions.assertThat(OfferService.convertUrl(null)).isNull();
        Assertions.assertThat(OfferService.convertUrl("")).isNull();
        Assertions.assertThat(OfferService.convertUrl("    ")).isNull();
    }

    @Test
    public void whenPassingUrlWithoutProtocolShouldReturnHttpProtocol() {
        Assertions.assertThat(OfferService.convertUrl("www.no-protocol.com/pic"))
            .isEqualTo("http://www.no-protocol.com/pic");
    }

    @Test
    public void whenModelIdIsNullShouldReturnEmptyList() {
        Assertions.assertThat(offerService.getOffersMatchedToModelId(null, 1)).isEmpty();
    }

    @Test
    public void whenGetOffersByModelIdShouldCallScatTemplate() {
        Mockito.when(scLogScatJdbcTemplate.query(Mockito.anyString(), Mockito.anyMap(), Mockito.any(RowMapper.class)))
            .thenReturn(Collections.emptyList());
        List<OfferData> foundOffers = offerService.getOffersMatchedToModelId(1L, 2);
        Mockito.verify(chytNamedJdbcTemplateMock, Mockito.times(1))
            .query(Mockito.anyString(), Mockito.anyMap(), Mockito.any(RowMapper.class));
        Assertions.assertThat(foundOffers).isEmpty();
    }
}
