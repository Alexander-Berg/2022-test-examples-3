package ru.yandex.direct.grid.processing.service.group;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.validation.defect.CommonDefects;

import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class AdGroupValidationTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private GridValidationService gridValidationService;

    @Test
    public void validateGdAdGroupsContainer_InvalidLibraryMwIdFilter() {
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(GdAdGroupsContainer.FILTER.name() + "."
                                + GdAdGroupFilter.LIBRARY_MW_ID_IN.name(),
                        CommonDefects.validId()))));
        gridValidationService.validateGdAdGroupsContainer(getValidAdGroupContainer()
                .withFilter(new GdAdGroupFilter()
                        .withLibraryMwIdIn(Collections.singleton(-1L))));
    }

    private GdAdGroupsContainer getValidAdGroupContainer() {
        return new GdAdGroupsContainer()
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(LocalDate.now().minusDays(2))
                        .withTo(LocalDate.now()))
                .withFilter(new GdAdGroupFilter()
                        .withLibraryMwIdIn(Collections.singleton(-1L)))
                .withLimitOffset(new GdLimitOffset()
                        .withLimit(2)
                        .withOffset(1));
    }
}
