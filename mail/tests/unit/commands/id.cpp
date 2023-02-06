#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct IDTest : CommandTestBase
{
    string commandName = "ID"s;
    string testClientId = R"(("name" "test-client" "version" "2.0.2.0" "vendor" "RTEC"))";

    auto createCommand()
    {
        return CommandTestBase::createCommand(commandName, testClientId);
    }

    auto createAndStartCommand()
    {
        auto command = createCommand();
        startCommand(command);
        return command->getFuture();
    }

    auto serverIDBeginning()
    {
        return "* ID (\"name\" \"Yandex Mail\" \"vendor\" \"Yandex\""s;
    }

    auto idOkResponse()
    {
        return commandTag() + " OK ID Completed."s;
    }
};

TEST_F(IDTest, createOK)
{
    auto command = createCommand();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(IDTest, finishedWithIDResponse)
{
    createAndStartCommand();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], serverIDBeginning()))
        << session->outgoingData[0];
    ASSERT_TRUE(beginsWith(session->outgoingData[1], idOkResponse())) << session->outgoingData[1];
}
