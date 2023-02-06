package ru.yandex.direct.core.entity.moderationreason.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonWithDetails;

public final class FakeModerationReasons implements ModerationReasons {
    private final Map<Long, Map<ModerationReasonObjectType, List<ModerationDiag>>> reasons;
    private final List<ModerationReasonWithDetails> moderationReasonWithDetails;

    public FakeModerationReasons(
            Map<Long, Map<ModerationReasonObjectType, List<ModerationDiag>>> reasons,
            List<ModerationReasonWithDetails> moderationReasonWithDetails
    ) {
        this.reasons = reasons;
        this.moderationReasonWithDetails = moderationReasonWithDetails;
    }

    @Override
    public Map<Long, Map<ModerationReasonObjectType, List<ModerationDiag>>> getReasons(Collection<Long> bannerIds) {
        return bannerIds.stream().filter(reasons::containsKey).collect(Collectors.toMap(id -> id, reasons::get));
    }

    @Override
    public List<ModerationReasonWithDetails> getReasonsWithDetails(int shard, Collection<Long> bannerIds) {
        return moderationReasonWithDetails
                .stream()
                .filter(r -> bannerIds.contains(r.getBannerId()))
                .collect(Collectors.toList());
    }
}
