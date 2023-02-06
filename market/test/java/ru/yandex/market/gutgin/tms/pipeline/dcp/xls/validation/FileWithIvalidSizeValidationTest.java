package ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;

import static org.assertj.core.api.Assertions.assertThat;

public class FileWithIvalidSizeValidationTest {

    public static final long UNITED_SIZE_ID = 25L;
    FileWithInvalidSizeValidation validation;

    @Before
    public void setUp() {
        validation = new FileWithInvalidSizeValidation(null);
    }

    @Test
    public void CheckIfUnitedSizeDoesNotExistsThanFileIsValid() {
        MboParameters.Parameter unitedSize = null;
        assertThat(validation.isInvalidFile(unitedSize, null, null)).isFalse();
    }

    @Test
    public void CheckIfUnitedSizeIsNotMandatoryForPartnerThanFileIsValid() {
        MboParameters.Parameter unitedSize =
            MboParameters.Parameter.newBuilder().setId(UNITED_SIZE_ID).setMandatoryForPartner(false).build();
        assertThat(validation.isInvalidFile(unitedSize, null, null)).isFalse();
    }

    @Test
    public void CheckIfUnitedSizeExistsAndNoSizeParamsInParametersThanFileIsValid() {
        MboParameters.Parameter unitedSize =
            MboParameters.Parameter.newBuilder().setId(UNITED_SIZE_ID).setMandatoryForPartner(true).build();

        Set<Long> sizeParamIds = new HashSet<>();
        sizeParamIds.add(1L);
        sizeParamIds.add(2L);
        sizeParamIds.add(3L);

        Set<Long> parameters = new HashSet<>();
        parameters.add(10L);
        parameters.add(11L);
        parameters.add(12L);

        assertThat(validation.isInvalidFile(unitedSize, sizeParamIds, parameters)).isFalse();
    }

    @Test
    public void CheckIfUnitedSizeExistsAndSizeParamInParametersThanFileIsInvalid() {
        MboParameters.Parameter unitedSize =
            MboParameters.Parameter.newBuilder().setId(UNITED_SIZE_ID).setMandatoryForPartner(true).build();

        Set<Long> sizeParamIds = new HashSet<>();
        sizeParamIds.add(10L);
        sizeParamIds.add(2L);
        sizeParamIds.add(3L);

        Set<Long> parameters = new HashSet<>();
        parameters.add(10L);
        parameters.add(11L);
        parameters.add(12L);

        assertThat(validation.isInvalidFile(unitedSize, sizeParamIds, parameters)).isTrue();
    }
}
