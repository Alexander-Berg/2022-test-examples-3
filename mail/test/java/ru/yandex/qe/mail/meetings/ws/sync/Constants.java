package ru.yandex.qe.mail.meetings.ws.sync;

import java.util.Map;

import ru.yandex.qe.mail.meetings.services.abc.dto.AbcService;
import ru.yandex.qe.mail.meetings.services.staff.dto.StaffGroup;
import ru.yandex.qe.mail.meetings.synchronizer.dto.IdWithType;
import ru.yandex.qe.mail.meetings.synchronizer.dto.SourceType;
import ru.yandex.qe.mail.meetings.synchronizer.dto.SyncEvent;

public final class Constants {
    public static final IdWithType PERSON_IDWT = new IdWithType("person-login", SourceType.PERSON);
    public static final IdWithType PERSON2_IDWT = new IdWithType("person-login2", SourceType.PERSON);
    public static final IdWithType STAFF_IDWT = new IdWithType("1", SourceType.PERSON);
    public static final IdWithType ABC_IDWT = new IdWithType("1", SourceType.ABC);

    public static final int EVENT_ID = 1;
    public static final int EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE = 37730976;
    public static final int EVENT_ID_BY_RUBTSOVDMV_CAN_NOT_EDIT = 37730977;
    public static final int EVENT_ID_BY_RUBTSOVDMV_CAN_NOT_INVITE = 37730978;

    public static final String RUBTSOVDMV = "rubtsovdmv";
    public static final String OWNER = "owner";

    public static final AbcService ABC_SERVICE = new AbcService(1, "abc-1", Map.of("ru", "abc-1"));
    public static final StaffGroup STAFF_GROUP = new StaffGroup(1, "desc", false, "name", "url");

    public static final SyncEvent SYNC_EVENT = new SyncEvent(OWNER, EVENT_ID);

    private Constants(){}
}
