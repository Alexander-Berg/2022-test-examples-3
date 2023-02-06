#include <crypta/lib/native/proto_secrets/remove_secrets.h>
#include <crypta/lib/native/proto_secrets/test/proto/secrets.pb.h>
#include <crypta/lib/native/yaml/yaml2proto.h>

#include <google/protobuf/text_format.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta;

namespace {
    const TString& PROTO_WITH_SECRETS = "Root {\n"
                                        "  Login: \"root\"\n"
                                        "  Password: \"password_root\"\n"
                                        "  Description: \"root!\"\n"
                                        "  Token: \"token\"\n"
                                        "}\n"
                                        "Users {\n"
                                        "  Login: \"user1\"\n"
                                        "  Password: \"password_1\"\n"
                                        "  Description: \"number one\"\n"
                                        "  Token: \"token\"\n"
                                        "}\n"
                                        "Users {\n"
                                        "  Login: \"user2\"\n"
                                        "  Password: \"password_2\"\n"
                                        "}\n";

    const TString& PROTO_WO_SECRETS = "Root {\n"
                                      "  Login: \"root\"\n"
                                      "  Password: \"<secret>\"\n"
                                      "  Description: \"root!\"\n"
                                      "  Token: \"<secret>\"\n"
                                      "}\n"
                                      "Users {\n"
                                      "  Login: \"user1\"\n"
                                      "  Password: \"<secret>\"\n"
                                      "  Description: \"number one\"\n"
                                      "  Token: \"<secret>\"\n"
                                      "}\n"
                                      "Users {\n"
                                      "  Login: \"user2\"\n"
                                      "  Password: \"<secret>\"\n"
                                      "}\n";
}

TEST(NProtoSecrets, RemoveSecrets) {
    NProtoSecrets::TSecrets secrets;
    NProtoBuf::TextFormat::ParseFromString(PROTO_WITH_SECRETS, &secrets);

    NProtoSecrets::RemoveSecrets(secrets);

    EXPECT_EQ(PROTO_WO_SECRETS, secrets.DebugString());
}

TEST(NProtoSecrets, GetCopyWithoutSecrets) {
    NProtoSecrets::TSecrets secrets;
    NProtoBuf::TextFormat::ParseFromString(PROTO_WITH_SECRETS, &secrets);

    EXPECT_EQ(PROTO_WO_SECRETS, NProtoSecrets::GetCopyWithoutSecrets(secrets).DebugString());
}
