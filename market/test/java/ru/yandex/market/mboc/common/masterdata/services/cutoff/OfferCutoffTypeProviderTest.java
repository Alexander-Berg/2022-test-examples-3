package ru.yandex.market.mboc.common.masterdata.services.cutoff;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.model.QualityDocument.QualityDocumentType;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits.TimeUnit;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoffInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("checkstyle:MagicNumber")
public class OfferCutoffTypeProviderTest {

    @Test
    public void testSimpleErrorCutoffs() {
        OfferCutoffInfo noCountries = OfferCutoffTypeProvider.missManufacturerCountryError();
        assertEquals(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE, noCountries.getErrorCode());
        assertEquals(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE, noCountries.getTypeId());
        assertTrue(noCountries.getErrorData().getParams().isEmpty());

        OfferCutoffInfo noShelfLife = OfferCutoffTypeProvider.missShelfLifeError();
        assertEquals(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE, noShelfLife.getErrorCode());
        assertEquals(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE, noShelfLife.getTypeId());
        assertTrue(noShelfLife.getErrorData().getParams().isEmpty());

        OfferCutoffInfo noLifeTime = OfferCutoffTypeProvider.missLifeTimeError();
        assertEquals(OfferCutoffTypeProvider.MISS_LIFE_TIME_ERROR_CODE, noLifeTime.getErrorCode());
        assertEquals(OfferCutoffTypeProvider.MISS_LIFE_TIME_ERROR_CODE, noLifeTime.getTypeId());
        assertTrue(noLifeTime.getErrorData().getParams().isEmpty());

        OfferCutoffInfo noGuarantee = OfferCutoffTypeProvider.missGuaranteePeriodError();
        assertEquals(OfferCutoffTypeProvider.MISS_GUARANTEE_PERIOD_ERROR_CODE, noGuarantee.getErrorCode());
        assertEquals(OfferCutoffTypeProvider.MISS_GUARANTEE_PERIOD_ERROR_CODE, noGuarantee.getTypeId());
        assertTrue(noGuarantee.getErrorData().getParams().isEmpty());
    }

    @Test
    public void testTimePeriodedCutoffs() {
        OfferCutoffInfo wrongShelfLife = OfferCutoffTypeProvider.invalidShelfLifeError(
            new TimeInUnits(3, TimeUnit.HOUR),
            new TimeInUnits(21, TimeUnit.HOUR)
        );
        assertEquals(OfferCutoffTypeProvider.INVALID_SHELF_LIFE_ERROR_CODE, wrongShelfLife.getErrorCode());
        assertEquals(OfferCutoffTypeProvider.INVALID_SHELF_LIFE_ERROR_CODE, wrongShelfLife.getTypeId());
        Map<String, Object> expectedParams = ImmutableMap.of(
            "min_value", 3,
            "max_value", 21,
            "time_unit", "часа"
        );
        assertEquals(expectedParams, wrongShelfLife.getErrorData().getParams());


        OfferCutoffInfo wrongLifeTime = OfferCutoffTypeProvider.invalidLifeTimeError(
            new TimeInUnits(1, TimeUnit.DAY),
            new TimeInUnits(12, TimeUnit.DAY)
        );
        assertEquals(OfferCutoffTypeProvider.INVALID_LIFE_TIME_ERROR_CODE, wrongLifeTime.getErrorCode());
        assertEquals(OfferCutoffTypeProvider.INVALID_LIFE_TIME_ERROR_CODE, wrongLifeTime.getTypeId());
        expectedParams = ImmutableMap.of(
            "min_value", 1,
            "max_value", 12,
            "time_unit", "дней"
        );
        assertEquals(expectedParams, wrongLifeTime.getErrorData().getParams());

        OfferCutoffInfo wrongGuarantee = OfferCutoffTypeProvider.invalidGuaranteePeriodError(
            new TimeInUnits(1, TimeUnit.YEAR),
            new TimeInUnits(4, TimeUnit.YEAR)
        );
        assertEquals(OfferCutoffTypeProvider.INVALID_GUARANTEE_PERIOD_ERROR_CODE, wrongGuarantee.getErrorCode());
        assertEquals(OfferCutoffTypeProvider.INVALID_GUARANTEE_PERIOD_ERROR_CODE, wrongGuarantee.getTypeId());
        expectedParams = ImmutableMap.of(
            "min_value", 1,
            "max_value", 4,
            "time_unit", "лет"
        );
        assertEquals(expectedParams, wrongGuarantee.getErrorData().getParams());
    }

    @Test
    public void testCompositeTypeIdForDocuments() {
        OfferCutoffInfo info = OfferCutoffTypeProvider.missRequiredDocumentsError(Arrays.asList(
            QualityDocumentType.ENVIRONMENTAL_SAFETY_CERTIFICATE,
            QualityDocumentType.CERTIFICATE_OF_CONFORMITY,
            QualityDocumentType.DECLARATION_OF_CONFORMITY
        ));
        assertEquals(OfferCutoffTypeProvider.MISS_DOCUMENTS_ERROR_CODE, info.getErrorCode());

        String expectedTypeId = OfferCutoffTypeProvider.MISS_DOCUMENTS_ERROR_CODE +
            "-" + QualityDocumentType.DECLARATION_OF_CONFORMITY.name().toLowerCase() +
            "-" + QualityDocumentType.CERTIFICATE_OF_CONFORMITY.name().toLowerCase() +
            "-" + QualityDocumentType.ENVIRONMENTAL_SAFETY_CERTIFICATE.name().toLowerCase();
        assertEquals(expectedTypeId, info.getTypeId());

        List<String> expectedReadableNames = Arrays.asList(
            "декларацию соответствия",
            "сертификат соответствия",
            "сертификат соответствия экологической безопасности");
        assertEquals(expectedReadableNames, info.getErrorData().getParams().get("doc_types"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDifferentTimeUnitsThrowException() {
        OfferCutoffTypeProvider.invalidShelfLifeError(
            new TimeInUnits(4, TimeUnit.HOUR),
            new TimeInUnits(8, TimeUnit.YEAR)
        );
        fail("Exception should've been thrown.");
    }
}
