package ru.yandex.market.mboc.common.masterdata.parsing.validator;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields.MANUFACTURER_COUNTRY;

public class ManufacturerCountryValidatorTest {

    private ManufacturerCountryValidator validator = new ManufacturerCountryValidator();

    @Test
    public void whenNonExistingCountryOutputValidationError() {
        Optional<ErrorInfo> errorInfo = validator.validate(
            MANUFACTURER_COUNTRY, "non_existing_test_country"
        );

        assertSoftly(softly -> {
                softly.assertThat(errorInfo).hasValueSatisfying(error -> {
                        softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
                        softly.assertThat(error.getErrorCode()).isEqualTo(
                            MbocErrors.get()
                                .mdManufacturerCountryValue("", "", "")
                                .getErrorCode());
                    }
                );
            }
        );
    }

    @Test
    public void whenExistingCountryOutputNoValigationError() {
        Optional<ErrorInfo> errorInfo = validator.validate(
            MANUFACTURER_COUNTRY, "Бурунди"
        );
        Assertions.assertThat(errorInfo).isEmpty();
    }
}
