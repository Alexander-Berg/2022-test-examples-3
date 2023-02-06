package ru.yandex.market.sqb.test.db;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.assertj.core.api.Assertions;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.market.sqb.model.common.AbstractNameModel;
import ru.yandex.market.sqb.model.common.HasName;
import ru.yandex.market.sqb.model.conf.ParameterModel;
import ru.yandex.market.sqb.model.conf.QueryModel;
import ru.yandex.market.sqb.model.filter.QueryModelFilter;
import ru.yandex.market.sqb.model.filter.QueryModelFilterBuilder;
import ru.yandex.market.sqb.service.config.ConfigurationModelFilterService;
import ru.yandex.market.sqb.service.config.ConfigurationModelService;
import ru.yandex.market.sqb.service.config.reader.composite.CompositeConfigurationReader;
import ru.yandex.market.sqb.util.SqbGenerationUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Компонент для проверки корректности модели заспроса.
 * Использует подключение к тестовой БД.
 *
 * @author Vladislav Bauer
 */
public final class DbQueryConfigChecker {

    private DbQueryConfigChecker() {
        throw new UnsupportedOperationException();
    }


    public static void checkRequiredFields(final QueryModel queryModel, final Supplier<String> fieldsData) {
        final Function<Stream<String>, List<String>> normalize = lines -> lines
                .map(StringUtils::lowerCase)
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        final List<String> requiredFields = normalize.apply(fieldsData.get().lines());
        final List<String> queryFields = normalize.apply(
                queryModel.getParameters().stream().map(ParameterModel::getName));

        final Collection<String> disjunction = CollectionUtils.disjunction(requiredFields, queryFields);
        assertThat("Замечено изменение в конфигурации! Внимательно ознакомьтесь с " +
                        "https://wiki.yandex-team.ru/MBI/NewDesign/sql-query-builder/#izmenenijadljapotrebitelejj",
                disjunction, empty());
    }

    /**
     * Проверяет, что набор данных в actuals равен набору данных expects.
     * Равенство подразумевает:
     * <ol>
     * <li>Кол-во записей в actuals и expects одинаково</li>
     * <li>Размер каждого элемета в actuals и expects одинаков</li>
     * <li>Элементы по соответствующим ключам в map'ах в actuals и expects одинаковы (equals)</li>
     * </ol>
     *
     * @param actuals    список строк
     * @param expects    список "эталонных" строк
     * @param uniqueKey  ключ
     * @param checkOrder флаг упорядоченности строк
     */
    public static void checkRows(
            @Nonnull final List<Map<String, Object>> actuals,
            @Nonnull final List<Map<String, Object>> expects,
            @Nonnull final String uniqueKey,
            final boolean checkOrder
    ) {
        Assertions.assertThat(actuals)
                .overridingErrorMessage(() -> {
                    List<Object> actualShopId = actuals.stream()
                            .map(row -> row.get("SHOP_ID"))
                            .map(Object::toString)
                            .sorted()
                            .collect(Collectors.toList());
                    List<Object> expectedShopId = expects.stream()
                            .map(row -> row.get("SHOP_ID"))
                            .map(Object::toString)
                            .sorted()
                            .collect(Collectors.toList());
                    return String.format("Different amount of rows. Actual: %d. Expected: %d." +
                                    "\nActual shop ids: %s.\nExpected shop ids: %s",
                            actuals.size(), expects.size(), actualShopId, expectedShopId);
                })
                .hasSize(expects.size());

        final List<String> errorNoRow = new ArrayList<>();
        final List<String> errorColumn = new ArrayList<>();
        final List<ValueError> errorValue = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < expects.size(); ++rowIndex) {
            final Map<String, Object> expect = expects.get(rowIndex);
            final String uniqueValue = StringUtils.defaultIfEmpty(value(expect.get(uniqueKey)), StringUtils.EMPTY);
            final Map<String, Object> actual =
                    checkOrder ? actuals.get(rowIndex) : getRowByUniqueKey(actuals, uniqueKey, uniqueValue);

            if (actual == null) {
                errorNoRow.add(uniqueValue);
                continue;
            }
            if (actual.size() != expect.size()) {
                errorColumn.add(uniqueValue);
                continue;
            }

            for (final Map.Entry<String, Object> entry : expect.entrySet()) {
                final String key = entry.getKey();
                final Object ex = entry.getValue();
                final Object act = actual.get(key);

                final String a = value(act);
                final String e = value(ex);

                if (!equalValues(a, e)) {
                    errorValue.add(new ValueError(uniqueKey, uniqueValue, key, e, a));
                }
            }
        }

        if (!errorNoRow.isEmpty() || !errorColumn.isEmpty() || !errorValue.isEmpty()) {
            final Collector<CharSequence, ?, String> joiner = Collectors.joining(SystemUtils.LINE_SEPARATOR);
            fail(
                    errorNoRow.stream()
                            .map(v -> String.format("No corresponding row (%s:%s)", uniqueKey, v))
                            .collect(joiner) +
                    errorColumn.stream().
                            map(v -> String.format("Amount of columns is wrong (%s:%s)", uniqueKey, v))
                            .collect(joiner) +
                    errorValue.stream()
                            .map(ValueError::toString)
                            .collect(joiner)
            );
        }
    }

    /**
     * Валит тест, если список параметро badParams не пуст с соответствующим сообщением.
     */
    public static void checkParameters(@Nullable final List<ParamResultInfo> badParams) {
        if (!CollectionUtils.isEmpty(badParams)) {
            final String separator = SystemUtils.LINE_SEPARATOR + SystemUtils.LINE_SEPARATOR;
            final String params = StringUtils.join(badParams, separator);
            fail(String.format("Following parameters have problems:%s%s", separator, params));
        }
    }

    /**
     * Проверяет запрос, получаемый из reader.
     * Список проверок:
     *
     * <ol>
     * <li>Запрос корректно парсится</li>
     * <li>Данные, получаемые по этому запросу, содержат указанное в нем кол-во колонок</li>
     * <li>Результат запроса - непустое множество</li>
     * </ol>
     */
    @Nonnull
    public static QueryResultInfo checkAll(@Nonnull final Supplier<String> reader) {
        final QueryModel query = getQuery(reader);

        assertThat(query, notNullValue());

        final List<ParamResultInfo> badParams = Lists.newArrayList();
        final List<Map<String, Object>> rows = Lists.newArrayList();

        try {
            // Проверяем модель
            rows.addAll(checkQuery(query));
        } catch (final Throwable ex) {
            System.err.println("Internal error: \n" + ex);
            badParams.addAll(detectBadParams(query));
        }
        return new QueryResultInfo(query, rows, badParams);
    }

    /**
     * Проверка, что типы параметров указанные в конфиге соответствют прибитым типам из definedParameterTypes.yaml.
     */
    public static void checkDefinedParameterTypes(@Nonnull final Supplier<String> reader,
                                                  Map<String, Optional<String>> definedParameters) {
        if (MapUtils.isNotEmpty(definedParameters)) {
            Map<String, Optional<String>> queryParameterTypes =
                    getQuery(reader)
                            .getParameters()
                            .stream()
                            .collect(Collectors.toMap(AbstractNameModel::getName, ParameterModel::getType));

            definedParameters.forEach((parameter, type) -> assertThat(
                    String.format(
                            Optional.ofNullable(queryParameterTypes.get(parameter)).isPresent()
                                    ? "Тип параметра %s не совпадает со значением указанным в definedParameterTypes"
                                    : "Параметр %s был удалён из конфига, но остался прибитым в definedParameterTypes",
                            parameter
                    ),
                    type,
                    equalTo(queryParameterTypes.get(parameter)))
            );
        }
    }

    private static QueryModel getQuery(final Supplier<String> reader) {
        final ConfigurationModelService configurationModelService = ConfigurationModelService.createDefault();
        return reader instanceof CompositeConfigurationReader
                ? configurationModelService.read((CompositeConfigurationReader) reader)
                : configurationModelService.read(reader);
    }

    /**
     * Проверяет, что список колонок в resultSet соответствует тому, что указан в query. Проверяется кол-во колонок и их
     * тип.
     */
    public static void checkColumns(
            @Nonnull final ResultSet resultSet, @Nonnull final QueryModel query
    ) throws SQLException {
        final List<String> columnNames = DbUtils.getColumnLabels(resultSet);
        assertThat(columnNames, not(emptyCollectionOf(String.class)));

        final List<ParameterModel> parameters = query.getParameters();
        final List<String> parameterNames = HasName.getNames(parameters);

        assertThat(columnNames, hasSize(parameterNames.size()));
        assertThat(columnNames, equalTo(parameterNames));
    }


    /**
     * Находит параметры, которых не существует в выдаче по заданной query для текущих данных.
     *
     * @param query модель запроса
     * @return список {@link ParamResultInfo}, для параметров, которых нет в выдаче
     */
    private static List<ParamResultInfo> detectBadParams(final QueryModel query) {
        final List<ParameterModel> parameters = query.getParameters();
        final List<String> parameterNames = HasName.getNames(parameters);

        return parameterNames.stream()
                .map(parameter -> checkSingleParameter(query, parameter))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Проверяет, что указанный параметр существует в выдаче по заданной query и для него есть хотя бы 1 значение.
     */
    private static ParamResultInfo checkSingleParameter(final QueryModel query, final String parameter) {
        try {
            final ConfigurationModelFilterService filterService = new ConfigurationModelFilterService();
            final QueryModelFilter filter = new QueryModelFilterBuilder()
                    .setParameterFilters(Collections.singletonList(HasName.byNames(parameter)))
                    .build();

            final QueryModel newQuery = filterService.filter(query, filter);
            checkQuery(newQuery);
            return null;
        } catch (final Throwable ex) {
            return new ParamResultInfo(parameter, ex.getMessage());
        }
    }

    /**
     * Выполняет query на тестовых данных и проверяет комплектность колонок и наличие хотя бы 1 строки результата.
     */
    private static List<Map<String, Object>> checkQuery(final QueryModel query) {
        return DbUtils.runWithStatement(statement -> {
            final String sql = SqbGenerationUtils.generateSQL(query);
            assertThat(sql, not(emptyOrNullString()));
            assertThat("SQL query cannot be ended with semicolon", sql, new SqlStringEndsProperlyMatcher());

            final ResultSet resultSet = statement.executeQuery(sql);
            assertThat(resultSet, notNullValue());

            checkColumns(resultSet, query);

            final List<Map<String, Object>> rows = DbUtils.getRows(resultSet);
            assertThat("Could not fetch no one row", rows.size(), greaterThan(0));

            return rows;
        });
    }

    private static Map<String, Object> getRowByUniqueKey(
            final List<Map<String, Object>> rows,
            final String uniqueKey, final String uniqueValue
    ) {
        for (final Map<String, Object> row : rows) {
            final Object value = row.get(uniqueKey);
            if (equalValues(value(value), uniqueValue)) {
                return row;
            }
        }
        return null;
    }

    private static String value(Object obj) {
        return StringUtils.trimToNull(Objects.toString(obj, null));
    }

    private static boolean equalValues(final String a, final String b) {
        try {
            // Если строка представляет собой число - сравниваются числа, а не строки
            BigDecimal decimalA = new BigDecimal(a);
            BigDecimal decimalB = new BigDecimal(b);
            if (decimalA.compareTo(decimalB) != 0) {
                return false;
            }
        } catch (NullPointerException | NumberFormatException exception) {
            if (!Objects.equals(a, b)) {
                return false;
            }
        }
        return true;
    }

    public static class ParamResultInfo implements HasName {

        private final String name;
        private final String error;

        ParamResultInfo(final String name, final String error) {
            this.name = name;
            this.error = error;
        }

        @Override
        public String getName() {
            return name;
        }

        public String getError() {
            return error;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ParamResultInfo) {
                final ParamResultInfo other = (ParamResultInfo) obj;
                return Objects.equals(getName(), other.getName());
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(getName());
        }

    }

    public static class QueryResultInfo {

        private final QueryModel query;
        private final List<Map<String, Object>> rows;
        private final List<ParamResultInfo> badParams;

        private QueryResultInfo(
                final QueryModel query, final List<Map<String, Object>> rows, final List<ParamResultInfo> badParams
        ) {
            this.query = query;
            this.rows = rows;
            this.badParams = badParams;
        }

        public static boolean isOrdered(@Nonnull final QueryResultInfo resultInfo) {
            final QueryModel query = resultInfo.getQuery();
            return !CollectionUtils.isEmpty(query.getOrders());
        }

        public QueryModel getQuery() {
            return query;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }

        public List<ParamResultInfo> getBadParams() {
            return badParams;
        }

    }

    public static class ValueError {

        private final String uniqueKey;
        private final String uniqueValue;
        private final String keyName;
        private final String expectedValue;
        private final String actualValue;

        private ValueError(
                final String uniqueKey, final String uniqueValue, final String keyName,
                final String expectedValue, final String actualValue
        ) {
            this.uniqueKey = uniqueKey;
            this.uniqueValue = uniqueValue;
            this.keyName = keyName;
            this.expectedValue = expectedValue;
            this.actualValue = actualValue;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("(%s:%s) key: %s, expect: %s, actual: %s",
                    uniqueKey, uniqueValue, keyName, expectedValue, actualValue);
        }

    }

    private static class SqlStringEndsProperlyMatcher extends TypeSafeMatcher<String> {
        @Override
        protected boolean matchesSafely(String item) {
            //удаляем все комментарии и пробельные символы - последний символ не должен оказаться ';'
            return !item.replaceAll("/\\*.*\\*/", "").replaceAll("\\s", "").endsWith(";");
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("SQL query string that is not ended with semicolon");
        }
    }

}
