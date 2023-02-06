#define CATCH_CONFIG_RUNNER
#include <generator/pg_conninfo_provider.h>
#include <yxiva/core/json.h>
#include <catch.hpp>
#include <fstream>
#include <sstream>

using std::vector;
using namespace yxiva::equalizer;
using namespace yxiva;

boost::asio::io_service t_selector_io;
string test_cases_filename;

class t_conninfo_provider : public pg_conninfo_provider
{
public:
    t_conninfo_provider()
        : pg_conninfo_provider(
              { "", "user", seconds(10) },
              t_selector_io,
              []() { /*conninfos updated*/ })
    {
    }

    void process_response(const string& data)
    {
        pg_conninfo_provider::process_response(data, context_ptr());
    }
};

TEST_CASE("pg_conninfo_provider/process_response", "bulk test")
{
    std::cout << "read tests from " << test_cases_filename << std::endl;
    std::fstream test_data_file(test_cases_filename);
    stringstream test_data_raw;
    test_data_raw << test_data_file.rdbuf();
    test_data_file.close();
    json_value test_data = json_parse(test_data_raw.str());

    for (auto itest = test_data.begin(); itest != test_data.end(); ++itest)
    {
        auto&& key = itest.key();
        auto&& test_case = *itest;
        INFO("testcase::[" + key + "]");
        t_conninfo_provider connprovider;
        connprovider.process_response(test_case["sharpei_response"].toStyledString());
        auto expected = test_case["expected"];
        if (expected.empty())
        {
            CHECK(connprovider.all_databases().empty());
        }
        else
        {
            for (auto idb = expected.begin(); idb != expected.end(); ++idb)
            {
                CHECK(
                    connprovider.conninfo(idb.key().to_string()) ==
                    (*idb)["connfinfo"].to_string());
                CHECK(connprovider.alias(idb.key().to_string()) == (*idb)["name"].to_string());
            }
        }
    }
}

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        std::cerr << "No test data file specified" << std::endl;
        return 1;
    }
    test_cases_filename = argv[1];
    Catch::Session session;
    // catch expects programm name at 0 index
    argv[1] = argv[0];
    if (auto ret = session.applyCommandLine(argc - 1, argv + 1)) return ret;
    return session.run();
}