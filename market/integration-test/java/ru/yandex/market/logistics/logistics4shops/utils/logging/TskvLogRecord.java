package ru.yandex.market.logistics.logistics4shops.utils.logging;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.Accessors;
import one.util.streamex.StreamEx;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.logistics.logistics4shops.config.IntegrationTestConfiguration;
import ru.yandex.market.logistics.logistics4shops.logging.LoggingCode;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecordFormat.ExceptionPayload;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Accessors(chain = true)
@ParametersAreNonnullByDefault
public final class TskvLogRecord<T> {
    private Level level;
    private final TskvLogRecordFormat<T> format;
    private T payload;
    private String requestId;
    private String code;
    private String[] tags;
    private Map<String, ? extends Collection<String>> entities;
    private Map<String, String> extra;

    private TskvLogRecord(TskvLogRecordFormat<T> format, String payloadAsString) {
        this.format = format;
        this.payload = format.parsePayload(payloadAsString);
    }

    private TskvLogRecord(TskvLogRecordFormat<T> format, Level level, T payload) {
        this.format = format;
        this.level = level;
        this.payload = payload;
        this.requestId = IntegrationTestConfiguration.TEST_REQUEST_ID;
    }

    @Nonnull
    public TskvLogRecord<T> setLoggingCode(LoggingCode<?> code) {
        return this.setTag(code.getTag().name())
            .setCode(code.asEnum().name());
    }

    @Nonnull
    public TskvLogRecord<T> setTag(String tag) {
        this.tags = new String[]{tag};
        return this;
    }

    @Nonnull
    public static TskvLogRecord<String> info(String payload) {
        return new TskvLogRecord<>(TskvLogRecordFormat.PLAIN, Level.INFO, payload);
    }

    @Nonnull
    public static TskvLogRecord<String> warn(String payload) {
        return new TskvLogRecord<>(TskvLogRecordFormat.PLAIN, Level.WARN, payload);
    }

    @Nonnull
    public static TskvLogRecord<String> error(String payload) {
        return new TskvLogRecord<>(TskvLogRecordFormat.PLAIN, Level.ERROR, payload);
    }

    @Nonnull
    public static TskvLogRecord<ExceptionPayload> exception(ExceptionPayload payload) {
        return new TskvLogRecord<>(TskvLogRecordFormat.JSON_EXCEPTION, Level.ERROR, payload);
    }

    /**
     * Разбирает строку структурированного tskv лога в типизированную сущность.
     * Алгоритм:
     * <ol>
     * <li>Отрезает лишнее от записи функцией {@link #purifyTskv(String)};</li>
     * <li>Делит запись по табуляциям;</li>
     * <li>Для каждой пары key-value разделяет их по знаку равенства ({@code =}) и сохраняет в промежуточную мапу;</li>
     * <li>Затем исходя из формата сообщения извлекает payload и создает запись нужного формата;</li>
     * <li>Заполняет обязательные и опциональные поля типа {@code level, request_id} и пр.;</li>
     * <li>Отдельная логика для extra: берет значения {@code extra_keys, extra_values} и мержит их обратно в мапу,
     *     ожидая, что их количество совпадет после деления по запятой ({@code ,});</li>
     * <li>Отдельная логика для entity: берет только значения {@code entity_values} и собирает из них мапу,
     *     отделяя значения от ключей по двоеточию ({@code :}).</li>
     * </ol>
     *
     * @param tskvLog запись структурированного tskv лога
     * @return объект записи лога
     * @see ru.yandex.market.logistics.logging.backlog.BackLogUtil#buildBackLogRecordWithMDC
     * @see ru.yandex.market.logistics.logging.backlog.BackLogRecordBuilder#build
     */
    @Nonnull
    @SneakyThrows
    public static TskvLogRecord<?> parseFromStringTskv(String tskvLog) {
        Map<String, String> tskvMap = StreamEx.of(purifyTskv(tskvLog).split("\t"))
            .map(splitOn('='))
            .toMap(Map.Entry::getKey, Map.Entry::getValue);

        TskvLogRecord<?> record = new TskvLogRecord<>(
            TskvLogRecordFormat.getByFormat(tskvMap.get("format")),
            tskvMap.get("payload")
        );

        record.setLevel(Level.valueOf(tskvMap.get("level")));
        Optional.ofNullable(tskvMap.get("request_id")).ifPresent(record::setRequestId);
        Optional.ofNullable(tskvMap.get("code")).ifPresent(record::setCode);

        String[] tags = extractArray(tskvMap, "tags");
        if (ArrayUtils.isNotEmpty(tags)) {
            record.setTags(tags);
        }

        Map<String, String> extraMap = StreamEx.zip(
                extractArray(tskvMap, "extra_keys"),
                extractArray(tskvMap, "extra_values"),
                Pair::of
            )
            .toMap(Pair::getKey, Pair::getValue);
        if (MapUtils.isNotEmpty(extraMap)) {
            record.setExtra(extraMap);
        }

        Map<String, List<String>> entityMap = StreamEx.of(extractArray(tskvMap, "entity_values"))
            .map(splitOn(':'))
            .mapToEntry(Map.Entry::getKey, Map.Entry::getValue)
            .collapseKeys()
            .toMap();
        if (MapUtils.isNotEmpty(entityMap)) {
            record.setEntities(entityMap);
        }

        return record;
    }

    /**
     * Отрезает ненужные начало и конец tskv записи.
     * Предполагается, что любая запись начинается с последовательности
     * {@code <pre>tskv    ts=%timestamp%    level=%level%    ...</pre>}
     * и может заканчиваться переносом строки, который тоже удаляется.
     *
     * @param record запись структурированного tskv лога
     * @return очищенная запись, готовая к разбору
     */
    @Nonnull
    private static String purifyTskv(String record) {
        return record.substring(
            record.indexOf("level"),
            record.length() - (record.endsWith("\n") ? 1 : 0)
        );
    }

    /**
     * Возвращает функцию, которая разделяет строку на две по первому появлению символа {@code delimiter}.
     *
     * @param delimiter разделитель двух частей строки
     * @return функция-разделитель
     */
    @Nonnull
    private static Function<String, Map.Entry<String, String>> splitOn(char delimiter) {
        return kv -> {
            int delimiterPos = kv.indexOf(delimiter);
            return delimiterPos == -1
                ? Pair.of(kv, "")
                : Pair.of(kv.substring(0, delimiterPos), kv.substring(delimiterPos + 1));
        };
    }

    @Nonnull
    private static String[] extractArray(Map<String, String> map, String key) {
        return Optional.ofNullable(map.get(key)).map(str -> str.split(",")).orElse(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    public enum Level {
        ERROR,
        WARN,
        INFO,
    }
}
