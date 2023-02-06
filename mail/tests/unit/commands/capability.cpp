#include "command_test_base.h"

using namespace yimap;
using namespace yimap::backend;

struct CapabilityTest : CommandTestBase
{
    string commandName = "CAPABILITY"s;

    auto createCommand()
    {
        return CommandTestBase::createCommand(commandName, "");
    }

    auto createAndStartCommand()
    {
        auto command = createCommand();
        startCommand(command);
        return command->getFuture();
    }

    auto capabilityListBeginning()
    {
        return "* CAPABILITY"s;
    }

    auto capabilityOkResponse()
    {
        return commandTag() + " OK CAPABILITY Completed."s;
    }
};

TEST_F(CapabilityTest, createOK)
{
    auto command = createCommand();
    ASSERT_TRUE(command != nullptr);
}

TEST_F(CapabilityTest, finishedWithCapabilityResponse)
{
    createAndStartCommand();

    ASSERT_EQ(session->outgoingData.size(), 2);
    ASSERT_TRUE(beginsWith(session->outgoingData[0], capabilityListBeginning()))
        << session->outgoingData[0];
    ASSERT_TRUE(beginsWith(session->outgoingData[1], capabilityOkResponse()))
        << session->outgoingData[1];
}
