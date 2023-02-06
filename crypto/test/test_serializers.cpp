#include <crypta/styx/services/common/mutation_commands/mutation_command.pb.h>
#include <crypta/styx/services/common/mutation_commands/serializers.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <limits>

using namespace NCrypta::NStyx;

namespace {
    const ui64 PUID = std::numeric_limits<ui64>::max();
    const ui64 UNIXTIME = 100500u;
    const TString SERIALIZED_DELETE = R"({"Delete":{"Puid":18446744073709551615,"Unixtime":100500}})";
}

TEST(NMutationSerializers, DeleteToJson) {
    TMutationCommand command;
    auto* deleteCommand = command.MutableDelete();
    deleteCommand->SetPuid(PUID);
    deleteCommand->SetUnixtime(UNIXTIME);

    EXPECT_EQ(SERIALIZED_DELETE, NMutationSerializers::ToJson(command));
}

TEST(NMutationSerializers, JsonToDelete) {
    const auto& command = NMutationSerializers::FromJson(SERIALIZED_DELETE);
    const auto& deleteCommand = command.GetDelete();
    EXPECT_EQ(PUID, deleteCommand.GetPuid());
    EXPECT_EQ(UNIXTIME, deleteCommand.GetUnixtime());
}
