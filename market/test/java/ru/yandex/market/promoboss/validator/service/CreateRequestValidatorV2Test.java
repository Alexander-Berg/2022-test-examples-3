package ru.yandex.market.promoboss.validator.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.promoboss.service.mechanics.PromocodeReservationService;
import ru.yandex.market.promoboss.validator.exception.ConstraintListsValidationException;
import ru.yandex.market.promoboss.validator.exception.DateTimeValidationException;
import ru.yandex.market.promoboss.validator.exception.FieldsValidationException;
import ru.yandex.market.promoboss.validator.exception.MechanicsTypeValidationException;
import ru.yandex.market.promoboss.validator.exception.PromoIdValidationException;
import ru.yandex.market.promoboss.validator.exception.SourceTypeValidationException;
import ru.yandex.market.promoboss.validator.service.mechanics.CreatePromocodeValidator;
import ru.yandex.mj.generated.server.model.CheapestAsGift;
import ru.yandex.mj.generated.server.model.MechanicsType;
import ru.yandex.mj.generated.server.model.PromoMainRequestParams;
import ru.yandex.mj.generated.server.model.PromoMechanicsParams;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.PromoSrcParams;
import ru.yandex.mj.generated.server.model.Promocode;
import ru.yandex.mj.generated.server.model.SourceType;
import ru.yandex.mj.generated.server.model.SrcCifaceDtoV2;
import ru.yandex.mj.generated.server.model.PromoStatus;
import ru.yandex.mj.generated.server.model.SupplierPromoConstraintsDto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ContextConfiguration(classes = {
        CreateRequestValidator.class,
        PromoIdValidator.class,
        ModifiedByValidator.class,
        SourceTypeValidator.class,
        MechanicsTypeValidator.class,
        DateTimeValidator.class,
        StatusCreateRequestValidator.class,
        SrcCifaceFieldsValidator.class,
        ConstraintListsValidator.class,
        CreatePromocodeValidator.class
})
public class CreateRequestValidatorV2Test {

    @Autowired
    private CreateRequestValidator validator;

    @MockBean
    private PromocodeReservationService promocodeReservationService;

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
                )
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
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
                )
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
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
                )
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        assertThrows(SourceTypeValidationException.class, () -> validator.validate(request));
    }

    @Test
    public void shouldThrowExceptionIfMechanicsTypeIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("cf_123")
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(SourceType.CATEGORYIFACE)
                                .mechanicsType(null)
                                .status(PromoStatus.NEW)
                                .startAt(OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond())
                                .endAt(OffsetDateTime.now().toEpochSecond())
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(new CheapestAsGift())
                )
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        assertThrows(MechanicsTypeValidationException.class, () -> validator.validate(request));
    }

    @Test
    public void shouldNotThrowExceptionIfNoPromoMechanic() {

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
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    public void shouldThrowExceptionIfWrongDates() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("cf_123")
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(SourceType.CATEGORYIFACE)
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .status(PromoStatus.NEW)
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(new CheapestAsGift())
                )
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );


        // act and verify
        assertThrows(DateTimeValidationException.class, () -> validator.validate(request));
    }

    @Test
    public void shouldThrowExceptionIfWrongSrcCifaceField() {
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
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        assertThrows(FieldsValidationException.class, () -> validator.validate(request));
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
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
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
                )
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    public void shouldNotThrowIfPromocodeValid() {
        // setup
        long startAt = OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond();
        long endAt = OffsetDateTime.now().toEpochSecond();
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("cf_0000000073")
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(SourceType.CATEGORYIFACE)
                                .mechanicsType(MechanicsType.PROMO_CODE)
                                .status(PromoStatus.NEW)
                                .startAt(startAt)
                                .endAt(endAt)
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .promocode(new Promocode()
                                        .code("CODE")
                                        .codeType(Promocode.CodeTypeEnum.PERCENTAGE)
                                        .value(5))
                );
        doReturn(true).when(promocodeReservationService).isPromocodeAvailable("CODE", startAt, endAt);

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));

        verify(promocodeReservationService).isPromocodeAvailable("CODE", startAt, endAt);
    }

    @Test
    public void shouldThrowIfPromocodeOccupied() {
        // setup
        long startAt = OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond();
        long endAt = OffsetDateTime.now().toEpochSecond();
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("cf_0000000073")
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(SourceType.CATEGORYIFACE)
                                .mechanicsType(MechanicsType.PROMO_CODE)
                                .status(PromoStatus.NEW)
                                .startAt(startAt)
                                .endAt(endAt)
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .promocode(new Promocode()
                                        .code("CODE")
                                        .codeType(Promocode.CodeTypeEnum.PERCENTAGE)
                                        .value(5))
                );
        doReturn(false).when(promocodeReservationService).isPromocodeAvailable("CODE", startAt, endAt);

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class, () -> validator.validate(request));
        assertEquals("Promocode code is occupied by another promo", e.getMessage());

        verify(promocodeReservationService).isPromocodeAvailable("CODE", startAt, endAt);
    }
}
