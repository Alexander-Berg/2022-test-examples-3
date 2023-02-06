package ru.yandex.direct.core.entity.banner.service.validation.type.update;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import jdk.jfr.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.StubBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.href.BannerWithHrefUpdateValidationTypeSupport;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.validation.wrapper.ModelItemValidationBuilder;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.validation.constraint.CommonConstraints.unconditional;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;

@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class BannerUpdateValidationTypeSupportFacadeTest {

    @Spy
    @InjectMocks
    private BannerWithHrefUpdateValidationTypeSupport newBannerWithHrefUpdateValidationTypeSupport;

    private BannerUpdateValidationTypeSupportFacade validationTypeSupportFacade;

    @Mock
    private BannersUpdateOperationContainer container;

    @Before
    public void initTestData() {
        doAnswer(returnsSecondArg()).when(newBannerWithHrefUpdateValidationTypeSupport).validate(any(), any());
        List<BannerUpdateValidationTypeSupport<? extends Banner>> supports =
                List.of(newBannerWithHrefUpdateValidationTypeSupport);
        validationTypeSupportFacade = new BannerUpdateValidationTypeSupportFacade(supports);
    }

    @Test
    @Description("Проверяем, что элементы, которые упали на пре-валидации, не передаются в валидацию.")
    public void checkCallValidate_FiltersInvalidItemsFromValidationResult() {
        // Создадим 4 баннера:
        // - подходящего типа с ошибкой валидации
        // - подходящего типа без ошибки валидации
        // - неподходящего типа без ошибки валидации
        // - неподходящего типа с ошибкой валидации
        var banner1 = new TextBanner().withId(1L);
        var banner2 = new TextBanner().withId(2L);
        var banner3 = new StubBanner().withId(3L);
        var banner4 = new StubBanner().withId(4L);

        checkState(!BannerWithHref.class.isAssignableFrom(StubBanner.class));
        checkState(newBannerWithHrefUpdateValidationTypeSupport.getTypeClass().equals(BannerWithHref.class));

        ValidationResult<List<BannerWithSystemFields>, Defect> listVr =
                new ValidationResult<>(List.of(banner1, banner2, banner3, banner4));

        new ListValidationBuilder<>(listVr)
                .checkEachBy(banner -> {
                    if (banner == banner1 || banner == banner3) {
                        return ModelItemValidationBuilder.of(banner)
                                .check(unconditional(objectNotFound()))
                                .getResult();
                    } else {
                        return ModelItemValidationBuilder.of(banner)
                                .getResult();
                    }
                });

        var appliedChanges2 = new ModelChanges<>(banner2.getId(), TextBanner.class)
                .process("testHref2", TextBanner.HREF)
                .applyTo(banner2);
        var appliedChanges4 = new ModelChanges<>(banner4.getId(), StubBanner.class)
                .process(5L, StubBanner.ID)
                .applyTo(banner4);

        validationTypeSupportFacade.validate(container, listVr,
                Map.of(listVr.getValue().indexOf(banner2), appliedChanges2,
                        listVr.getValue().indexOf(banner4), appliedChanges4));

        ArgumentCaptor<ValidationResult> validationResultCaptor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(newBannerWithHrefUpdateValidationTypeSupport)
                .validate(eq(container), validationResultCaptor.capture(), any());
        assertThat(validationResultCaptor.getValue().getValue()).isEqualTo(List.of(banner2));
    }

}
