#include <mail/sharpei/tests/mocks.h>

#include <gmock/gmock.h>
#include <gtest/gtest.h>

namespace {

using namespace sharpei;
using namespace sharpei::db;

ConfigPtr makeConfigWithHostlist(const std::vector<Host>& hostlist) {
    auto configPtree = makeTestConfigAsPtree();
    auto& child = configPtree.get_child("meta_connection.endpoint_provider");
    child.erase("hostlist");
    assert(child.count("hostlist") == 0u);
    addHostlist(configPtree, hostlist);
    return makeConfig(configPtree);
}

TEST(ConfigDeserializationTest, empty_hostlist_is_not_allowed) {
    const std::vector<Host> hostlist;
    ASSERT_THROW(makeConfigWithHostlist(hostlist), std::logic_error);
}

TEST(ConfigDeserializationTest, single_host_deserialization) {
    const std::vector<Host> hostlist = {{"localhost1", DC::sas}};
    auto config = makeConfigWithHostlist(hostlist);
    ASSERT_EQ(config->meta.endpointProvider.hostlist, hostlist);
}

TEST(ConfigDeserializationTest, multihost_deserialization) {
    const std::vector<Host> hostlist = {{"localhost1", DC::sas}, {"localhost2", DC::sas}, {"localhost3", DC::sas}};
    auto config = makeConfigWithHostlist(hostlist);
    std::sort(config->meta.endpointProvider.hostlist.begin(), config->meta.endpointProvider.hostlist.end());
    ASSERT_EQ(config->meta.endpointProvider.hostlist, hostlist);
}

TEST(ConfigDeserializationTest, multihost_with_dc) {
    const std::vector<Host> hostlist = {{"localhost1", DC::iva},
                                        {"localhost2", DC::man},
                                        {"localhost3", DC::myt},
                                        {"localhost4", DC::sas},
                                        {"localhost5", DC::vla}};
    auto config = makeConfigWithHostlist(hostlist);
    std::sort(config->meta.endpointProvider.hostlist.begin(), config->meta.endpointProvider.hostlist.end());
    ASSERT_EQ(config->meta.endpointProvider.hostlist, hostlist);
}

}  // namespace
