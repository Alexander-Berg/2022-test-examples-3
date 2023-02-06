package ru.yandex.market.core.testing;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import org.apache.commons.lang3.StringUtils;

/**
 * Статус магазина в песочном индеске.
 * Вместе с {@link TestingType} определяет полное состояние магазина в тестинг
 *
 * @author zoom
 */
@XmlEnum
public enum TestingStatus {

    /**
     * Состояние магазина в тестинге не было сконвертированно в новый формат.
     */
    @XmlEnumValue("0")
    UNDEFINED(0),

    /**
     * Были иницирована необходимость отправки магазина на проверку.
     */
    @XmlEnumValue("1")
    INITED(1),

    /**
     * Магазин ожидает запуска проверки. Тип проверки определяется через {@link TestingType причину нахождения магазина
     * в песочном индексе}. Запуск проверки может быть по разным причинам отложен на разное время
     */
    @XmlEnumValue("2")
    READY_FOR_CHECK(2),

    /**
     * На этом этапе фиды начинают грузиться в индекс. Ждем первой успешной индексации.
     */
    @XmlEnumValue("3")
    WAITING_FEED_FIRST_LOAD(3),

    /**
     * Первая успешна индексация фида произошла. Либо магазин находится на самопроверке либо магазин находится (или в
     * ближайшее время отправится) на премодерации в АБО/СКК.
     */
    @XmlEnumValue("4")
    CHECKING(4),

    /**
     * Проверка загрузки фида после успешного прохождения премодерации в СКК.
     */
    @XmlEnumValue("5")
    WAITING_FEED_LAST_LOAD(5),

    /**
     * Последняя проверка фидов были успешно завершена. Премодерация пройдена.
     */
    @XmlEnumValue("6")
    PASSED(6),

    /**
     * Попытка магазина пройти премодерацию была отменена без штрафов.
     */
    @XmlEnumValue("7")
    CANCELED(7),

    /**
     * Магазин провалил премодерацию. Ожидается подтверждение результата.
     */
    @XmlEnumValue("8")
    READY_TO_FAIL(8),

    /**
     * Тестирование отменено, так как магазин провалил премодерацию. Ждем, когда магазин нажмет кнопку отправки на
     * премодерацию.
     */
    @XmlEnumValue("9")
    FAILED(9),

    /**
     * У магазина отключена возможность проходить какие-либо проверки.
     */
    @XmlEnumValue("10")
    DISABLED(10),

    /**
     * Истек срок нахождения магазина в песочном индексе на самопроверке.
     */
    @XmlEnumValue("11")
    EXPIRED(11),

    /**
     * Запуск проверки магазина отложен на определенное время. Магазину даётся время на внесение последних изменений
     * параметров.
     */
    @XmlEnumValue("12")
    PENDING_CHECK_START(12),

    /**
     * Модерация остановлена, от магазина требуются документы.
     */
    @XmlEnumValue("13")
    NEED_INFO(13); //

    public static final Set<TestingStatus> MODERATION_IS_NOT_REQUESTED_STATUSES =
            Collections.unmodifiableSet(EnumSet.of(FAILED, CANCELED, READY_TO_FAIL, DISABLED, INITED));

    private static final TestingStatus[] INDEXED_BY_ID_STATUSES;

    static {
        int maxId = -1;
        for (TestingStatus status : values()) {
            int id = status.getId();
            if (id < 0) {
                throw new IllegalStateException("Do not use negative numbers for an enum ID. ID: " + id);
            }
            maxId = Math.max(maxId, id);
        }
        if (maxId > 100) {
            throw new IllegalStateException("Try to minimize number of statuses. ID:" + maxId);
        }
        INDEXED_BY_ID_STATUSES = new TestingStatus[maxId + 1];
        for (TestingStatus status : values()) {
            int id = status.getId();
            if (INDEXED_BY_ID_STATUSES[id] != null) {
                throw new IllegalStateException("Duplicated id was found. ID: " + id);
            }
            INDEXED_BY_ID_STATUSES[id] = status;
        }
    }

    private final int id;

    TestingStatus(int id) {
        this.id = id;
    }

    public static TestingStatus valueOf(int id) {
        TestingStatus result = INDEXED_BY_ID_STATUSES[id];
        if (result == null) {
            throw new IllegalArgumentException("Unknown testing status. ID: " + id);
        }
        return result;
    }

    public int getId() {
        return id;
    }

    public boolean moderationIsRequested() {
        return !MODERATION_IS_NOT_REQUESTED_STATUSES.contains(this);
    }

    public String getQuotedValue() {
        return StringUtils.wrap(String.valueOf(id), "'");
    }
}
