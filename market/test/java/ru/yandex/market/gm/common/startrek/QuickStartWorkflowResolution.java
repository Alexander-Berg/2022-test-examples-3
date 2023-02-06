package ru.yandex.market.gm.common.startrek;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import ru.yandex.market.gm.common.startrek.ticket.TicketResolutionEnum;

@Getter
@RequiredArgsConstructor
public enum QuickStartWorkflowResolution implements TicketResolutionEnum {

    RESOLVED("fixed"),
    WONT_BE_FIXED("won'tFix"),
    DUPLICATE("duplicate");

    private final String resolutionKey;

}
