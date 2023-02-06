package ru.yandex.direct.api.v5.configuration;

import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.common.validation.ApiDefectPresentation;
import ru.yandex.direct.api.v5.common.validation.DefaultApiPresentations;
import ru.yandex.direct.api.v5.common.validation.DefectPresentationService;
import ru.yandex.direct.api.v5.common.validation.DefectPresentationsHolder;
import ru.yandex.direct.api.v5.entity.audiencetargets.validation.AudienceTargetsDefectPresentations;
import ru.yandex.direct.api.v5.entity.bids.validation.BidsDefectPresentations;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.validation.result.DefectId;

import static ru.yandex.direct.core.validation.TestDefectIdMappingHelper.assertAllDefectIdIsMappedToPresentation;

/**
 * Тест находит все enum'ы, реализующие {@link DefectId} и проверяет, что для всех значений этих енумов
 * зарегистрированы представления в {@link DefectPresentationService} и известных {@link DefectPresentationsHolder}
 */
@Api5Test
@RunWith(SpringRunner.class)
public class DefectPresentationConfigurationTest {

    @Autowired
    private DefectPresentationService defectPresentationService;

    @Test
    public void presentationsAreRegisteredForAllDefectIdsInAudienceTargetsDefectPresentations() throws Exception {
        check(t -> getDefectPresentation(t, AudienceTargetsDefectPresentations.HOLDER));
    }

    @Test
    public void presentationsAreRegisteredForAllDefectIdsInBidsDefectPresentations() throws Exception {
        check(t -> getDefectPresentation(t, BidsDefectPresentations.HOLDER));
    }

    @Test
    public void presentationsAreRegisteredForAllDefectIdsInDefaultApiPresentations() throws Exception {
        check(t -> getDefectPresentation(t, DefaultApiPresentations.HOLDER));
    }

    private void check(Function<DefectId, ApiDefectPresentation> presentationFunction) {
        assertAllDefectIdIsMappedToPresentation(presentationFunction);
    }

    private ApiDefectPresentation getDefectPresentation(DefectId defectId,
                                                        DefectPresentationsHolder presentationsHolder) {
        try {
            return defectPresentationService.getPresentationFor(defectId, presentationsHolder);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
