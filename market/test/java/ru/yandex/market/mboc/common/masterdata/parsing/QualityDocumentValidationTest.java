package ru.yandex.market.mboc.common.masterdata.parsing;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class QualityDocumentValidationTest {

    private static final long SEED = 17267;

    private final QualityDocumentValidation validation = new QualityDocumentValidation();

    private EnhancedRandom random;

    @Before
    public void insertOffersAndSuppliers() {
        random = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenIncorrectStartDateShouldOutputError() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);
        document.setStartDate(LocalDate.now().plusYears(1));
        document.setEndDate(LocalDate.now().plusMonths(1));

        List<ErrorInfo> validationResult = validation.processAndValidate(document);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(validationResult).hasSize(1);
            s.assertThat(validationResult)
                .extracting(ErrorInfo::toString)
                .containsExactly(MbocErrors.get().qdStartDateAfterEndDate().toString());
        });
    }

    @Test
    public void whenIncorrectEndDateShouldOutputError() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);
        document.setStartDate(LocalDate.now().minusYears(1));
        document.setEndDate(LocalDate.now().minusMonths(1));

        List<ErrorInfo> validationResult = validation.processAndValidate(document);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(validationResult).hasSize(1);
            s.assertThat(validationResult)
                .extracting(ErrorInfo::toString)
                .containsExactly(MbocErrors.get().qdEndDateInPast().toString());
        });
    }

    @Test
    public void whenIncorrectRegNumShouldOutputError() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);
        document.setRegistrationNumber("1234567");

        List<ErrorInfo> validationResult = validation.processAndValidate(document);
        SoftAssertions.assertSoftly(s -> { // too short
            s.assertThat(validationResult).hasSize(1);
            s.assertThat(validationResult)
                .extracting(ErrorInfo::toString)
                .containsExactly(MbocErrors.get().qdIncorrectRegistrationNumberFormat().toString());
        });

        document.setRegistrationNumber(
            "Тхри монхс оф винтер кулнесс энд овесом холидейс " +
                "Ви'ф кепт аур хувсиес ворм эт хоум тайм офф фром ворк ту плэй " +
                "Бат зе фуд ви'ф сторед из раннинг аут энд ви кэнт гроу ин зис колд " +
                "Энд ивен тоугх ай лав май бутс зис фейшен'з геттинг олд");
        SoftAssertions.assertSoftly(s -> { // way too long
            s.assertThat(validationResult).hasSize(1);
            s.assertThat(validationResult)
                .extracting(ErrorInfo::toString)
                .containsExactly(MbocErrors.get().qdIncorrectRegistrationNumberFormat().toString());
        });

        document.setRegistrationNumber(null);
        SoftAssertions.assertSoftly(s -> { // null
            s.assertThat(validationResult).hasSize(1);
            s.assertThat(validationResult)
                .extracting(ErrorInfo::toString)
                .containsExactly(MbocErrors.get().qdIncorrectRegistrationNumberFormat().toString());
        });
    }

    @Test
    public void whenIncorrectExemptionLetterRegNumShouldOutputError() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);
        document.setType(QualityDocument.QualityDocumentType.EXEMPTION_LETTER);
        document.setCertificationOrgRegNumber("1234567890");
        List<String> wrongNums = Arrays.asList(
            //  too short
            "1234567890_123",
            //  cert org different from document.certOrgRegNumber
            "0987654321_1234"
        );

        List<List<ErrorInfo>> results = wrongNums.stream().map(num -> {
            document.setRegistrationNumber(num);
            return validation.processAndValidate(document);
        }).collect(Collectors.toList());

        SoftAssertions.assertSoftly(s -> {
            for (List<ErrorInfo> validationResult : results) {
                s.assertThat(validationResult).hasSize(1);
                s.assertThat(validationResult)
                    .containsExactly(MbocErrors.get()
                        .qdIncorrectRegistrationNumberForExemptionLettersFormat());
            }
        });
    }

    @Test
    public void whenValidateCorrectDocumentShouldNotOutputErrors() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);

        List<ErrorInfo> validationResult = validation.processAndValidate(document);
        Assertions.assertThat(validationResult).isEmpty();
    }

    @Test
    public void whenValidateCorrectExemptionLetterShouldNotOutputErrors() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);

        document.setType(QualityDocument.QualityDocumentType.EXEMPTION_LETTER);
        document.setRegistrationNumber("123456789123456789_639");
        document.setCertificationOrgRegNumber("123456789123456789");

        List<ErrorInfo> validationResult = validation.processAndValidate(document);
        Assertions.assertThat(validationResult).isEmpty();
    }

    @Test
    public void whenMultipleErrorsShouldOutputAllErrors() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);
        document.setStartDate(LocalDate.now().plusDays(1));
        document.setEndDate(LocalDate.now().minusMonths(1));
        document.setRegistrationNumber("12345");

        List<ErrorInfo> validationResult = validation.processAndValidate(document);

        SoftAssertions.assertSoftly(s -> {
            MbocErrors errors = MbocErrors.get();
            s.assertThat(validationResult).extracting(ErrorInfo::toString).containsExactlyInAnyOrder(
                errors.qdIncorrectRegistrationNumberFormat().toString(),
                errors.qdStartDateAfterEndDate().toString(),
                errors.qdEndDateInPast().toString()
            );
        });
    }

    @Test
    public void whenAddingDocumentWithNullStartDateShouldOutputError() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);
        document.setStartDate(null);

        List<ErrorInfo> validationResult = validation.processAndValidateForAddition(document);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(validationResult).hasSize(1);
            s.assertThat(validationResult)
                .extracting(ErrorInfo::toString)
                .containsExactly(MbocErrors.get().qdAddSupplierDocumentsInvalidDocument("Поле start_date обязательно.")
                    .toString());
        });
    }

    @Test
    public void whenAddingDocumentWithNullTypeShouldOutputError() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);
        document.setType(null);

        List<ErrorInfo> validationResult = validation.processAndValidateForAddition(document);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(validationResult).hasSize(1);
            s.assertThat(validationResult)
                .extracting(ErrorInfo::toString)
                .containsExactly(MbocErrors.get().qdAddSupplierDocumentsInvalidDocument("Поле type обязательно.")
                    .toString());
        });
    }

    @Test
    public void whenAddingDocumentShouldAlsoValidateWithCommonValidator() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random)
            .setStartDate(LocalDate.now().plusDays(1))
            .setEndDate(LocalDate.now().minusMonths(1))
            .setRegistrationNumber("12345");

        List<ErrorInfo> validationForAdditionResult = validation.processAndValidateForAddition(document);
        List<ErrorInfo> validationResult = validation.processAndValidate(document);

        SoftAssertions.assertSoftly(s -> s.assertThat(validationResult)
            .containsExactly(validationForAdditionResult.toArray(new ErrorInfo[0])));
    }

    @Test
    public void whenRegistrationNumberIsInvalidShouldReturnFalseInCertificationNumberCheck() {
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(
                validation.regNumberContainsCertificationOrganisation(null)).isFalse();
            softAssertions.assertThat(
                validation.regNumberContainsCertificationOrganisation("nounderscore")).isFalse();
            softAssertions.assertThat(
                validation.regNumberContainsCertificationOrganisation("_underscore-too-close")).isFalse();
            softAssertions.assertThat(
                validation.regNumberContainsCertificationOrganisation("underscore-too-far_")).isFalse();
        });
    }

    @Test
    public void whenRegistrationNumberIsValidShouldReturnTrueInCertificationNumberCheck() {
        Assertions.assertThat(validation
            .regNumberContainsCertificationOrganisation("underscore-is-ok_regnum")).isTrue();
    }

    @Test
    public void whenIncorrectExemptionLetterCertificationOrganisationShouldOutputError() {
        //  turn this test off for now
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);
        document.setType(QualityDocument.QualityDocumentType.EXEMPTION_LETTER);

        // null certification Number
        document.setCertificationOrgRegNumber(null);
        document.setRegistrationNumber("1234567890_1234");
        Assertions.assertThat(validation.processAndValidate(document))
            .isEmpty();
//        Assertions.assertThat(validation.processAndValidate(document))
//            .containsExactly(
//                MbocErrors.get().qdNullCertificationOrganisationNumberForExemptionLetter(),
//                MbocErrors.get().qdIncorrectRegistrationNumberForExemptionLettersFormat());
        // short certification Number
        document.setCertificationOrgRegNumber("123456789");
        document.setRegistrationNumber("1234567890_1234");
        Assertions.assertThat(validation.processAndValidate(document))
            .containsExactly(MbocErrors.get().qdIncorrectRegistrationNumberForExemptionLettersFormat());
//        Assertions.assertThat(validation.processAndValidate(document))
//            .containsExactly(
//                MbocErrors.get().qdShortCertificationOrganisationNumberForExemptionLetter(
//                    QualityDocumentValidation.REGISTRATION_NUMBER_FOR_EXEMPTION_LETTER_PREFIX_MIN_LENGTH),
//                MbocErrors.get().qdIncorrectRegistrationNumberForExemptionLettersFormat());
    }

    @Test
    public void whenProcessingShouldUpdateRegNumberWithCertOrg() {
        QualityDocument document = getExemptionLetterQualityDocument("1234567890", "some-text");
        validation.processAndValidate(document);
        Assertions.assertThat(document.getRegistrationNumber()).isEqualTo("1234567890_some-text");
    }

    @Test
    public void whenProcessingShouldNotUpdateNullRegNumber() {
        QualityDocument document = getExemptionLetterQualityDocument("1234567890", null);
        validation.processAndValidate(document);
        Assertions.assertThat(document.getRegistrationNumber()).isNull();
    }

    @Test
    public void whenProcessingShouldNotUpdateRegNumberWithNullCertOrg() {
        QualityDocument document = getExemptionLetterQualityDocument(null, "some-text");
        validation.processAndValidate(document);
        Assertions.assertThat(document.getRegistrationNumber()).isEqualTo("some-text");
    }

    @Test
    public void whenProcessingShouldNotUpdateNotExemptionLetterQD() {
        QualityDocument document = getExemptionLetterQualityDocument("1234567890", "some-text")
            .setType(QualityDocument.QualityDocumentType.CERTIFICATE_OF_CONFORMITY);
        validation.processAndValidate(document);
        Assertions.assertThat(document.getRegistrationNumber()).isEqualTo("some-text");
    }

    @Test
    public void whenProcessingShouldNotUpdateIfRegNumContainsCertOrg() {
        QualityDocument document = getExemptionLetterQualityDocument("1234567890",
            "certification-org_some-text");
        validation.processAndValidate(document);
        Assertions.assertThat(document.getRegistrationNumber()).isEqualTo("certification-org_some-text");
    }

    @Test
    public void whenProcessingExemptionLetterShouldReturnCorrectRegNumber() {
        QualityDocument document = getExemptionLetterQualityDocument("1234567890", "ololo");

        List<ErrorInfo> errorInfos = validation.processAndValidate(document);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(errorInfos).isEmpty();
            softAssertions.assertThat(document.getRegistrationNumber())
                .isEqualTo("1234567890_ololo");
        });
    }

    @Test
    public void whenProcessingExemptionLetterShouldValidateItWithoutCertOrg() {
        QualityDocument document = getExemptionLetterQualityDocument(null, "1234567890_ololo");

        List<ErrorInfo> errorInfos = validation.processAndValidate(document);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(errorInfos).isEmpty();
            softAssertions.assertThat(document.getRegistrationNumber())
                .isEqualTo("1234567890_ololo");
        });
    }

    private QualityDocument getExemptionLetterQualityDocument(String certOrgNum, String regNum) {
        return TestDataUtils.generateCorrectDocument(random)
            .setType(QualityDocument.QualityDocumentType.EXEMPTION_LETTER)
            .setCertificationOrgRegNumber(certOrgNum)
            .setRegistrationNumber(regNum);
    }
}
