package ru.yandex.market.logistics.management.service.export.dynamic.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.logistics.management.domain.entity.DynamicFault;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;

import static ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus.FAILED;
import static ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus.WARN;

class PartnerRelationDynamicValidationServiceTest {

    private static final ValidationRule RULE_ID = buildRule(pr -> pr.getId() != 666);
    private static final ValidationRule RULE_HANDLING_TIME =
        buildRule(pr -> pr.getHandlingTime() != 666);

    private static PartnerRelationDynamicValidationService service;

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @BeforeAll
    static void setUp() {
        var validationService = new DynamicValidationService(Arrays.asList(RULE_ID, RULE_HANDLING_TIME));
        service = new PartnerRelationDynamicValidationService(validationService);
    }

    @Test
    void testEmptyCollection() {

        Pair<List<PartnerRelationDto>, List<DynamicFault>> result = service.validate(Collections.emptyList());

        softly.assertThat(result.getFirst()).isEmpty();
        softly.assertThat(result.getSecond()).isEmpty();
    }

    @Test
    void testAllValid() {
        List<PartnerRelationDto> partnerRelations = Arrays.asList(
            partnerRelation(1, 1, true), // OK
            partnerRelation(2, 2, false) // OK
        );

        Pair<List<PartnerRelationDto>, List<DynamicFault>> result = service.validate(partnerRelations);

        softly.assertThat(result.getFirst()).hasSameElementsAs(partnerRelations);
        softly.assertThat(result.getSecond()).isEmpty();
    }

    @Test
    void testWarnIgnored() {
        PartnerRelationDto ok = partnerRelation(1, 1, true);
        PartnerRelationDto okWarn = partnerRelation(2, 666, false);
        PartnerRelationDto warnWarn = partnerRelation(666, 666, false);
        List<PartnerRelationDto> partnerRelations = Arrays.asList(ok, okWarn, warnWarn);

        Pair<List<PartnerRelationDto>, List<DynamicFault>> result = service.validate(partnerRelations);

        softly.assertThat(result.getSecond()).hasSize(2);
        softly.assertThat(result.getSecond()).extracting(DynamicFault::getStatus).containsOnly(WARN);
        softly.assertThat(result.getSecond()).extracting(DynamicFault::getReasons)
            .containsExactly(
                "Для связки [id=2] '11 + 12' 2, #связканевыгружается.",
                "Для связки [id=666] '11 + 12' 666, #связканевыгружается. "
                    + "Для связки [id=666] '11 + 12' 666, #связканевыгружается."
            );
        softly.assertThat(result.getSecond()).extracting(DynamicFault::getEntityId).containsExactly(2L, 666L);
        softly.assertThat(result.getSecond()).extracting(DynamicFault::getStatus).containsOnly(WARN);

        softly.assertThat(result.getFirst()).hasSize(1);
        softly.assertThat(result.getFirst()).extracting(PartnerRelationDto::getId).containsExactly(1L);
    }

    @Test
    void testFailed() {
        PartnerRelationDto okFail = partnerRelation(1, 666, true);
        PartnerRelationDto ok = partnerRelation(2, 1, true);
        PartnerRelationDto okWarn = partnerRelation(3, 666, false);
        PartnerRelationDto warnWarn = partnerRelation(666, 666, false);
        PartnerRelationDto failOk = partnerRelation(666, 2, true);

        List<PartnerRelationDto> partnerRelations = Arrays.asList(okFail, ok, okWarn, warnWarn, failOk);

        try {
            service.validate(partnerRelations);
            Assert.fail("Throwing Exception is expected, but was not");
        } catch (PartnerRelationException e) {
            List<DynamicFault> faults = e.getInvalidRelations();

            softly.assertThat(faults).hasSize(4);

            softly.assertThat(faults).extracting(DynamicFault::getReasons).containsExactly(
                "Для связки [id=1] '11 + 12' 1, #динамикневыгружается.",
                "Для связки [id=3] '11 + 12' 3, #связканевыгружается.",
                "Для связки [id=666] '11 + 12' 666, #связканевыгружается. "
                    + "Для связки [id=666] '11 + 12' 666, #связканевыгружается.",
                "Для связки [id=666] '11 + 12' 666, #динамикневыгружается."
            );
            softly.assertThat(faults).extracting(DynamicFault::getStatus).containsExactly(FAILED, WARN, WARN, FAILED);
            softly.assertThat(faults).extracting(DynamicFault::getEntityId).containsExactly(1L, 3L, 666L, 666L);

        }
    }

    private static PartnerRelationDto partnerRelation(long id, int handlingTime, boolean enabled) {
        return new PartnerRelationDto()
            .setId(id)
            .setHandlingTime(handlingTime)
            .setEnabled(enabled)

            .setFromPartner(
                new PartnerDto()
                    .setId(11L)
                    .setReadableName("11")
            )
            .setToPartner(
                new PartnerDto()
                    .setId(12L)
                    .setReadableName("12")
            );
    }

    private static ValidationRule buildRule(Predicate<PartnerRelationDto> predicate) {
        return pr -> {
            PartnerRelationDto prModel = (PartnerRelationDto) pr;
            if (predicate.test(prModel)) {
                return Result.ok();
            }
            return prModel.getEnabled() ? Result.failed(prModel.getId().toString()) :
                Result.warn(prModel.getId().toString());
        };
    }
}
