package ru.yandex.market.common.test.jdbc;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;

import ru.yandex.market.common.test.transformer.CompositeStringTransformer;
import ru.yandex.market.common.test.transformer.PatternStringTransformer;
import ru.yandex.market.common.test.transformer.StringTransformer;

/**
 * Преобразует sql-запрос из Оракл-диалекта в H2-совместмый диалект.
 * <p>Умеет заменять конструкции вида
 * {@code in (select value(t) from table(cast(? as t_number_tbl) t)} на синтаксис H2:
 * {@code in (select t from table(t varchar = ?)}.
 * <p>Ожидает, что все {@link Supplier} параметры будут заменены
 * на результат метода {@link Supplier#get()}.
 * <p>См. <a href='http://h2database.com/html/functions.html#table'>документацию H2</a>.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class H2SqlTransformer extends CompositeStringTransformer {

    private static final Map<String, String> REPLACEMENTS = ImmutableMap.<String, String>builder()
            .put("trunc\\(", "PUBLIC.my_trunc(")
            .put(
                    // https://www.h2database.com/html/functions-aggregate.html#listagg
                    // language=regexp
                    "listagg\\((?<v1>.+?(?=,\\s*'[^']*'\\)\\s+within\\s+group)),\\s*(?<v2>'[^']*?')\\)" +
                            "\\s+within\\s+group\\s*\\(\\s*order\\s+by\\s+(?<v3>\\S*)\\)",
                    "group_concat(${v1} order by ${v3} separator ${v2})"
            )
            .put(("row_number\\(\\)\\s+over\\s+\\([^)]+\\)"), "rownum()")
            .put("\\(\\s*select\\s+" +                                                      // select
                            "(/\\*\\+\\s*cardinality\\(t\\s+[0-9]+\\)\\s*\\*/\\s*)?" +      // /*+ cardinality(t 1) */
                            "value\\s*\\((?<id>\\w+)\\)\\s+" +                              // value(id)
                            "(?<alias>as\\s+\\w+\\s+)?" +                                   // as alias
                            "from\\s+table\\s*" +                                           // from table
                            "\\(\\s*cast\\s*\\(\\s*(?<param>(:\\w+|\\?))\\s+as\\s+" +       // (cast(? as
                            "((?<schema>\\w+)\\.)*(?<type>\\w+)" +                          // schema.t_number_tbl
                            "\\s*\\)\\s*\\)" +                                              // ))
                            "\\s+\\k<id>" +                                                 // id
                            "\\s*\\)",
                    "(select ${id} ${alias} from table(${id} ${type} = ${param}))")
            .put("t_number_tbl", "bigint")
            .put("t_varchar_tab", "varchar")
            .put("(shops_web.)?ntt_varchar2", "varchar")
            .put("full outer join", "left join") // h2 не поддерживает full outer join
            .put("for update nowait", "") // h2 не поддерживает nowait и запрещает for update для view
            .put("for update skip locked", "") // h2 не поддерживает skip locked
            .put("[^\\s]*\\.?rowid", "_rowid_") // скорее заглушка, если есть идеи альтернативы, то можно заменить
            .put("keep \\(dense_rank [^(^)]+(\\([^)]+\\))?[^)]*\\)", "")
            .put("INTERVAL\\s+'(?<v1>\\d+)'\\s+DAY", "${v1}")
            .build();

    @Override
    protected void customizeTransformers(List<StringTransformer> transformers) {
        REPLACEMENTS.entrySet().stream()
                .map(e -> new PatternStringTransformer(e.getKey(), e.getValue()))
                .forEach(transformers::add);
    }

}
