package ru.yandex.direct.core.entity.moderationreason.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonRequest;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonsResponse;

public final class FakeModerationReasonText implements ModerationReasonText {
    private final Map<String, String> reasonsById;

    public FakeModerationReasonText(Map<String, String> reasonsById) {
        this.reasonsById = reasonsById;
    }

    @Override
    public ModerationReasonsResponse showModReasons(ModerationReasonRequest request, @Nullable Long bannerId) {
        final var reasons = Arrays
                .stream(request.getTooltipIDs())
                .collect(Collectors.toMap(id -> id, reasonsById::get));

        return new ModerationReasonsResponse(reasons, List.of());
    }
}
