package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * @author Bogdan Timofeev <timofeevb@yandex-team.ru>
 */
@Data
@DictTable(name = "messages")
public class Message implements DictionaryRecord {

    @DictionaryIdField
    private String messageId;

    private String conversationId;

    private String authorUid;

    private String attachmentGroupId;

    private String authorRoleId;

    private String convBeforeStatus;

    private String convAfterStatus;

    private String messageTs;

    private String text;

    private String isHidden;

    private String privacyMode;

    private String resolutionType;

    private String resolutionSubtype;

    private String label;
}
