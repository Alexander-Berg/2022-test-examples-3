package ru.yandex.market.promoboss.validator.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.promoboss.config.LoyaltyClientConfig;
import ru.yandex.market.promoboss.config.PostgresDbConfig;
import ru.yandex.market.promoboss.dao.PromoDao;
import ru.yandex.market.promoboss.dao.mechanics.PromocodeDao;
import ru.yandex.market.promoboss.service.TimeService;
import ru.yandex.market.promoboss.service.mechanics.PromocodeReservationService;
import ru.yandex.market.promoboss.service.mechanics.PromocodeService;
import ru.yandex.market.promoboss.validator.exception.ConstraintListsValidationException;
import ru.yandex.market.promoboss.validator.exception.FieldsValidationException;
import ru.yandex.market.promoboss.validator.exception.PromoIdValidationException;
import ru.yandex.market.promoboss.validator.exception.SourceTypeValidationException;
import ru.yandex.market.promoboss.validator.service.mechanics.UpdatePromocodeValidator;
import ru.yandex.mj.generated.server.model.CheapestAsGift;
import ru.yandex.mj.generated.server.model.MechanicsType;
import ru.yandex.mj.generated.server.model.PromoMainRequestParams;
import ru.yandex.mj.generated.server.model.PromoMechanicsParams;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.Promocode;
import ru.yandex.mj.generated.server.model.SourceType;
import ru.yandex.mj.generated.server.model.PromoStatus;
import ru.yandex.mj.generated.server.model.SupplierPromoConstraintsDto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ContextConfiguration(classes = {
        UpdatePromoRequestValidator.class,
        PromoIdValidator.class,
        ModifiedByValidator.class,
        SourceTypeValidator.class,
        StatusUpdateRequestValidator.class,
        SrcCifaceFieldsValidator.class,
        ConstraintListsValidator.class,
        UpdatePromocodeValidator.class,
        PromocodeService.class,
        PromocodeReservationService.class,
        PromocodeDao.class,
        PostgresDbConfig.class,
        PromoDao.class,
        LoyaltyClientConfig.class,
        TimeService.class
})
public class UpdatePromoRequestValidatorV2Test {

    @Autowired
    private UpdatePromoRequestValidator validator;

    @Test
    public void shouldThrowExceptionIfPromoIdInvalid() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(SourceType.CATEGORYIFACE)
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .status(PromoStatus.NEW)
                                .startAt(OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond())
                                .endAt(OffsetDateTime.now().toEpochSecond())
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(new CheapestAsGift())
                );

        // act and verify
        assertThrows(PromoIdValidationException.class, () -> validator.validate(request));
    }

    @Test
    public void shouldThrowExceptionIfModifiedByInvalid() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("cf_123")
                .modifiedBy("")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(SourceType.CATEGORYIFACE)
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .status(PromoStatus.NEW)
                                .startAt(OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond())
                                .endAt(OffsetDateTime.now().toEpochSecond())
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(new CheapestAsGift())
                );

        // act and verify
        assertThrows(FieldsValidationException.class, () -> validator.validate(request));
    }

    @Test
    public void shouldThrowExceptionIfSourceTypeInvalid() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("cf_123")
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .status(PromoStatus.NEW)
                                .startAt(OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond())
                                .endAt(OffsetDateTime.now().toEpochSecond())
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(new CheapestAsGift())
                );

        // act and verify
        assertThrows(SourceTypeValidationException.class, () -> validator.validate(request));
    }

    @Test
    public void shouldThrowExceptionIfConstraintExcludeIsNull() {
        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("cf_123")
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(SourceType.CATEGORYIFACE)
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .status(PromoStatus.NEW)
                                .startAt(OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond())
                                .endAt(OffsetDateTime.now().toEpochSecond())
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(new CheapestAsGift())
                )
                .suppliersConstraints(new SupplierPromoConstraintsDto()
                        .suppliers(List.of(123L)));

        // act and verify
        assertThrows(ConstraintListsValidationException.class, () -> validator.validate(request));
    }

    @Test
    public void shouldNotThrowExceptionIfValidRequest() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("cf_123")
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(SourceType.CATEGORYIFACE)
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .status(PromoStatus.NEW)
                                .startAt(OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond())
                                .endAt(OffsetDateTime.now().toEpochSecond())
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(new CheapestAsGift())
                );

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @Sql(scripts = "classpath:/test-sql/updatePromoRequestValidatorTest_promocode_fillIn.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:/test-sql/clearPromosTable.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void shouldThrowExceptionIfCodeChanged() {
        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("cf_0000000073")
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(SourceType.CATEGORYIFACE)
                                .mechanicsType(MechanicsType.PROMO_CODE)
                                .status(PromoStatus.NEW)
                                .startAt(OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond())
                                .endAt(OffsetDateTime.now().toEpochSecond())
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .promocode(new Promocode()
                                        .code("NEWCODE")
                                        .codeType(Promocode.CodeTypeEnum.PERCENTAGE)
                                        .value(5))
                );
        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class, () -> validator.validate(request));
        assertEquals("Promocode code can't be changed", e.getMessage());
    }

    @Test
    @Sql(scripts = "classpath:/test-sql/updatePromoRequestValidatorTest_promocode_fillIn.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:/test-sql/clearPromosTable.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void shouldNotThrowIfPromocodeValid() {
        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("cf_0000000073")
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(SourceType.CATEGORYIFACE)
                                .mechanicsType(MechanicsType.PROMO_CODE)
                                .status(PromoStatus.NEW)
                                .startAt(OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond())
                                .endAt(OffsetDateTime.now().toEpochSecond())
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .promocode(new Promocode()
                                        .code("CODE")
                                        .codeType(Promocode.CodeTypeEnum.PERCENTAGE)
                                        .value(5))
                );
        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }
}

