package ru.yandex.market.loyalty.test.database;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.ASTNodeAccess;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static ru.yandex.market.loyalty.lightweight.CollectionUtils.concat;

/**
 * Этот класс парсит заданный SQL и проверяет по метаданным что на колонках в
 * фильтрах WHERE навешаны индексы.
 *
 * @see #validate(String)
 */
public class IndexSQLValidator implements SQLValidator {
    private static final String CREATE_KEYWORD = "create";
    private static final String TRUNCATE_KEYWORD = "truncate";
    private static final String INSERT_KEYWORD = "insert";
    private static final String DELETE_KEYWORD = "delete";
    /*
     если в теле sql встречается этот коментарий то ошибки индексов игнорируются
     */
    private static final Pattern VALIDATION_DISABLE_COMMENT = Pattern.compile(
            "/\\*[\\s]*validator[\\s]*=[\\s]*false[\\s]*\\*/"
    );

    private static final Logger logger = LogManager.getLogger(IndexSQLValidator.class);


    private final DatabaseMetadata databaseMetadata;

    private final Set<String> validated;
    private final Map<String, List<String>> failures;
    private final List<String> excludedTables;
    private final List<Pair<String, String>> excludedColumns;
    private final List<Pair<Pair<String, String>, List<Pair<String, String>>>> excludedColumnsPrecise;

    /**
     * @param databaseMetadata
     * @param excludedColumns        пропустить ошибку если непроиндексированная колонка перечислена в этом списке
     * @param excludedTables         пропустить ошибку если непроиндексированная колонка находится в одной из
     *                               указанных таблиц
     * @param excludedColumnsPrecise пропустить ошибку если непроиндексированная колонка находится в этом списке
     *                               как ключ и если в запросе кроме нее есть фильтры на колонки из списка в значении
     */
    public IndexSQLValidator(
            DatabaseMetadata databaseMetadata, List<Pair<String, String>> excludedColumns, List<String> excludedTables,
            List<Pair<Pair<String, String>, List<Pair<String, String>>>> excludedColumnsPrecise
    ) {
        this.failures = new HashMap<>();
        this.validated = new HashSet<>();
        this.databaseMetadata = databaseMetadata;
        this.excludedColumns = excludedColumns;
        this.excludedTables = excludedTables;
        this.excludedColumnsPrecise = excludedColumnsPrecise;
    }

    public static Pair<Pair<String, String>, List<Pair<String, String>>> excludeColumnPrecise(
            String tableName, String columnName, List<Pair<String, String>> columns
    ) {
        return Pair.of(Pair.of(tableName, columnName), columns);
    }

    public void validate(String sql) {
        if (databaseMetadata.isLoaded()) {
            if (VALIDATION_DISABLE_COMMENT.matcher(sql).find()
                    || validated.contains(sql)
                    // парсер не поддерживает DML и TRUNCATE со словом CASCADE
                    || sql.substring(0, CREATE_KEYWORD.length()).equalsIgnoreCase(CREATE_KEYWORD)
                    || sql.substring(0, TRUNCATE_KEYWORD.length()).equalsIgnoreCase(TRUNCATE_KEYWORD)
                    // insert не имеет условий а delete мы не используем
                    || sql.substring(0, DELETE_KEYWORD.length()).equalsIgnoreCase(DELETE_KEYWORD)
                    || sql.substring(0, INSERT_KEYWORD.length()).equalsIgnoreCase(INSERT_KEYWORD)
            ) {
                return;
            }
            logger.info(sql);
            Statement stmt;
            try {
                stmt = CCJSqlParserUtil.parse(sql);
                stmt.accept(new StatementVisitorAdapter() {
                    @Override
                    public void visit(Update update) {
                        List<String> errors = checkWhereExpression(update.getWhere(), update.getTables());
                        if (!errors.isEmpty()) {
                            failures.put(sql, errors);
                        }
                    }

                    @Override
                    public void visit(Select select) {
                        select.getSelectBody().accept(new SelectVisitorAdapter() {
                            @Override
                            public void visit(PlainSelect select) {
                                List<String> errors = checkWhereExpression(select.getWhere(), getTables(select));
                                if (!errors.isEmpty()) {
                                    failures.put(sql, errors);
                                }
                            }
                        });
                    }
                });
            } catch (JSQLParserException e) {
                logger.error("error parsing: {}", e.getMessage());
            } catch (UnsupportedOperationException ex) {
                logger.error("unsupported: {}", ex.getMessage());
            } finally {
                validated.add(sql);
            }
        }
    }

    private static List<Table> getTables(PlainSelect plainSelect) {
        Tables tables = new Tables();
        if (plainSelect.getFromItem() != null) {
            plainSelect.getFromItem().accept(tables);
        }
        if (plainSelect.getJoins() != null) {
            plainSelect.getJoins().forEach(j -> j.getRightItem().accept(tables));
        }
        return tables.getTables();
    }

    private List<String> checkWhereExpression(Expression where, List<Table> tables) {
        if (where == null) {
            return Collections.emptyList();
        }

        ComparisonChecker comparisonChecker = new ComparisonChecker(
                tables,
                databaseMetadata,
                tree(where),
                excludedTables, excludedColumns, excludedColumnsPrecise
        );
        where.accept(comparisonChecker);

        comparisonChecker.postMulticolumnIndexCheck();

        return comparisonChecker.getErrors();
    }

    private static ANDsIndex tree(Expression where) {
        ANDsIndex tree = new ANDsIndex();
        where.accept(tree);
        return tree;
    }

    public void startTest() {
        databaseMetadata.load();
        failures.clear();
    }

    public List<String> finishTest() {
        Map<String, List<String>> result = new HashMap<>(failures);
        failures.clear();
        return result.entrySet().stream()
                .map(e -> e.getKey() + ": " + String.join(", ", e.getValue()))
                .collect(Collectors.toList());
    }

    static class Tables extends FromItemVisitorAdapter {
        private List<Table> tables;

        private Tables() {
            tables = new ArrayList<>();
        }

        public List<Table> getTables() {
            return tables;
        }

        @Override
        public void visit(Table tableName) {
            tables.add(tableName);
        }

        @Override
        public void visit(SubSelect subSelect) {
            // для внешнего фильтра нужно транзитивно выяснять в какой таблице физически лежит колонка
            throw new UnsupportedOperationException("подзапросы не поддерживаются");
        }
    }

    static class ANDedColumn {
        private final Table table;
        private final Column column;
        private final Expression operator;

        public ANDedColumn(Table table, Column column, Expression operator) {
            this.table = table;
            this.column = column;
            this.operator = operator;
        }

        public Table getTable() {
            return table;
        }

        public Column getColumn() {
            return column;
        }

        public Expression getOperator() {
            return operator;
        }
    }

    static class ANDedColumns {
        private Table table;
        private List<Column> columns;
        private List<Expression> operators;

        public ANDedColumns() {
        }

        public ANDedColumns(Table table, List<Column> columns, List<Expression> operators) {
            this.table = table;
            this.columns = columns;
            this.operators = operators;
        }

        public Table getTable() {
            return table;
        }

        public List<Column> getColumns() {
            return columns;
        }

        public List<Expression> getOperators() {
            return operators;
        }

        public ANDedColumns merge(ANDedColumns t) {
            return new ANDedColumns(
                    ObjectUtils.firstNonNull(table, t.table),
                    concat(columns, t.columns).collect(toList()),
                    concat(operators, t.operators).collect(toList())
            );
        }
    }

    /**
     * <p>Проверяем выражения которые индексируются обычными B-tree индексами. Выражения для
     * которых Postgresql задействует B-tree индекс должно быть вида: <code>колонка (&lt; | &gt; | &lt;= | &gt;= | =)
     * константа<code></p>
     *
     * <p>Вместо <code>колонки</code> может быть <code>выражение</code> и тогда индекс должен быть по выражению.</p>
     *
     * <p>Также для одиночных сравнений может при определенных условиях использоваться мультиколоночный индекс. Для
     * этого
     * несколько или все его первые колонки связаны логическим AND.</p>
     */
    private static class ComparisonChecker extends ExpressionVisitorAdapter {
        private final List<Table> tables;
        private final DatabaseMetadata databaseMetadata;
        private final ANDsIndex andsIndex;
        private final Map<Expression, Optional<Boolean>> failures;
        private final List<Pair<String, String>> allColumns;
        private final List<String> excludedTables;
        private final List<Pair<String, String>> excludedColumns;
        private final List<Pair<Pair<String, String>, List<Pair<String, String>>>> excludedColumnsPrecise;

        public ComparisonChecker(
                List<Table> tables,
                DatabaseMetadata databaseMetadata,
                ANDsIndex andsIndex,
                List<String> excludedTables,
                List<Pair<String, String>> excludedColumns,
                List<Pair<Pair<String, String>, List<Pair<String, String>>>> excludedColumnsPrecise
        ) {
            this.tables = tables;
            this.databaseMetadata = databaseMetadata;
            this.andsIndex = andsIndex;
            this.excludedTables = excludedTables;
            this.excludedColumns = excludedColumns;
            this.excludedColumnsPrecise = excludedColumnsPrecise;
            this.failures = new HashMap<>();
            this.allColumns = new ArrayList<>();
        }

        @Override
        public void visit(EqualsTo equalsTo) {
            checkComparision(equalsTo);
            super.visit(equalsTo);
        }

        @Override
        public void visit(GreaterThan greaterThan) {
            checkComparision(greaterThan);
            super.visit(greaterThan);
        }

        @Override
        public void visit(GreaterThanEquals greaterThanEquals) {
            checkComparision(greaterThanEquals);
            super.visit(greaterThanEquals);
        }

        @Override
        public void visit(MinorThan minorThan) {
            checkComparision(minorThan);
            super.visit(minorThan);
        }

        @Override
        public void visit(MinorThanEquals minorThanEquals) {
            checkComparision(minorThanEquals);
            super.visit(minorThanEquals);
        }

        @Override
        public void visit(NotEqualsTo notEqualsTo) {
            checkComparision(notEqualsTo);
            super.visit(notEqualsTo);
        }

        @Override
        public void visit(InExpression inExpression) {
            checkInExpression(inExpression);
            super.visit(inExpression);
        }

        @Override
        public void visit(LikeExpression likeExpression) {
            checkLikeExpression(likeExpression);
            super.visit(likeExpression);
        }

        public List<String> getErrors() {
            return failures.entrySet()
                    .stream()
                    .filter(e -> e.getValue().orElse(false))
                    .map(e -> "not indexed: " + e.getKey())
                    .collect(toList());
        }

        public void postMulticolumnIndexCheck() {
            // тут нужно посчитать выражения которые покрываются мультиколоночными индексами
            List<Expression> conditions = new ArrayList<>(failures.keySet());

            // собираем выражения родительский элемент которых является оператором AND
            // так это бинарный оператор то дерево имеет вид (a AND (b AND (c AND d)))
            // поэтому чтобы сматчить a,b,c,d в один AND нужно в цикле подниматься к самому верхнему родительскому AND
            Map<AndExpression, List<ANDedColumn>> andExpressions = new HashMap<>();
            for (Expression operator : conditions) {
                Expression current = andsIndex.getParentAND(operator);
                Expression parent = null;
                while (isAND(current)) {
                    parent = current;
                    current = andsIndex.getParentAND(current);
                }

                Pair<Table, Column> column = getColumn(operator);
                if (isAND(parent)) {
                    List<ANDedColumn> operators = andExpressions.computeIfAbsent(
                            (AndExpression) parent, e -> new ArrayList<>());
                    operators.add(new ANDedColumn(column.getKey(), column.getValue(), operator));
                } else {
                    // предположение о мультиколоночном индексе не сработало
                    // просто смотрим одноколоночный индекс
                    if (!failures.get(operator).isPresent()) {
                        boolean indexed = databaseMetadata.isIndexed(column.getRight(), column.getLeft(), i -> i == 0);
                        boolean value = false;
                        if (!(indexed || isExcluded(column.getRight(), column.getLeft()))) {
                            value = true;
                        }
                        failures.put(operator, Optional.of(value));
                    }
                }
            }

            for (Map.Entry<AndExpression, List<ANDedColumn>> a : andExpressions.entrySet()) {
                Map<Table, ANDedColumns> chunk = a.getValue().stream()
                        .collect(
                                groupingBy(
                                        ANDedColumn::getTable,
                                        mapping(
                                                aa -> new ANDedColumns(aa.getTable(), singletonList(aa.getColumn()),
                                                        singletonList(aa.getOperator())),
                                                reducing(new ANDedColumns(), ANDedColumns::merge)
                                        )));


                for (Map.Entry<Table, ANDedColumns> t : chunk.entrySet()) {
                    // проверяем что группа колонок объедененная через AND проиндексирована
                    // для этого достаточно чтобы хотябы одна колонка из группы стояла в индексе на первом месте
                    boolean indexed = databaseMetadata.isIndexed(t.getValue().getColumns(), t.getKey());
                    // после проверки записываем результат
                    for (Expression operator : t.getValue().getOperators()) {
                        if (!failures.get(operator).isPresent()) {
                            Pair<Table, Column> column = getColumn(operator);
                            boolean value = false;
                            if (!(indexed || isExcluded(column.getRight(), column.getLeft()))) {
                                value = true;
                            }
                            failures.put(operator, Optional.of(value));
                        }
                    }
                }
            }
        }

        private void checkComparision(ComparisonOperator comparisonOperator) {
            Expression leftExpression = comparisonOperator.getLeftExpression();
            Expression rightExpression = comparisonOperator.getRightExpression();
            if (isConstant(leftExpression) && isConstant(rightExpression)) {
                return;
            }
            if (isConstant(leftExpression) || isConstant(rightExpression)) {
                failures.put(comparisonOperator, Optional.empty());
            }

            if (isColumn(rightExpression)) {
                allColumns.add(getStringColumn((Column) rightExpression));
            }
            if (isColumn(leftExpression)) {
                allColumns.add(getStringColumn((Column) leftExpression));
            }
        }

        private Pair<String, String> getStringColumn(Column c) {
            Pair<Table, Column> column = Pair.of(getTable(c, tables), c);
            return Pair.of(column.getLeft().getName(), column.getRight().getColumnName());
        }

        private void checkInExpression(InExpression inExpression) {
            failures.put(inExpression, Optional.empty());
            if (isColumn(inExpression.getLeftExpression())) {
                allColumns.add(getStringColumn((Column) inExpression.getLeftExpression()));
            }
        }

        private void checkLikeExpression(LikeExpression likeExpression) {
            Expression rightExpression = likeExpression.getRightExpression();
            if (rightExpression instanceof StringValue && isLikePatternConstPartAnchoredToBeginning((StringValue) rightExpression)) {
                failures.put(likeExpression, Optional.empty());
            }
            if (isColumn(likeExpression.getLeftExpression())) {
                allColumns.add(getStringColumn((Column) likeExpression.getLeftExpression()));
            }
        }

        private static boolean isLikePatternConstPartAnchoredToBeginning(StringValue value) {
            return !value.getValue().startsWith("'%");
        }

        private static boolean isAND(ASTNodeAccess parent) {
            return parent instanceof AndExpression;
        }

        private Pair<Table, Column> getColumn(Expression operator) {
            if (operator instanceof ComparisonOperator) {
                Expression leftExpression = ((ComparisonOperator) operator).getLeftExpression();
                Expression rightExpression = ((ComparisonOperator) operator).getRightExpression();
                if (!isConstant(leftExpression) && !isConstant(rightExpression)) {
                    // скорее всего это join
                    // тест на foreign key DatabaseValidationTest#everyForeignKeyShouldHaveIndex
                    return null;
                } else {
                    Column column;
                    if (isColumn(leftExpression)) {
                        column = (Column) leftExpression;
                    } else if (isColumn(rightExpression)) {
                        column = (Column) rightExpression;
                    } else {
                        // закоментарь это исключение и создай тикет на поддержку теста индексации по выражению
                        throw new AssertionError("");
                    }
                    Table table = getTable(column, tables);

                    return Pair.of(table, column);
                }
            } else if (operator instanceof InExpression) {
                Column column = (Column) ((InExpression) operator).getLeftExpression();
                if (column == null) {
                    throw new UnsupportedOperationException("только колонка слева поддерживается");
                }
                Table table = getTable(column, tables);
                return Pair.of(table, column);
            } else if (operator instanceof LikeExpression) {
                Column column = (Column) ((LikeExpression) operator).getLeftExpression();
                Table table = getTable(column, tables);
                return Pair.of(table, column);
            } else {
                throw new AssertionError("bug");
            }
        }

        private boolean isExcluded(Column column, Table table) {
            if (table == null || table.getName() == null) {
                throw new IllegalArgumentException("table name must be non null");
            }
            String tableName = table.getName().toLowerCase();
            if (excludedTables.contains(tableName) || tableName.startsWith("pg_")) {
                return true;
            } else {
                String columnName = column.getColumnName().toLowerCase();
                if (excludedColumns.contains(Pair.of(tableName, columnName))) {
                    return true;
                } else if (excludedColumnsPrecise.stream().anyMatch(c -> c.getLeft().equals(Pair.of(tableName,
                        columnName)))) {
                    return excludedColumnsPrecise.stream()
                            .filter(c -> c.getLeft().equals(Pair.of(tableName, columnName)))
                            .anyMatch(c -> allColumns.containsAll(c.getRight()));
                }
            }
            return false;
        }

        private Table getTable(Column column, List<Table> tables) {
            Table columnTable = null;
            String tableAlias = null;
            if (column.getTable() != null) {
                // для колонок алиас неожиданно находится в name а не в alias
                tableAlias = column.getTable().getName();
                if (tableAlias != null) {
                    for (Table table : tables) {
                        if (table.getAlias() != null && StringUtils.equalsIgnoreCase(table.getAlias().getName(),
                                tableAlias) ||
                                (table.getName() != null && StringUtils.equalsIgnoreCase(table.getName(), tableAlias))) {
                            columnTable = table;
                            break;
                        }
                    }
                }
            }
            // не получилось через алиас установить чья колонка - пробуем через словарь
            // если колонка была указана через алиас таблицы то увязать ее правильно игнорируя то что таблицы
            // с нужным алиасом нет - нельзя. поэтому пропускаем
            if (columnTable == null && tableAlias == null) {
                for (Table table : tables) {
                    if (databaseMetadata.hasColumn(table.getName(), column.getColumnName())) {
                        if (columnTable == null) {
                            columnTable = table;
                        } else {
                            throw new IllegalStateException(String.format(
                                    "неоднозначность: колонка %s может принадлежать нескольким таблицам %s %s",
                                    column.getColumnName(), columnTable.getName(), table.getName()
                            ));
                        }
                    }
                }
            }
            // не удалось найти через словарь - считаем, если в списке одна таблица, что это ее колонка
            if (columnTable == null && tableAlias == null) {
                if (tables.size() == 1) {
                    columnTable = tables.get(0);
                }
            }

            if (columnTable == null) {
                throw new IllegalArgumentException("column " + column + " not found in tables " + tables);
            }

            return columnTable;
        }

        private static boolean isColumn(Expression expression) {
            return expression instanceof Column;
        }

        private static boolean isConstant(Expression expression) {
            return expression instanceof JdbcParameter ||
                    expression instanceof JdbcNamedParameter ||
                    expression instanceof DoubleValue ||
                    expression instanceof LongValue ||
                    expression instanceof DateValue ||
                    expression instanceof TimeValue ||
                    expression instanceof TimestampValue ||
                    expression instanceof StringValue;
        }
    }

    private static class ANDsIndex extends ExpressionVisitorAdapter {
        private Map<Expression, Expression> index;

        private ANDsIndex() {
            index = new HashMap<>();
        }

        @Override
        public void visit(AndExpression andExpression) {
            index.put(andExpression.getLeftExpression(), andExpression);
            index.put(andExpression.getRightExpression(), andExpression);
            super.visit(andExpression);
        }

        @Override
        public void visit(Parenthesis andExpression) {
            index.put(andExpression.getExpression(), andExpression);
            super.visit(andExpression);
        }

        public Expression getParentAND(Expression operator) {
            Expression current = index.get(operator);
            while (isParenthesis(current)) {
                current = index.get(current);
            }
            return current;
        }

        private static boolean isParenthesis(Expression current) {
            return current instanceof Parenthesis;
        }
    }
}
