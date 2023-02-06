package ru.yandex.market.mbo.mdm.common.masterdata.validator.block;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.TestBlockCreationUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ValueCommentBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.ItemBlockValidationResult;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class ManufacturerCountriesBlockValidatorTest {


    private static final ManufacturerCountriesBlockValidator VALIDATOR =
        new ManufacturerCountriesBlockValidator(new StorageKeyValueServiceMock());

    private static ItemBlock createManufacturerCountriesBlock(List<String> countries) {
        return TestBlockCreationUtil.createStringsMdmParamValueBlock(
            KnownMdmParams.MANUFACTURER_COUNTRY,
            countries,
            Instant.EPOCH
        );
    }

    private static ErrorInfo createMissingCountryError() {
        return MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER_COUNTRY);
    }

    private static ErrorInfo createInvalidCountryError(String country) {
        return MbocErrors.get().mdManufacturerCountryValue(
            SskuMasterDataFields.MANUFACTURER_COUNTRY, country,
            "Страна: " + country + " не является валидной."
        );
    }

    @Test
    public void whenBlockContainsInvalidCountryReturnAppropriateError() {
        String country = "Гондор";
        ItemBlock block = createManufacturerCountriesBlock(List.of(country));
        var validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createInvalidCountryError(country);
        var expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        Assertions.assertThat(ManufacturerCountriesBlockValidator.validateManufacturerCountries(List.of(country), true))
            .containsOnly(expectedError);
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenBlockContainsValidCountriesReturnNoError() {
        List<String> countries = List.of("Россия", "Китай");
        ItemBlock block = createManufacturerCountriesBlock(countries);
        var validationData = new ItemBlockValidationData(List.of(block));

        var expectedResult = new ItemBlockValidationResult(block, List.of());

        Assertions.assertThat(ManufacturerCountriesBlockValidator.validateManufacturerCountries(countries, true))
            .isEmpty();
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenBlockContainsNoCountryReturnMissingCountryError() {
        List<String> countries = Collections.emptyList();
        ItemBlock block = createManufacturerCountriesBlock(countries);
        var validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createMissingCountryError();
        var expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        Assertions.assertThat(ManufacturerCountriesBlockValidator.validateManufacturerCountries(countries, true))
            .containsOnly(expectedError);
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenNoBLockReturnMissingCountryError() {
        var validationData = new ItemBlockValidationData(List.of());
        var expectedResult = new ItemBlockValidationResult(null, List.of(createMissingCountryError()));

        Assertions.assertThat(VALIDATOR.validateBlock(null, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenNotSupportedBlockReturnNoError() {
        ValueCommentBlock shelfLifeBlock =
            TestBlockCreationUtil.createShelfLifeBlock(TimeInUnits.UNLIMITED, "", Instant.EPOCH);
        var validationData = new ItemBlockValidationData(List.of(shelfLifeBlock));

        Assertions.assertThat(VALIDATOR.validateBlock(shelfLifeBlock, validationData))
            .isEqualTo(new ItemBlockValidationResult(shelfLifeBlock, List.of()));
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData))
            .isEqualTo(new ItemBlockValidationResult(null, List.of(createMissingCountryError())));
    }

    @Test
    public void whenEngNameOrSynonymReturnNoError() {
        // Synonym
        Assertions.assertThat(
            ManufacturerCountriesBlockValidator.validateManufacturerCountries(List.of("ⵍⵉⴱⵢⴰ"), true)
        ).isEmpty();
        // Eng name
        Assertions.assertThat(
            ManufacturerCountriesBlockValidator.validateManufacturerCountries(List.of("Libya"), true)
        ).isEmpty();
        // Ru name
        Assertions.assertThat(
            ManufacturerCountriesBlockValidator.validateManufacturerCountries(List.of("Ливия"), true)
        ).isEmpty();
        // Unknown
        Assertions.assertThat(
            ManufacturerCountriesBlockValidator.validateManufacturerCountries(List.of("Ливийская национальная армия"),
                true)
        ).containsExactly(createInvalidCountryError("Ливийская национальная армия"));
    }
}
