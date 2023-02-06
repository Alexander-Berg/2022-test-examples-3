#include <common/helpers/utf7imap.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

using yimap::folderNameToUtf7Imap;
using yimap::folderNameFromUtf7Imap;

TEST(HELPERS, Utf7Imap_from_utf8)
{
    EXPECT_EQ(folderNameToUtf7Imap("Спам", '|'), "&BCEEPwQwBDw-");
    EXPECT_EQ(folderNameToUtf7Imap("Удаленные", '|'), "&BCMENAQwBDsENQQ9BD0ESwQ1-");
    EXPECT_EQ(folderNameToUtf7Imap("Черновики", '|'), "&BCcENQRABD0EPgQyBDgEOgQ4-");
    EXPECT_EQ(folderNameToUtf7Imap("Отправленные", '|'), "&BB4EQgQ,BEAEMAQyBDsENQQ9BD0ESwQ1-");
    EXPECT_EQ(folderNameToUtf7Imap("Исходящие", '|'), "&BBgEQQRFBD4ENARPBEkEOAQ1-");

    EXPECT_EQ(folderNameToUtf7Imap("предок1", '|'), "&BD8EQAQ1BDQEPgQ6-1");
    EXPECT_EQ(
        folderNameToUtf7Imap("предок1|потомок2", '|'),
        "&BD8EQAQ1BDQEPgQ6-1|&BD8EPgRCBD4EPAQ+BDo-2");

    EXPECT_EQ(folderNameToUtf7Imap("предок1", '/'), "&BD8EQAQ1BDQEPgQ6-1");
    EXPECT_EQ(
        folderNameToUtf7Imap("предок1/потомок2", '/'),
        "&BD8EQAQ1BDQEPgQ6-1/&BD8EPgRCBD4EPAQ+BDo-2");
}

TEST(HELPERS, Utf7Imap_to_utf8)
{
    EXPECT_EQ("Спам", folderNameFromUtf7Imap("&BCEEPwQwBDw-", '|'));
    EXPECT_EQ("Удаленные", folderNameFromUtf7Imap("&BCMENAQwBDsENQQ9BD0ESwQ1-", '|'));
    EXPECT_EQ("Черновики", folderNameFromUtf7Imap("&BCcENQRABD0EPgQyBDgEOgQ4-", '|'));
    EXPECT_EQ("Отправленные", folderNameFromUtf7Imap("&BB4EQgQ,BEAEMAQyBDsENQQ9BD0ESwQ1-", '|'));
    EXPECT_EQ("Исходящие", folderNameFromUtf7Imap("&BBgEQQRFBD4ENARPBEkEOAQ1-", '|'));

    EXPECT_EQ("предок1", folderNameFromUtf7Imap("&BD8EQAQ1BDQEPgQ6-1", '|'));
    EXPECT_EQ(
        "предок1|потомок2",
        folderNameFromUtf7Imap("&BD8EQAQ1BDQEPgQ6-1|&BD8EPgRCBD4EPAQ+BDo-2", '|'));

    EXPECT_EQ("предок1", folderNameFromUtf7Imap("&BD8EQAQ1BDQEPgQ6-1", '/'));
    EXPECT_EQ(
        "предок1/потомок2",
        folderNameFromUtf7Imap("&BD8EQAQ1BDQEPgQ6-1/&BD8EPgRCBD4EPAQ+BDo-2", '/'));

    EXPECT_THROW(folderNameFromUtf7Imap("&1BD8EQAQ1BDQEPgQ6-1", '|'), yimap::Utf7EncodingError);
    EXPECT_THROW(
        folderNameFromUtf7Imap("&BD8EQAQ1BDQEPgQ6-1|&1BD8EPgRCBD4EPAQ+BDo-2", '/'),
        yimap::Utf7EncodingError);
}
