#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs_pg/service/factory.h>
#include <internal/query/query_register_impl.h>
#include "path_to_query_conf.h"

namespace {

using namespace testing;
using namespace macs::pg;

TEST(QueryConfTest, checkQueryConf) {
    readQueryConfFile(macs::pathToQueryConf());
}


} // namespace
