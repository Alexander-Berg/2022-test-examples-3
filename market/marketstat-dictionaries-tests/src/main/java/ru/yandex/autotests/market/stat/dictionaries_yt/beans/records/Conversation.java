package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * @author Bogdan Timofeev <timofeevb@yandex-team.ru>
 */

@Data
@DictTable(name = "conversations")
public class Conversation implements DictionaryRecord {
    @DictionaryIdField
    private String conversationId;

    private String orderId;

    private String title;

    private String issueTypes;

    private String claimType;

    private String closureType;

    private String inquiryType;

    private String inquiryDueTs;

    private String inquiry_from_ts;

    private String last_status;

    private String last_status_ts;

    private String last_message_ts;

    private String last_author_role;

    private String created_ts;

    private String resolution_count;

    private String resolution_type;

    private String resolution_subtype;

    private String resolution_ts;

    private String participation_mask;

    private String read_status_mask;

    private String unread_user_count;

    private String unread_shop_count;

    private String unread_arbiter_count;

    private Boolean is_archived;

    private String note_event_mask;

    private String note_event_send_mask;

    private String label_mask;

    private String last_label;
}
