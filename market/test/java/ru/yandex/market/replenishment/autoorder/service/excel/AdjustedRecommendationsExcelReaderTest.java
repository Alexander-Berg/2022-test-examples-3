package ru.yandex.market.replenishment.autoorder.service.excel;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationsDTO;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.CorrectionReason;
import ru.yandex.market.replenishment.autoorder.repository.postgres.CorrectionReasonRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AdjustedRecommendationsExcelReaderTest extends FunctionalTest {

    @Autowired
    CorrectionReasonRepository correctionReasonRepository;

    @Test
    public void testExcelParsing() throws IllegalStateException {
        InputStream bytes = this.getClass().getResourceAsStream("AdjustedRecommendationsExcelReaderTest.adjust.xlsx");
        if (bytes == null) {
            throw new IllegalStateException("Could not locate AdjustedRecommendationsExcelReaderTest.adjust.xlsx");
        }

        AdjustedRecommendationsExcelReader reader = new AdjustedRecommendationsExcelReader(getCorrectionReasonMap());
        AdjustedRecommendationsDTO recommendationsDTO = reader.read(bytes, DemandType.TYPE_1P);

        final Long demandId = recommendationsDTO.getDemandId();

        assertNotNull(demandId);
        assertEquals(2465106, demandId);

        List<AdjustedRecommendationDTO> adjustedRecommendations = recommendationsDTO.getAdjustedRecommendations();
        assertNotNull(adjustedRecommendations);
        assertEquals(2, adjustedRecommendations.size());

        AdjustedRecommendationDTO adjustedRecommendation = adjustedRecommendations.get(0);
        assertNotNull(adjustedRecommendation);

        assertEquals(0, adjustedRecommendation.getGroupId());
        assertEquals(10750562, adjustedRecommendation.getMsku());
        assertEquals(3, adjustedRecommendation.getAdjustedPurchQty());
        assertEquals(8, adjustedRecommendation.getCorrectionReason());

        adjustedRecommendation = adjustedRecommendations.get(1);
        assertNotNull(adjustedRecommendation);

        assertEquals(0, adjustedRecommendation.getGroupId());
        assertEquals(10750563, adjustedRecommendation.getMsku());
        assertEquals(5, adjustedRecommendation.getAdjustedPurchQty());
        assertEquals(1, adjustedRecommendation.getCorrectionReason());
    }

    private Map<String, Long> getCorrectionReasonMap() {
        return correctionReasonRepository.findAll().stream()
            .collect(Collectors.toMap(CorrectionReason::getName, CorrectionReason::getPosition));
    }
}
