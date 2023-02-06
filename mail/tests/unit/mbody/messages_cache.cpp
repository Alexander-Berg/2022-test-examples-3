#include <backend/mbody/storage/messages_cache.h>
#include <gtest/gtest.h>

using namespace yimap;
using namespace yimap::mbody;

const string STID = "123";
const string SECOND_STID = "456";
const string THIRD_STID = "789";
const string MESSAGE = "Subject: Hello\r\n\r\nHello world!";

StringPtr get(MessagesCache& cache, const string& stid)
{
    return cache.get(stid, 0, MESSAGE.length());
}

void put(MessagesCache& cache, const string& stid)
{
    cache.put(stid, 0, MESSAGE.length(), MESSAGE);
}

TEST(MESSAGES_CACHE, GetNotExisting)
{
    MessagesCache cache(MESSAGE.length());
    EXPECT_EQ(get(cache, STID), nullptr);
}

TEST(MESSAGES_CACHE, Put)
{
    MessagesCache cache(MESSAGE.length());
    EXPECT_NO_THROW(put(cache, STID));
}

TEST(MESSAGES_CACHE, IgnoreTooLarge)
{
    MessagesCache cache(MESSAGE.length() / 2);
    EXPECT_NO_THROW(put(cache, STID));
    EXPECT_EQ(get(cache, STID), nullptr);
}

TEST(MESSAGES_CACHE, Get)
{
    MessagesCache cache(MESSAGE.length());
    put(cache, STID);
    auto data = get(cache, STID);
    ASSERT_NE(data, nullptr);
    EXPECT_EQ(*data, MESSAGE);
}

TEST(MESSAGES_CACHE, Remove)
{
    MessagesCache cache(MESSAGE.length());
    put(cache, STID);
    put(cache, SECOND_STID);
    EXPECT_EQ(get(cache, STID), nullptr);
    EXPECT_NE(get(cache, SECOND_STID), nullptr);
}

TEST(MESSAGES_CACHE, DontRemoveRecentlyUsed)
{
    MessagesCache cache(2 * MESSAGE.length());
    put(cache, STID);
    put(cache, SECOND_STID);
    get(cache, STID);
    put(cache, THIRD_STID);
    EXPECT_NE(get(cache, STID), nullptr);
    EXPECT_EQ(get(cache, SECOND_STID), nullptr);
}
