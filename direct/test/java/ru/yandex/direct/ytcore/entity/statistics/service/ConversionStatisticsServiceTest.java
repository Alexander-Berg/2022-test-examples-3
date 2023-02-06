package ru.yandex.direct.ytcore.entity.statistics.service;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.ytcomponents.statistics.model.DateRange;
import ru.yandex.direct.ytcore.entity.statistics.repository.ConversionStatisticsRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConversionStatisticsServiceTest {

    private ConversionStatisticsRepository repository;
    private CampaignService campaignService;

    private ConversionStatisticsService service;

    @Before
    public void setUp() {
        repository = mock(ConversionStatisticsRepository.class);
        campaignService = mock(CampaignService.class);
        service = new ConversionStatisticsService(repository, campaignService);
        when(repository.getConversionsStatistics(anyCollection(), anyMap(), any(DateRange.class)))
                .thenReturn(List.of());
        when(campaignService.getSubCampaignIdsWithMasterIds(anyCollection()))
                .thenReturn(Map.of());
    }

    @Test
    public void getConversionStatisticsForTheLastYear() {
        var cid = 1L;
        var response = service.getConversionStatistics(List.of(cid));

        var captor = ArgumentCaptor.forClass(DateRange.class);
        Mockito.verify(repository).getConversionsStatistics(argThat(cids -> cids.contains(cid)), eq(Map.of()), captor.capture());

        var capturedValue = captor.getValue();

        assertThat(capturedValue.getFromInclusive()).isEqualTo(LocalDate.now().minusYears(1));
        assertThat(capturedValue.getToInclusive()).isEqualTo(LocalDate.now());
        assertThat(response).isEmpty();
    }

}
