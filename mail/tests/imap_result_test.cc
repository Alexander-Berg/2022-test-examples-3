#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <ymod_imapclient/imap_result.h>
#include <src/imap_result_parser.hpp>
#include <src/imap_result_debug.hpp>

static const std::string prefix = "=============================================";

using namespace ymod_imap_client;

std::vector<std::string> UntaggedProto = {
    "* 1 FETCH (UID 4865 FLAGS (\\Seen \\Recent))",
    "* 2 FETCH (UID 4868 FLAGS (\\Seen))",
    "* 3 FETCH (UID 4869 FLAGS (\\Seen \\Deleted))",
    "* 4 FETCH (UID 4901 FLAGS (\\Seen \\Answered))",
    "* 5 FETCH (UID 4901 FLAGS (\\Flagged \\Seen))",
    "* 6 FETCH (UID 4905 FLAGS (\\Seen))",
    "* 7 FETCH (UID 4906 FLAGS (\\Seen))",
};

TEST(TestImapResult, testImapResult_UID_FETCH)
{
    std::vector<std::string> untagged(UntaggedProto);
    std::string response = "";

    MessageSet messageSet(std::move(response), std::move(untagged));
    ImapResultParser<MessageSet> parser(messageSet);

    std::cerr << "Uids " << debugString(messageSet) << std::endl;
}
