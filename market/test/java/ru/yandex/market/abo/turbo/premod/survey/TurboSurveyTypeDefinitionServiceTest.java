package ru.yandex.market.abo.turbo.premod.survey;

import java.util.HashSet;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.no_placement.NoPlacementManager;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.turbo.premod.survey.model.TurboSurveyTypeDefinition;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 27.07.2020
 */
class TurboSurveyTypeDefinitionServiceTest {
    @InjectMocks
    TurboSurveyTypeDefinitionService turboSurveyTypeDefinitionService;
    @Mock
    NoPlacementManager noPlacementManager;
    @Mock
    MbiApiService mbiApiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(noPlacementManager.getActiveReasons()).thenReturn(List.of());
        when(mbiApiService.getCommonDeliveryServices()).thenReturn(List.of());
    }

    @Test
    public void uniqueTypesTest() {
        var set = new HashSet<>();
        StreamEx.of(turboSurveyTypeDefinitionService.get())
                .map(TurboSurveyTypeDefinition::getType)
                .forEach(type -> assertTrue(set.add(type), type + " occurs more than once"));
    }
}
