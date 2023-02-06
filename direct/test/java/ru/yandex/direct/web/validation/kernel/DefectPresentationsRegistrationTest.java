package ru.yandex.direct.web.validation.kernel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.validation.presentation.DefectPresentationRegistry;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.direct.web.configuration.DirectWebTest;

import static ru.yandex.direct.core.validation.TestDefectIdMappingHelper.assertAllDefectIdsAreMappedToPresentations;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DefectPresentationsRegistrationTest {

    @Autowired
    DefectPresentationRegistry<TranslatableWebDefect> defectPresentationRegistry;

    /**
     * Находит все енумы, реализующие {@link DefectId}, за исключением тех, которые называются TestDefectIds
     * Проверяет, что для всех значений этих енумов зарегистрированы представления в {@link DefectPresentationRegistry}
     */
    @Test
    public void presentationsAreRegisteredForAllDefectIds() throws ClassNotFoundException {
        assertAllDefectIdsAreMappedToPresentations(
                defectId -> defectPresentationRegistry.getRegisteredDefectIds().contains(defectId));
    }
}
