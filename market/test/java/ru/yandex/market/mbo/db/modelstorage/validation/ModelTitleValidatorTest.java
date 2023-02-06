package ru.yandex.market.mbo.db.modelstorage.validation;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.LocalizedString;
import ru.yandex.market.mbo.http.ModelStorage.Model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilder;

/**
 * Test of {@link ModelTitleValidator}.
 *
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.class)
public class ModelTitleValidatorTest extends BaseValidatorTestClass {

    private ModelTitleValidator validator;

    @Mock
    private ModelValidationContext context;

    CommonModel guru = getGuruBuilder()
        .id(1)
        .endModel();

    CommonModel guruAsSku = getGuruBuilder()
        .id(1)
        .parameterValues(5L, XslNames.IS_SKU, true)
        .endModel();

    CommonModel sku = getSkuBuilder(1)
        .id(2)
        .endModel();

    private Model dumpGuru = getGuruBuilder()
        .id(1)
        .title("Valid title")
        .getRawModel();

    private Model dumpSku = getSkuBuilder(1)
        .id(2)
        .title("Valid sku title")
        .getRawModel();

    private ModelStorage.Model dumpConstructedSku = getGuruBuilder()
        .id(1)
        .currentType(CommonModel.Source.SKU)
        .title("Valid sku title")
        .getRawModel();

    @Before
    public void setup() {
        when(context.getDumpModel(any(CommonModel.class), any(), anyBoolean())).thenAnswer(i -> {
            CommonModel input = i.getArgument(0);
            boolean isSkuMode = i.getArgument(2);
            if (input == guru) {
                return dumpGuru;
            }
            if (input == guruAsSku) {
                return isSkuMode ? dumpConstructedSku : dumpGuru;
            }
            if (input == sku) {
                return dumpSku;
            }
            return null;
        });
        validator = new ModelTitleValidator();
    }

    @Test
    public void testGuruModel() {
        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(guru), Arrays.asList(guru));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testSku() {
        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(sku), Arrays.asList(sku, guru));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testGuruTitleGenerationFails() {
        dumpGuru = getGuruBuilder()
            .id(1)
            .getRawModel();

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(guru), Arrays.asList(guru));

        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(guru.getId(), ModelValidationError.ErrorType.TITLE_GENERATION_FAILED,
                ModelValidationError.ErrorSubtype.GURU_TITLE_GENERATION_FAILED, true)
                .addLocalizedMessagePattern("Ошибка генерации тайтла Гуру карточки")
        );
    }

    @Test
    public void testSkuTitleGenerationFails() {
        dumpSku = getSkuBuilder(1)
            .id(2)
            .getRawModel();

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(sku), Arrays.asList(sku, guru));

        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(sku.getId(), ModelValidationError.ErrorType.TITLE_GENERATION_FAILED,
                ModelValidationError.ErrorSubtype.SKU_TITLE_GENERATION_FAILED, true)
                .addLocalizedMessagePattern("Ошибка генерации тайтла SKU")
        );
    }


    @Test
    public void testGuruAsSkuSkuGenerationFails() {
        dumpConstructedSku = getGuruBuilder()
            .id(1)
            .currentType(CommonModel.Source.SKU)
            .getRawModel();

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(guruAsSku), Arrays.asList(sku, guruAsSku));

        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(guruAsSku.getId(), ModelValidationError.ErrorType.TITLE_GENERATION_FAILED,
                ModelValidationError.ErrorSubtype.SKU_TITLE_GENERATION_FAILED, true)
                .addLocalizedMessagePattern("Ошибка генерации тайтла SKU")
        );
    }


    /**
     * Проверка, что при совокупности некоторых условий ошибка некритичная.
     * - Модель не должна опубликовываться в данный момент
     * - Это не должен быть перенос промеж категорий (т.е. категория до и после не меняется)
     * - Это должно быть обновление, но не создание модели
     * - У предыдущей версии модели тоже не было никаких тайтлов, в том числе сгенерённых "как в выгрузке".
     * Если все эти условия выполняются одновременно, то ошибка становится ворнингом.
     */
    @Test
    public void testWarningIfFailsUnderSetOfConditions() {
        CommonModel after = guru;
        CommonModel before = SerializationUtils.clone(after);
        dumpGuru = getGuruBuilder().id(1).getRawModel(); // dump after
        Model dumpBeforeGuru = dumpGuru; // dump before

        // Выгрузочная before-модель тоже будет без тайтлов. Остальным условиям входные данные и так удовлетворяют.
        when(context.getDumpBeforeModel(any(CommonModel.class))).thenReturn(dumpBeforeGuru);

        List<ModelValidationError> warnings = validator
            .validate(context, new ModelChanges(before, after), Collections.singletonList(guru));
        assertThat(warnings).containsExactlyInAnyOrder(
            new ModelValidationError(after.getId(), ModelValidationError.ErrorType.TITLE_GENERATION_FAILED,
                ModelValidationError.ErrorSubtype.GURU_TITLE_GENERATION_FAILED, false) // <-- non crit
                .addLocalizedMessagePattern("Ошибка генерации тайтла Гуру карточки")
        );

        // А теперь для верности добавим тайтл в прото-before модель и убедимся, что появился крит, ибо выходит,
        // что в новой модели эффективные тайтлы каким-то образом пропали == ошибка.
        dumpBeforeGuru = dumpBeforeGuru.toBuilder().addTitles(LocalizedString.newBuilder().build()).build();
        when(context.getDumpBeforeModel(any(CommonModel.class))).thenReturn(dumpBeforeGuru);

        List<ModelValidationError> criticals = validator
            .validate(context, new ModelChanges(before, after), Collections.singletonList(guru));
        assertThat(criticals).containsExactlyInAnyOrder(
            new ModelValidationError(after.getId(), ModelValidationError.ErrorType.TITLE_GENERATION_FAILED,
                ModelValidationError.ErrorSubtype.GURU_TITLE_GENERATION_FAILED, true) // <-- crit!
                .addLocalizedMessagePattern("Ошибка генерации тайтла Гуру карточки")
        );
    }
}
