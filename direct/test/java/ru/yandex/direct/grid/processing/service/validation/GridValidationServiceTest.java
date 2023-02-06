package ru.yandex.direct.grid.processing.service.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.grid.core.entity.deal.model.GdiDealFilter;
import ru.yandex.direct.grid.model.GdEntityStatsFilter;
import ru.yandex.direct.grid.model.GdStatPreset;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilter;
import ru.yandex.direct.grid.processing.model.client.GdClientSearchRequest;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class GridValidationServiceTest {

    @Mock
    private GridValidationResultConversionService validationResultConversionService;

    @InjectMocks
    private GridValidationService service;

    @Parameterized.Parameter
    public ValidationPair<?> validationPair;
    @Parameterized.Parameter(1)
    public boolean expectException;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        LocalDate fromDate = LocalDate.now().minusMonths(1);
        LocalDate toDate = fromDate.plusDays(10);
        return Arrays.asList(new Object[][]{
                // ids collection
                {
                        pair(null, GridValidationService::validateIdsCollection),
                        false,
                },
                {
                        pair(Collections.singleton(1L), GridValidationService::validateIdsCollection),
                        false,
                },
                {
                        pair(Collections.singleton(null), GridValidationService::validateIdsCollection),
                        true,
                },
                {
                        pair(Collections.singleton(-1L), GridValidationService::validateIdsCollection),
                        true,
                },
                // limit-offset
                {
                        pair(null, GridValidationService::validateLimitOffset),
                        false,
                },
                {
                        pair(new GdLimitOffset(), GridValidationService::validateLimitOffset),
                        false,
                },
                {
                        pair(new GdLimitOffset().withLimit(10), GridValidationService::validateLimitOffset),
                        false,
                },
                {
                        pair(new GdLimitOffset().withLimit(10).withOffset(10),
                                GridValidationService::validateLimitOffset),
                        false,
                },
                {
                        pair(new GdLimitOffset().withLimit(-10).withOffset(10),
                                GridValidationService::validateLimitOffset),
                        true,
                },
                {
                        pair(new GdLimitOffset().withOffset(-10),
                                GridValidationService::validateLimitOffset),
                        true,
                },
                // Stat reqs
                {
                        pair(null, GridValidationService::validateStatRequirements),
                        false,
                },
                {
                        pair(new GdStatRequirements()
                                        .withPreset(GdStatPreset.PREVIOUS_MONTH),
                                GridValidationService::validateStatRequirements),
                        false,
                },
                {
                        pair(new GdStatRequirements()
                                        .withFrom(fromDate)
                                        .withTo(toDate),
                                GridValidationService::validateStatRequirements),
                        false,
                },
                {
                        pair(new GdStatRequirements()
                                        .withFrom(fromDate)
                                        .withTo(fromDate),
                                GridValidationService::validateStatRequirements),
                        false,
                },
                {
                        pair(new GdStatRequirements()
                                        .withTo(fromDate),
                                GridValidationService::validateStatRequirements),
                        true,
                },
                {
                        pair(new GdStatRequirements()
                                        .withPreset(GdStatPreset.PREVIOUS_MONTH)
                                        .withFrom(fromDate)
                                        .withTo(toDate),
                                GridValidationService::validateStatRequirements),
                        true,
                },
                {
                        pair(new GdStatRequirements()
                                        .withFrom(toDate)
                                        .withTo(fromDate),
                                GridValidationService::validateStatRequirements),
                        true,
                },
                {
                        pair(new GdStatRequirements(),
                                GridValidationService::validateStatRequirements),
                        true,
                },
                // validateClientSearchRequest
                {
                        pair(new GdClientSearchRequest()
                                        .withId(1L),
                                GridValidationService::validateClientSearchRequest),
                        false,
                },
                {
                        pair(new GdClientSearchRequest()
                                        .withUserId(1L),
                                GridValidationService::validateClientSearchRequest),
                        false,
                },
                {
                        pair(new GdClientSearchRequest()
                                        .withLogin("test-login"),
                                GridValidationService::validateClientSearchRequest),
                        false,
                },
                {
                        pair(new GdClientSearchRequest()
                                        .withLogin("test%%$-login"),
                                GridValidationService::validateClientSearchRequest),
                        true,
                },
                {
                        pair(null,
                                GridValidationService::validateClientSearchRequest),
                        true,
                },
                {
                        pair(new GdClientSearchRequest(),
                                GridValidationService::validateClientSearchRequest),
                        true,
                },
                {
                        pair(new GdClientSearchRequest()
                                        .withId(1L)
                                        .withLogin("test-login"),
                                GridValidationService::validateClientSearchRequest),
                        true,
                },
                // validateAdGroupFilter
                {
                        pair(null, GridValidationService::validateAdGroupFilter),
                        false,
                },
                {
                        pair(new GdAdGroupFilter()
                                        .withAdGroupIdNotIn(Collections.singleton(1L))
                                        .withStats(new GdEntityStatsFilter()
                                                .withMaxBounceRate(BigDecimal.TEN)),
                                GridValidationService::validateAdGroupFilter),
                        false,
                },
                {
                        pair(new GdAdGroupFilter()
                                        .withAdGroupIdNotIn(Collections.singleton(-1L))
                                        .withStats(new GdEntityStatsFilter()
                                                .withMaxBounceRate(BigDecimal.TEN)),
                                GridValidationService::validateAdGroupFilter),
                        true,
                },
                {
                        pair(new GdAdGroupFilter()
                                        .withAdGroupIdNotIn(Collections.singleton(1L))
                                        .withStats(new GdEntityStatsFilter()
                                                .withMaxBounceRate(BigDecimal.ZERO.subtract(BigDecimal.ONE))),
                                GridValidationService::validateAdGroupFilter),
                        true,
                },
                // validateCampaignFilter
                {
                        pair(null, GridValidationService::validateCampaignFilter),
                        false,
                },
                {
                        pair(new GdCampaignFilter()
                                        .withCampaignIdIn(Collections.singleton(1L))
                                        .withStats(new GdEntityStatsFilter()
                                                .withMaxBounceRate(BigDecimal.TEN)),
                                GridValidationService::validateCampaignFilter),
                        false,
                },
                {
                        pair(new GdCampaignFilter()
                                        .withCampaignIdIn(Collections.singleton(-1L))
                                        .withStats(new GdEntityStatsFilter()
                                                .withMaxBounceRate(BigDecimal.TEN)),
                                GridValidationService::validateCampaignFilter),
                        true,
                },
                {
                        pair(new GdCampaignFilter()
                                        .withCampaignIdIn(Collections.singleton(1L))
                                        .withStats(new GdEntityStatsFilter()
                                                .withMaxBounceRate(BigDecimal.ZERO.subtract(BigDecimal.ONE))),
                                GridValidationService::validateCampaignFilter),
                        true,
                },
                // validateAdFilter
                {
                        pair(null, GridValidationService::validateAdFilter),
                        false,
                },
                {
                        pair(new GdAdFilter()
                                        .withAdIdNotIn(Collections.singleton(1L))
                                        .withStats(new GdEntityStatsFilter()
                                                .withMaxBounceRate(BigDecimal.TEN)),
                                GridValidationService::validateAdFilter),
                        false,
                },
                {
                        pair(new GdAdFilter()
                                        .withAdIdNotIn(Collections.singleton(1L))
                                        .withStats(new GdEntityStatsFilter()
                                                .withMaxBounceRate(BigDecimal.ZERO.subtract(BigDecimal.ONE))),
                                GridValidationService::validateAdFilter),
                        true,
                },
                // validateDealFilter
                {
                        pair(null, GridValidationService::validateDealFilter),
                        false,
                },
                {
                        pair(new GdiDealFilter()
                                        .withDealIdIncluded(Collections.singleton(1L))
                                        .withDealIdExcluded(Collections.singleton(1L)),
                                GridValidationService::validateDealFilter),
                        false,
                },
                {
                        pair(new GdiDealFilter()
                                        .withDealIdIncluded(Collections.singleton(-1L))
                                        .withDealIdExcluded(Collections.singleton(1L)),
                                GridValidationService::validateDealFilter),
                        true,
                },
                {
                        pair(new GdiDealFilter()
                                        .withDealIdIncluded(Collections.singleton(1L))
                                        .withDealIdExcluded(Collections.singleton(0L)),
                                GridValidationService::validateDealFilter),
                        true,
                },
        });
    }

    private static <T> ValidationPair<T> pair(@Nullable T value, BiConsumer<GridValidationService, T> validator) {
        return new ValidationPair<>(value, validator);
    }

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);
        doReturn(new GdValidationResult())
                .when(validationResultConversionService).buildGridValidationResult(any(), any());
    }

    @Test
    public void checkValidation() {
        if (!expectException) {
            validationPair.check(service);
        } else {
            assertThatThrownBy(() -> validationPair.check(service))
                    .isInstanceOf(GridValidationException.class);
        }
    }

    private static class ValidationPair<T> {
        private final T value;
        private final BiConsumer<GridValidationService, T> validator;

        private ValidationPair(T value, BiConsumer<GridValidationService, T> validator) {
            this.value = value;
            this.validator = validator;
        }

        private void check(GridValidationService service) {
            validator.accept(service, value);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
