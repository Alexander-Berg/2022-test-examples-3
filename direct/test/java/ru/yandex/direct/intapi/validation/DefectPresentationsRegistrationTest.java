package ru.yandex.direct.intapi.validation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.validation.kernel.TranslatableIntapiDefect;
import ru.yandex.direct.validation.presentation.DefectPresentationRegistry;
import ru.yandex.direct.validation.result.DefectId;

import static ru.yandex.direct.core.validation.TestDefectIdMappingHelper.assertAllDefectIdsAreMappedToPresentations;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DefectPresentationsRegistrationTest {

    @Autowired
    DefectPresentationRegistry<TranslatableIntapiDefect> defectPresentationRegistry;

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
