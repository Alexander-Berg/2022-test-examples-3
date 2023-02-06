package ru.yandex.market.core.database;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OracleToPostgresTransformerTest {
    private final SqlTransformers.Transformer transformer = SqlTransformers.ora2pg();

    static Stream<Arguments> parameters() {
        return Stream.of(
                arguments(
                        "select 1 from dual",
                        "select 1 from dual"
                ),
                arguments(
                        "select 1 from dual where x in ?",
                        "select 1 from dual where x in ?"
                ),
                arguments(
                        "select listagg(reason, ',') within group (order by reason) reasons, " +
                                "listagg(coalesce(subreason, ' '), ',') within group (order by subreason) subreasons, " +
                                "listagg(coalesce(problem_type_id, ?), ',') within group (order by " +
                                "problem_type_id) " +
                                "row_number() over (order by datasource_id ASC) " +
                                "from table where id = ?",
                        "select string_agg(reason, ',' order by reason) reasons," +
                                " string_agg(coalesce(subreason, ' ')," +
                                " ','" +
                                " order by subreason) subreasons," +
                                " string_agg(coalesce(problem_type_id, ?), ',' order by problem_type_id)" +
                                " row_number() over (order by datasource_id ASC)" +
                                " from table where id = ?"
                ),
                arguments(
                        "Mysequence.nextval",
                        "nextval('Mysequence')"
                ),
                arguments(
                        "SCHEMA.SEQUENCE.nextval",
                        "nextval('SCHEMA.SEQUENCE')"
                ),
                arguments(
                        "select 1 from dual where x in (select value(t) from table (CAST(? as shops_web.t_number_tbl)" +
                                ") t)",
                        "select 1 from dual where x = any(?)"
                ),
                arguments(
                        "select 1 from dual where x in (select value(t) from table (CAST(? as t_number_tbl)) t)",
                        "select 1 from dual where x = any(?)"
                ),
                arguments(
                        "select 1 from dual where x in (select /*+ cardinality(t 1) */ value(t) from table (CAST(? as" +
                                " shops_web.t_number_tbl)) t)",
                        "select 1 from dual where x = any(?)"
                ),
                arguments(
                        "select 1 from dual where x in (select /*+ cardinality(t 1) */ value(t) from table (CAST(? as" +
                                " vendors.t_number_tbl)) t)",
                        "select 1 from dual where x = any(?)"
                ),
                arguments(
                        "select 1 from dual where x in (select /*+ cardinality(t 1) */ value(t) from table (CAST(? as" +
                                " t_number_tbl)) t)",
                        "select 1 from dual where x = any(?)"
                ),
                arguments(
                        "select 1 from dual where x in (select value(t) from table (CAST(? as shops_web.NTT_VARCHAR2)" +
                                ") t)",
                        "select 1 from dual where x = any(?)"
                ),
                arguments(
                        "update shops_web.bank_info\n" +
                                "set archived = 1\n" +
                                "where bic in (\n" +
                                "select /*+ cardinality(t 1)*/ value(t) from table(cast(? as shops_web.ntt_varchar2))" +
                                " t)\n" +
                                "and archived = 0\n",
                        "update shops_web.bank_info\n" +
                                "set archived = 1\n" +
                                "where bic = any(?)\n" +
                                "and archived = 0\n"
                ),
                arguments(
                        "SELECT * FROM shops_web.tanker_text WHERE keyset IN (" +
                                "SELECT /*+ cardinality(t 1)*/ VALUE(t) FROM TABLE(CAST(? AS " +
                                "shops_web.T_VARCHAR_TAB)) t) ",
                        "SELECT * FROM shops_web.tanker_text WHERE keyset = any(?) "
                ),
                arguments(
                        "INSERT INTO market_billing.delivery_balance_order(   shop_id,   balance_order_id,   " +
                                "balance_person_id,   bic,   account_number,   contact_name,   contact_email,   " +
                                "contact_phone,   post_code,   inn,   kpp,   start_date,   contract_type,   " +
                                "seller_client_id,   contract_id)VALUES(    :shop_id,    " +
                                "market_billing.s_delivery_balance_order.nextval,    :balance_person_id,    " +
                                ":bic,    :account_number,    :contact_name,    :contact_email,    :contact_phone,   " +
                                " " +
                                ":post_code,    :inn,    :kpp,    :start_date,    :contract_type,    " +
                                ":seller_client_id,     :contract_id)",
                        "INSERT INTO market_billing.delivery_balance_order( shop_id, balance_order_id, " +
                                "balance_person_id, bic, account_number, contact_name, contact_email, " +
                                "contact_phone, post_code, inn, kpp, start_date, contract_type, seller_client_id, " +
                                "contract_id)VALUES( :shop_id, nextval('market_billing.s_delivery_balance_order'), " +
                                ":balance_person_id, :bic, :account_number, :contact_name, :contact_email, " +
                                ":contact_phone, :post_code, :inn, :kpp, :start_date, :contract_type, " +
                                ":seller_client_id, " +
                                ":contract_id)"),
                arguments(
                        "rownum rn",
                        "row_number() over() as rn"
                ),
                arguments(
                        "partner_id in (              select value(t) from table(cast(? as shops_web.t_number_tbl)) t          )",
                        "partner_id = any(?)"
                ),
                arguments(
                        // minimal valid example
                        "in(select value(t) from table(cast(? as shops_web.t_number_tbl))t)",
                        "= any(?)"
                ),
                arguments(
                        // minimal valid example with comment
                        "in(select/*whatever*/value(t) from table(cast(? as shops_web.t_number_tbl))t)",
                        "= any(?)"
                ),
                arguments(
                        // minimal valid example with comment
                        "from(select value(t)as some_alias from table(cast(? as shops_web.t_number_tbl))t)",
                        "from unnest(?) as some_alias"
                ),
                arguments(
                        // minimal valid example with comment
                        "from(select/*whatever*/value(t)as some_alias from table(cast(? as shops_web.t_number_tbl))t)",
                        "from unnest(?) as some_alias"
                ),
                arguments(
                        "select sysdate from dual",
                        "select current_timestamp from dual"
                ),
                arguments(
                        "rowid",
                        "ctid"
                ),
                arguments(
                        "cast(var as varchar2(4))",
                        "cast(var as varchar(4))"
                ),
                arguments(
                        "cast(var as varchar2(4 char))",
                        "cast(var as varchar(4))"
                ),
                arguments(
                        "varchar2(4)",
                        "varchar(4)"
                ),
                arguments(
                        "varchar2(4 char)",
                        "varchar(4)"
                ),
                arguments(
                        "nvarchar2(4)",
                        "varchar(4)"
                ),
                arguments(
                        "nvarchar2(4 char)",
                        "varchar(4)"
                ),
                arguments(
                        "rownum as n",
                        "row_number() over() as n"
                ),
                arguments(
                        "with /*pg:recursive*/ parent(id,parent_region_id) as (...)",
                        "with recursive parent(id,parent_region_id) as (...)"
                ),
                arguments(
                        "in (select column_value from table(shops_web.split(var.regions)))",
                        "= any(shops_web.split(var.regions))"
                ),
                arguments(
                        "cast(collect(num_value) as shops_web.t_number_tbl)",
                        "array_agg(num_value)"
                ),
                arguments(
                        "cast(collect(shops_web.t_cutoff(coc.id, coc.datasource_id, coc.type, coc.from_time, null, " +
                                "coc.\"COMMENT\", \"\")) as shops_web.t_cutoff_tbl)",
                        "array_agg(shops_web.t_cutoff(coc.id, coc.datasource_id, coc.type, coc.from_time, null, " +
                                "coc.\"COMMENT\", \"\"))"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void transforms(String query, String expectedResult) {
        String result = transformer.transform(query);
        assertThat(transformer.transform(result)).as("трансформер идемпотентный").isEqualTo(result);
        assertThat(result.replaceAll(" +", " ")).isEqualTo(expectedResult); // минус пробелы чтобы проще сравнивать
    }

    @Test
    void returnsEmptyQueryAsIs() {
        assertThat(transformer.transform("")).isEmpty();
    }

    @Test
    void throwsExceptionIfQueryIsNull() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> transformer.transform(null))
                .withMessage("input sql should be not null");
    }

    @Test
    void skipsTransformIfSpecified() {
        var query = "rowid";
        assertThat(transformer.transform(query))
                .as("ensure that we chosen query to be transformed")
                .isEqualTo("ctid");
        assertThat(transformer.transform(SqlTransformers.SKIP_TRANSFORM_PREFIX + query))
                .isEqualTo(SqlTransformers.SKIP_TRANSFORM_PREFIX + query);
    }
}
