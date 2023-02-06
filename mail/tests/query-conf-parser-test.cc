#include <internal/query_conf/parser.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace pgg {
namespace query {

std::ostream & operator << ( std::ostream & s, const Traits::Milliseconds & m ) {
    return s << m.count() << "us";
}

std::ostream & operator << ( std::ostream & s, const Parameters & parameters ) {
    for( const auto & p : parameters ) {
        s << p.first << " = (";
        for( const auto & i : p.second ) {
            s << i.first << " : " << '"' << i.second << '"' << ", ";
        }
        s << ')' << std::endl;
    }
    return s;
}

} // namespace query
} // namespace pgg


namespace {

using namespace testing;
using namespace pgg::query;
using namespace pgg::query::conf;

using pgg::query::Parameters;

struct QueryConfParserTest : public Test {
    QueryConfParserTest() {}
};

TEST_F(QueryConfParserTest, parseConfig_empryString_returnsEmptyMap) {
    const std::string query = "";
    Entries res;
    parseConfig(query, res);
    const TraitsMap & out(res.queries);
    EXPECT_EQ(out.size(), 0ul);
}

TEST_F(QueryConfParserTest, parseConfig_withTwoQueries_returnsTwoEntriesWithNames) {
    const std::string query = "sql test;\n query \"\";sql test2;\n query \"\";";
    Entries res;
    parseConfig(query, res);
    const TraitsMap & out(res.queries);
    EXPECT_EQ(out.size(), 2ul);
    EXPECT_EQ(out.begin()->first, "test");
    EXPECT_EQ((++(out.begin()))->first, "test2");
}

TEST_F(QueryConfParserTest, parseConfig_withOptions_setsOptions) {
    const std::string query =
            "sql test; # Имя запроса - обязательный\n"
            "[rollback:false, debug:true, timeout:666, endpoint:replica];\n"
            "#Тело запроса\n"
            "query \"SELECT * \";";
    Entries res;
    parseConfig(query, res);
    const TraitsMap & out(res.queries);
    EXPECT_EQ(out.size(), 1ul);
    EXPECT_EQ(out.begin()->second.options.rollback, false);
    EXPECT_EQ(out.begin()->second.options.debug, true);
    EXPECT_EQ(out.begin()->second.options.timeout, Traits::Milliseconds(666));
    EXPECT_EQ(out.begin()->second.options.endpoint, Traits::EndpointType::replica);
}

TEST_F(QueryConfParserTest, parseConfig_withQueryBodyWithoutVariables_setsBody) {
    const std::string query = "sql test;\n query \"SELECT * \";";
    Entries res;
    parseConfig(query, res);
    const TraitsMap & out(res.queries);
    std::string s;
    out.begin()->second.body.stream(s);
    EXPECT_EQ(s, "SELECT * ");
}

TEST_F(QueryConfParserTest, parseConfig_withQueryBodyWithResolvedVariables_returnsTextWithVariableIndex) {
    const std::string query = "sql test;\n query \"SELECT * WHERE id=$var::int FROM table\";";
    Entries res;
    parseConfig(query, res);
    TraitsMap & out(res.queries);
    VariablesMap map;
    VariablesSet resolved;
    map["var"] = 1;
    out.begin()->second.body.resolve(map, resolved);
    std::string s;
    out.begin()->second.body.stream(s);
    EXPECT_EQ(s, "SELECT * WHERE id=$1::int FROM table");
}

template <typename T>
void printVector( const T & v ) {
    typedef typename T::const_iterator Iter;
    for( Iter i(v.begin()), last(v.end()); i!=last; ++i) {
        std::cout << '"' << i->text() << '"' << std::endl;
    }
}

TEST_F(QueryConfParserTest, do_withArgs_postcondition) {
    const std::string query =
            "sql mailbox_list_folder; # Имя запроса - обязательный\n"
            "[rollback:false, debug:true, timeout:1000]; #, endpoint:re];\n"
            "#Тело запроса\n"
            "query \"SELECT  m.mid, m.fid, m.tid, m.imap_id, m.revision, m.seen, m.recent, m.deleted,\n"
            "           m.st_id, ROUND(EXTRACT(EPOCH FROM m.received_date)) as received_date,\n"
            "           m.size, m.attach_count, m.attach_size, m.lids, me.types, me.attributes,\n"
            "           me.attaches, me.subject, me.firstline, ROUND(EXTRACT(EPOCH FROM me.hdr_date)) as hdr_date,\n"
            "           me.hdr_message_id, code.expand_rids(m.uid, me.recipients) as recipients, me.extra_data\n"
            "       FROM mail.mailbox m\n"
            "       LEFT JOIN mail.mailbox_extra me ON  m.mid = me.mid AND me.uid = m.uid\n"
            "       WHERE m.uid = $uid::int AND m.fid = $fid AND m.received_date BETWEEN $dateFrom AND $dateTo\n"
            "       ORDER BY $(order) DESC LIMIT $rowFrom-$rowTo OFFSET $rowFrom\";";
    Entries res;
    parseConfig(query, res);
    TraitsMap & out(res.queries);
    std::cout << "Body:" << std::endl;
    VariablesMap map;
    map["uid"] = 0;
    VariablesSet resolved;
    out.begin()->second.body.resolve(map, resolved);
    printVector(out.begin()->second.body);
    EXPECT_EQ(out.size(), 1ul);
}

TEST_F(QueryConfParserTest, parseConfig_withParameters_setsParameters) {
    const std::string query =
            "parameters\n"
            "    sortOrder = (asc:\"ASC\", desc:\"DESC\"),\n"
            "    sortField = (date:\"m.received_date\", subj:\"m.subject\", null:\"\");\n"
            "# Here queries begin\n"
            "sql mailbox_list_folder;\n"
            "query \"SELECT  * \n"
            "    FROM mail.mailbox m\n"
            "    WHERE m.uid = $uid AND m.fid = $fid AND m.received_date BETWEEN $dateFrom AND $dateTo\n"
            "    ORDER BY $(sortField) $(sortOrder) LIMIT $rowCount OFFSET $rowFrom\";";

    const Parameters parameters = {
            {"sortOrder", {{"asc","ASC"}, {"desc", "DESC"}} },
            {"sortField", {{"date","m.received_date"}, {"subj", "m.subject"}, {"null", ""} }}
    };

    Entries res;
    parseConfig(query, res);
    const Parameters & result = res.parameters;
    EXPECT_EQ(parameters, result);
}

} // namespace
