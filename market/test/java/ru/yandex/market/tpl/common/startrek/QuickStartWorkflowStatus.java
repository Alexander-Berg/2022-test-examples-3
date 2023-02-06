package ru.yandex.market.tpl.common.startrek;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import ru.yandex.market.tpl.common.startrek.ticket.TicketStatusEnum;

@Getter
@RequiredArgsConstructor
public enum QuickStartWorkflowStatus implements TicketStatusEnum {
    OPENED("open"),
    IN_PROGRESS("inProgress"),
    NEED_INFO("needInfo"),
    CLOSED("closed");

    private final String statusKey;

}
