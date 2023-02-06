package ru.yandex.market.psku.postprocessor.service.preparator;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.psku.postprocessor.MemorizingLongGenerator;
import ru.yandex.market.psku.postprocessor.TestDataGenerator;
import ru.yandex.market.psku.postprocessor.service.EnrichedPskuInfo;
import ru.yandex.market.psku.postprocessor.service.MockUtils;
import ru.yandex.market.psku.postprocessor.service.PskuInfo;
import ru.yandex.market.psku.postprocessor.service.uc.UCService;
import ru.yandex.market.psku.postprocessor.service.yt.YtDataService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.eq;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
@SuppressWarnings("checkstyle:magicnumber")
public class EnrichAndSavePskuJobTest {

    @Mock
    private YtDataService ytDataService;
    @Mock
    private UCService ucService;

    @Captor
    private ArgumentCaptor<List<EnrichedPskuInfo>> enrichedPskuInfoListCaptor;

    private List<PskuInfo> testPskuInfos;
    private Map<PskuInfo, UltraController.EnrichedOffer> pskuInfoToUCOffer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        testPskuInfos = new ArrayList<>();
        pskuInfoToUCOffer = new HashMap<>();
    }

    @Test
    public void whenEnrichAndSavePskusOk() {
        testPskuInfos.addAll(Arrays.asList(
                PskuInfo.builder().setId(1L).setTitle("psku_1").build(),
                PskuInfo.builder().setId(2L).setTitle("psku_2").build(),
                PskuInfo.builder().setId(3L).setTitle("psku_3").build()));

        MemorizingLongGenerator idsGenerator = new MemorizingLongGenerator();

        testPskuInfos.forEach(pskuInfo -> pskuInfoToUCOffer.put(
                pskuInfo, TestDataGenerator.generateUCOffer(pskuInfo, idsGenerator)));

        MockUtils.mockUCService(ucService, testPskuInfos, pskuInfoToUCOffer);

        EnrichAndSavePskuJob.with(ytDataService, ucService)
                .enrichAndSavePskus(MockUtils.TEST_SESSION_NAME, testPskuInfos);

        List<EnrichedPskuInfo> enrichedPskuInfos = testPskuInfos.stream()
                .map(pskuInfo -> EnrichedPskuInfo.builder()
                        .setPskuInfo(pskuInfo)
                        .setUcEnrichedOffer(pskuInfoToUCOffer.get(pskuInfo))
                        .build())
                .collect(Collectors.toList());

        Mockito.verify(ytDataService)
                .saveEnrichedPskus(eq(MockUtils.TEST_SESSION_NAME), enrichedPskuInfoListCaptor.capture());
        Assertions.assertThat(enrichedPskuInfoListCaptor.getValue())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(enrichedPskuInfos);
    }
}
