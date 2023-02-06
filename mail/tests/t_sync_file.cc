#include <postprocessor/position/sync_file.h>
#include <catch.hpp>
#include <vector>

using std::vector;
using std::string;
using namespace yxiva::equalizer;

#define PATH "./"

class t_sync_file
{
public:
    t_sync_file() : syncer(PATH, 0)
    {
    }

    sync_file<int> syncer;
};

TEST_CASE_METHOD(t_sync_file, "position_holder/sync_file/1", "")
{
    string name("r1");
    syncer.sync(name, 10101);
    REQUIRE(10101 == syncer.restore(name));
    syncer.sync(name, 1);
    REQUIRE(1 == syncer.restore(name));
    syncer.sync(name, 2);
    REQUIRE(2 == syncer.restore(name));
    std::cout << syncer.restore(name) << std::endl;
    syncer.sync(name, 2020202);
    REQUIRE(2020202 == syncer.restore(name));
}
