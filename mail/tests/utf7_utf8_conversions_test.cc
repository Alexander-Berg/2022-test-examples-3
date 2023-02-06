#include <ymod_imapclient/config.h>

#include <gtest/gtest.h>

#include <vector>
#include <string>

using namespace ymod_imap_client;

TEST(Utf8Utf7Converter, UTF7_TO_UTF8)
{
    std::vector<std::string> utf7Imap = {
        "[Gmail]/&BB4EQgQ,BEAEMAQyBDsENQQ9BD0ESwQ1-",
        "&BD8EQwRCBDUESAQ1BEEEQgQyBDgETw-",
    };

    std::vector<std::string> utf8Imap = {
        "[Gmail]/Отправленные",
        "путешествия",
    };

    EXPECT_EQ(utf7Imap.size(), utf8Imap.size());

    for (size_t i = 0; i < utf7Imap.size(); ++i)
    {
        EXPECT_EQ(folderNameFromUtf7Imap(utf7Imap[i], '/'), utf8Imap[i]);
        EXPECT_EQ(folderNameToUtf7Imap(utf8Imap[i], '/'), utf7Imap[i]);
    }
}
