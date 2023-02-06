#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <boost/algorithm/string/join.hpp>
#include <pgg/service/uid_resolver.h>

namespace {

using namespace testing;
using namespace pgg;
using namespace pgg::query;
using namespace sharpei::client;

struct MockSharpeiClient : SharpeiClient {
    using AsyncHandler = SharpeiClient::AsyncHandler;
    using AsyncMapHandler = SharpeiClient::AsyncMapHandler;

    MOCK_METHOD(void, asyncGetConnInfo, (const ResolveParams&, AsyncHandler), (const, override));
    MOCK_METHOD(void, asyncGetDeletedConnInfo, (const ResolveParams&, AsyncHandler), (const, override));
    MOCK_METHOD(void, asyncGetOrgConnInfo, (const ResolveParams&, AsyncHandler), (const, override));
    MOCK_METHOD(void, asyncStat, (AsyncMapHandler), (const, override));
    MOCK_METHOD(void, asyncStatById, (const Shard::Id&, AsyncHandler), (const, override));
};

struct MockUidResolver : UidResolver {
    MOCK_METHOD(void, asyncGetConnInfo, (const UidResolveParams&, OnResolve), (const, override));
    MOCK_METHOD(void, asyncGetShardName, (const UidResolveParams&, OnShardName), (const, override));
};

const boost::system::error_code ok;
const boost::system::error_code someError = boost::system::errc::make_error_code(boost::system::errc::host_unreachable);

struct SharpeiUidResolverTest : Test {
    const Credentials credentials;
    const std::shared_ptr<MockSharpeiClient> client;
    const Shard aliveShard;
    const Shard deadShard;
    const Shard semiDeadReplicasShard;

    SharpeiUidResolverTest()
        : credentials {"user", "password"},
          client(std::make_shared<MockSharpeiClient>()),
          aliveShard {
              "1",
              "xdb1",
              {
                  Shard::Database {
                      Shard::Database::Address {"xdb1a.mail.yandex.net", 5432, "maildb", "a"},
                      "master",
                      "alive",
                      Shard::Database::State {0ul}
                  },
                  Shard::Database {
                      Shard::Database::Address {"xdb1b.mail.yandex.net", 5432, "maildb", "b"},
                      "replica",
                      "alive",
                      Shard::Database::State {0ul}
                  },
                  Shard::Database {
                      Shard::Database::Address {"xdb1c.mail.yandex.net", 5432, "maildb", "c"},
                      "replica",
                      "alive",
                      Shard::Database::State {13ul}
                  },
              },
          },
          deadShard {
              "2",
              "xdb2",
              {
                  Shard::Database {
                      Shard::Database::Address {"xdb2a.mail.yandex.net", 5432, "maildb", "a"},
                      "master",
                      "dead",
                      Shard::Database::State {0ul}
                  },
                  Shard::Database {
                      Shard::Database::Address {"xdb2b.mail.yandex.net", 5432, "maildb", "b"},
                      "replica",
                      "dead",
                      Shard::Database::State {2000ul}
                  },
                  Shard::Database {
                      Shard::Database::Address {"xdb2c.mail.yandex.net", 5432, "maildb", "c"},
                      "replica",
                      "dead",
                      Shard::Database::State {0ul}
                  },
              },
          },
          semiDeadReplicasShard {
              "3",
              "xdb3",
              {
                  Shard::Database {
                      Shard::Database::Address {"xdb3b.mail.yandex.net", 5432, "maildb", "b"},
                      "replica",
                      "alive",
                      Shard::Database::State {0ul}
                  },
                  Shard::Database {
                      Shard::Database::Address {"xdb3c.mail.yandex.net", 5432, "maildb", "c"},
                      "replica",
                      "dead",
                      Shard::Database::State {0ul}
                  },
              },
          } {}

    UidResolverPtr resolver(UidResolver::UserType userType = UidResolver::UserType::existing) const {
        return createSharpeiUidResolver(credentials, client, userType);
    }
};

TEST_F(SharpeiUidResolverTest, request_master_should_return_one_master_conninfo) {
    EXPECT_CALL(*client, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, aliveShard));
    resolver()->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::master),
        [] (pgg::error_code error, ConnStrings connStrs) {
            EXPECT_FALSE(error);
            const ConnStrings expected = {
                "host=xdb1a.mail.yandex.net port=5432 dbname=maildb user=user password=password"
            };
            EXPECT_EQ(connStrs, expected);
        });
}

TEST_F(SharpeiUidResolverTest, request_not_master_should_return_replicas_conninfo) {
    EXPECT_CALL(*client, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, aliveShard));
    resolver()->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::replica),
        [] (pgg::error_code error, ConnStrings connStrs) {
            EXPECT_FALSE(error);
            const ConnStrings expected = {
                "host=xdb1b.mail.yandex.net port=5432 dbname=maildb user=user password=password",
                "host=xdb1c.mail.yandex.net port=5432 dbname=maildb user=user password=password"
            };
            EXPECT_EQ(connStrs, expected);
        });
}

TEST_F(SharpeiUidResolverTest, request_master_from_shard_with_dead_master_should_return_error) {
    EXPECT_CALL(*client, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, deadShard));
    resolver()->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::master),
        [] (pgg::error_code error, ConnStrings) {
            EXPECT_TRUE(error);
        });
}

TEST_F(SharpeiUidResolverTest, request_not_master_from_shard_with_dead_replicas_should_return_error) {
    EXPECT_CALL(*client, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, deadShard));
    resolver()->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::replica),
        [] (pgg::error_code error, ConnStrings) {
            EXPECT_TRUE(error);
        });
}

TEST_F(SharpeiUidResolverTest, request_not_master_from_shard_with_partially_dead_replicas_should_return_alive_replicas_conninfo) {
    EXPECT_CALL(*client, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, semiDeadReplicasShard));
    resolver()->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::replica),
        [] (pgg::error_code error, ConnStrings connStrs) {
            EXPECT_FALSE(error);
            const ConnStrings expected = {
                "host=xdb3b.mail.yandex.net port=5432 dbname=maildb user=user password=password"
            };
            EXPECT_EQ(connStrs, expected);
        });
}

TEST_F(SharpeiUidResolverTest, deleted_request_master_should_return_one_master_conninfo) {
    EXPECT_CALL(*client, asyncGetDeletedConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, aliveShard));
    resolver(UidResolver::UserType::deleted)->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::master),
        [] (pgg::error_code error, ConnStrings connStrs) {
            EXPECT_FALSE(error);
            const ConnStrings expected = {
                "host=xdb1a.mail.yandex.net port=5432 dbname=maildb user=user password=password"
            };
            EXPECT_EQ(connStrs, expected);
        });
}

TEST_F(SharpeiUidResolverTest, deleted_request_not_master_should_return_replicas_conninfo) {
    EXPECT_CALL(*client, asyncGetDeletedConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, aliveShard));
    resolver(UidResolver::UserType::deleted)->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::replica),
        [] (pgg::error_code error, ConnStrings connStrs) {
            EXPECT_FALSE(error);
            const ConnStrings expected = {
                "host=xdb1b.mail.yandex.net port=5432 dbname=maildb user=user password=password",
                "host=xdb1c.mail.yandex.net port=5432 dbname=maildb user=user password=password"
            };
            EXPECT_EQ(connStrs, expected);
        });
}

TEST_F(SharpeiUidResolverTest, deleted_request_master_from_shard_with_dead_master_should_return_error) {
    EXPECT_CALL(*client, asyncGetDeletedConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, deadShard));
    resolver(UidResolver::UserType::deleted)->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::master),
        [] (pgg::error_code error, ConnStrings) {
            EXPECT_TRUE(error);
        });
}

TEST_F(SharpeiUidResolverTest, deleted_request_not_master_from_shard_with_dead_replicas_should_return_error) {
    EXPECT_CALL(*client, asyncGetDeletedConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, deadShard));
    resolver(UidResolver::UserType::deleted)->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::replica),
        [] (pgg::error_code error, ConnStrings) {
            EXPECT_TRUE(error);
        });
}

TEST_F(SharpeiUidResolverTest, deleted_request_not_master_from_shard_with_partially_dead_replicas_should_return_alive_replicas_conninfo) {
    EXPECT_CALL(*client, asyncGetDeletedConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, semiDeadReplicasShard));
    resolver(UidResolver::UserType::deleted)->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::replica),
        [] (pgg::error_code error, ConnStrings connStrs) {
            EXPECT_FALSE(error);
            const ConnStrings expected = {
                "host=xdb3b.mail.yandex.net port=5432 dbname=maildb user=user password=password"
            };
            EXPECT_EQ(connStrs, expected);
        });
}

TEST_F(SharpeiUidResolverTest, org_request_master_should_return_one_master_conninfo) {
    EXPECT_CALL(*client, asyncGetOrgConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, aliveShard));
    resolver(UidResolver::UserType::organization)->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::master),
        [] (pgg::error_code error, ConnStrings connStrs) {
            EXPECT_FALSE(error);
            const ConnStrings expected = {
                "host=xdb1a.mail.yandex.net port=5432 dbname=maildb user=user password=password"
            };
            EXPECT_EQ(connStrs, expected);
        });
}

TEST_F(SharpeiUidResolverTest, org_request_not_master_should_return_replicas_conninfo) {
    EXPECT_CALL(*client, asyncGetOrgConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, aliveShard));
    resolver(UidResolver::UserType::organization)->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::replica),
        [] (pgg::error_code error, ConnStrings connStrs) {
            EXPECT_FALSE(error);
            const ConnStrings expected = {
                "host=xdb1b.mail.yandex.net port=5432 dbname=maildb user=user password=password",
                "host=xdb1c.mail.yandex.net port=5432 dbname=maildb user=user password=password"
            };
            EXPECT_EQ(connStrs, expected);
        });
}

TEST_F(SharpeiUidResolverTest, org_request_master_from_shard_with_dead_master_should_return_error) {
    EXPECT_CALL(*client, asyncGetOrgConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, deadShard));
    resolver(UidResolver::UserType::organization)->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::master),
        [] (pgg::error_code error, ConnStrings) {
            EXPECT_TRUE(error);
        });
}

TEST_F(SharpeiUidResolverTest, org_request_not_master_from_shard_with_dead_replicas_should_return_error) {
    EXPECT_CALL(*client, asyncGetOrgConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, deadShard));
    resolver(UidResolver::UserType::organization)->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::replica),
        [] (pgg::error_code error, ConnStrings) {
            EXPECT_TRUE(error);
        });
}

TEST_F(SharpeiUidResolverTest, org_request_not_master_from_shard_with_partially_dead_replicas_should_return_alive_replicas_conninfo) {
    EXPECT_CALL(*client, asyncGetOrgConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ok, semiDeadReplicasShard));
    resolver(UidResolver::UserType::organization)->asyncGetConnInfo(UidResolveParams("uid").endpointType(Traits::EndpointType::replica),
        [] (pgg::error_code error, ConnStrings connStrs) {
            EXPECT_FALSE(error);
            const ConnStrings expected = {
                "host=xdb3b.mail.yandex.net port=5432 dbname=maildb user=user password=password"
            };
            EXPECT_EQ(connStrs, expected);
        });
}

} // namespace
