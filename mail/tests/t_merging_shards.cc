#include <yxiva/core/shards/storage.h>
#include <yxiva/core/resharding/migration_storage.h>
#include <yplatform/find.h>
#include <catch.hpp>

using namespace yxiva::resharding;
using namespace yxiva::shard_config;
using namespace yxiva;

static std::shared_ptr<storage> old_storage_mock;
static std::shared_ptr<storage> new_storage_mock;
static std::shared_ptr<migration_storage> migration_storage_mock;

namespace yplatform {

template <>
std::shared_ptr<storage> find<storage, std::shared_ptr>(const string& name)
{
    if (name == "old_shards")
    {
        return old_storage_mock;
    }
    if (name == "new_shards")
    {
        return new_storage_mock;
    }
    throw std::runtime_error("unexpected storage name " + name);
}

template <>
std::shared_ptr<migration_storage> find<migration_storage, std::shared_ptr>(const string& name)
{
    if (name == "migrations")
    {
        return migration_storage_mock;
    }
    throw std::runtime_error("unexpected migrations name " + name);
}

}

#include <yxiva/core/shards/merging_storage.h>

class mock_shard_storage : public storage
{
public:
    mock_shard_storage(shards shards) : shards_(std::make_shared<const ::shards>(std::move(shards)))
    {
    }

    std::shared_ptr<const ::shards> get() const override
    {
        return shards_;
    }

private:
    std::shared_ptr<const ::shards> shards_;
};

struct mock_migration_storage : public migration_storage
{
    std::vector<migration> migrations;

    std::shared_ptr<const std::vector<migration>> get() const override
    {
        return std::make_shared<std::vector<migration>>(migrations);
    }
};

namespace yxiva { namespace shard_config {

std::ostream& operator<<(std::ostream& os, const shard_config::shard& s)
{
    os << s.describe() << " " << s.master.conninfo;
    return os;
}

}}

std::shared_ptr<merging_storage> setup_merging_storage(
    const std::shared_ptr<storage>& old_storage,
    const std::shared_ptr<storage>& new_storage,
    const std::shared_ptr<migration_storage>& migration_storage)
{
    old_storage_mock = old_storage;
    new_storage_mock = new_storage;
    migration_storage_mock = migration_storage;

    yplatform::ptree config;
    yplatform::ptree resharding_config;
    resharding_config.put("migrations", "migrations");
    resharding_config.put("shards_from", "old_shards");
    resharding_config.put("shards_to", "new_shards");
    config.put_child("resharding", resharding_config);

    return std::make_shared<merging_storage>(config);
}

TEST_CASE("shards/merging_storage")
{
    auto old_storage = std::make_shared<mock_shard_storage>(
        mock_shard_storage{ { { 0, 0, 100, { "om0" }, { { { "or0" } } } },
                              { 1, 101, 200, { "om1" }, { { { "or1" } } } } } });
    auto old_shards = old_storage->get();

    auto new_storage = std::make_shared<mock_shard_storage>(
        mock_shard_storage{ { { 0, 0, 50, { "nm0" }, { { { "nr0" } } } },
                              { 1, 51, 100, { "nm1" }, { { { "nr1" } } } },
                              { 2, 101, 150, { "nm2" }, { { { "nr2" } } } },
                              { 3, 151, 200, { "nm3" }, { { { "nr3" } } } } } });
    auto new_shards = new_storage->get();

    auto migration_storage = std::make_shared<mock_migration_storage>();
    migration_storage->migrations =
        std::vector<migration>{ { migration::state_type::finished, 0, 75 },
                                { migration::state_type::inprogress, 76, 100 },
                                { migration::state_type::ready, 101, 125 },
                                { migration::state_type::pending, 126, 200 } };

    auto expected_shards = shards{ { 0, 0, 50, { "nm0" }, { { { "nr0" } } } },
                                   { 1, 51, 75, { "nm1" }, { { { "nr1" } } } },
                                   { 2, 76, 100, { "om0" }, { { { "or0" } } } },
                                   { 3, 101, 125, { "om1" }, { { { "or1" } } } },
                                   { 4, 126, 200, { "om1" }, { { { "or1" } } } } };

    auto test_subject = setup_merging_storage(old_storage, new_storage, migration_storage);
    auto result_shards = test_subject->get();

    REQUIRE(*result_shards == expected_shards);
}

TEST_CASE("shards/merging_storage/migration_edge_cases", "")
{
    auto old_storage = std::make_shared<mock_shard_storage>(
        mock_shard_storage{ { { 0, 0, 100, { "om0" }, { { { "or0" } } } } } });

    auto new_storage = std::make_shared<mock_shard_storage>(
        mock_shard_storage{ { { 1, 0, 100, { "nm1" }, { { { "nr1" } } } } } });

    auto migration_storage = std::make_shared<mock_migration_storage>();

    auto test_subject = setup_merging_storage(old_storage, new_storage, migration_storage);

    SECTION("single gid wide migration range")
    {
        migration_storage->migrations = { { migration::state_type::pending, 0, 25 },
                                          { migration::state_type::finished, 26, 26 },
                                          { migration::state_type::pending, 27, 100 } };
        auto result_shards = test_subject->get();
        auto expected_shards = shards{ { 0, 0, 25, { "om0" }, { { { "or0" } } } },
                                       { 1, 26, 26, { "nm1" }, { { { "nr1" } } } },
                                       { 2, 27, 100, { "om0" }, { { { "or0" } } } } };
        REQUIRE(*result_shards == expected_shards);
    }

    SECTION("single gid wide migration range left edge")
    {
        migration_storage->migrations = { { migration::state_type::finished, 0, 0 },
                                          { migration::state_type::pending, 1, 100 } };
        auto result_shards = test_subject->get();
        auto expected_shards = shards{ { 0, 0, 0, { "nm1" }, { { { "nr1" } } } },
                                       { 1, 1, 100, { "om0" }, { { { "or0" } } } } };
        REQUIRE(*result_shards == expected_shards);
    }

    SECTION("single gid wide migration range right edge")
    {
        migration_storage->migrations = { { migration::state_type::pending, 0, 99 },
                                          { migration::state_type::finished, 100, 100 } };
        auto result_shards = test_subject->get();
        auto expected_shards = shards{ { 0, 0, 99, { "om0" }, { { { "or0" } } } },
                                       { 1, 100, 100, { "nm1" }, { { { "nr1" } } } } };
        REQUIRE(*result_shards == expected_shards);
    }

    SECTION("migration range sits inside mapping range or touches one edge")
    {
        migration_storage->migrations = { { migration::state_type::pending, 0, 25 },
                                          { migration::state_type::finished, 26, 35 },
                                          { migration::state_type::pending, 36, 100 } };
        auto result_shards = test_subject->get();
        auto expected_shards = shards{ { 0, 0, 25, { "om0" }, { { { "or0" } } } },
                                       { 1, 26, 35, { "nm1" }, { { { "nr1" } } } },
                                       { 2, 36, 100, { "om0" }, { { { "or0" } } } } };
        REQUIRE(*result_shards == expected_shards);
    }

    SECTION("migration range covers exactly one mapping range")
    {
        migration_storage->migrations = { { migration::state_type::finished, 0, 100 } };
        auto result_shards = test_subject->get();
        auto expected_shards = shards{ { 0, 0, 100, { "nm1" }, { { { "nr1" } } } } };
        REQUIRE(*result_shards == expected_shards);
    }

    SECTION("migration range covers multiple mapping ranges")
    {
        old_storage = std::make_shared<mock_shard_storage>(
            mock_shard_storage{ { { 0, 0, 100, { "om0" }, { { { "or0" } } } },
                                  { 1, 101, 200, { "om1" }, { { { "or1" } } } },
                                  { 2, 201, 300, { "om2" }, { { { "or2" } } } } } });
        auto new_storage = std::make_shared<mock_shard_storage>(
            mock_shard_storage{ { { 3, 0, 100, { "nm3" }, { { { "nr3" } } } },
                                  { 4, 101, 200, { "nm4" }, { { { "nr4" } } } },
                                  { 5, 201, 300, { "nm5" }, { { { "nr5" } } } } } });
        auto migration_storage = std::make_shared<mock_migration_storage>();
        auto test_subject = setup_merging_storage(old_storage, new_storage, migration_storage);

        migration_storage->migrations = { { migration::state_type::pending, 0, 25 },
                                          { migration::state_type::finished, 26, 235 },
                                          { migration::state_type::pending, 236, 300 } };
        auto result_shards = test_subject->get();
        auto expected_shards = shards{
            { 0, 0, 25, { "om0" }, { { { "or0" } } } },
            { 1, 26, 100, { "nm3" }, { { { "nr3" } } } },
            { 2, 101, 200, { "nm4" }, { { { "nr4" } } } },
            { 3, 201, 235, { "nm5" }, { { { "nr5" } } } },
            { 4, 236, 300, { "om2" }, { { { "or2" } } } },
        };
    }

    SECTION("migration storage returns an empty vector")
    {
        auto result_shards = test_subject->get();
        REQUIRE(result_shards->empty());
    }
}
