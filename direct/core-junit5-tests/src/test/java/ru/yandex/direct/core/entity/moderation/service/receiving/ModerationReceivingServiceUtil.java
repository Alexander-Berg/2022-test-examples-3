package ru.yandex.direct.core.entity.moderation.service.receiving;

import java.time.LocalDate;
import java.util.List;

import ru.yandex.direct.core.entity.domain.model.ApiDomainStat;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.moderation.model.AbstractModerationResultResponse;
import ru.yandex.direct.core.entity.moderation.model.ModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.ModerationResult;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReason;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


public class ModerationReceivingServiceUtil {
    public static <T extends AbstractModerationResultResponse<?
                extends ModerationMeta, ? extends ModerationResult>> void checkModerationResponses(
            BaseModerationReceivingService<T, ?> moderationReceivingService,
            List<T> responses,
            DomainRepository domainRepository,
            String domain,
            int shard,
            long acceptedItemsExpected,
            long declinedItemsExpected,
            long badReasonsExpected) {
        var result = moderationReceivingService.processModerationResponses(shard, responses);
        assumeThat(sa -> {
            sa.assertThat(result.getLeft()).isZero();
            sa.assertThat(result.getRight()).hasSize(1);
        });


        var apiDomainStat = domainRepository.getDomainsStat(List.of(domain));
        var expectedDomainStat = new ApiDomainStat()
                .withFilterDomain(domain)
                .withStatDate(LocalDate.now())
                .withAcceptedItems(acceptedItemsExpected)
                .withDeclinedItems(declinedItemsExpected)
                .withBadReasons(badReasonsExpected);

        assertThat(apiDomainStat).containsExactlyInAnyOrder(expectedDomainStat);
    }

    public static void checkResponsesWithDetails(int shard,
                                                 ModerationReasonRepository moderationReasonRepository,
                                                 ModerationReasonObjectType objectType,
                                                 Long objectId,
                                                 List<ModerationReasonDetailed> expectedReasonsInDb) {
        var reasons = moderationReasonRepository.fetchRejected(shard, objectType, List.of(objectId));
        assertThat(reasons)
                .singleElement()
                .extracting(ModerationReason::getReasons)
                .asList()
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(expectedReasonsInDb);
    }
}
