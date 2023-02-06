#include <gtest/gtest.h>

#include <mail_getter/vdirect/href_iterator.h>

namespace {

TEST(HrefIteratorTest, construct_withNoHrefString_equalEnd)
{
    const std::string href = "<a \"some.link\">AAAA</a>";
    ASSERT_TRUE(HrefIterator(href) == HrefIterator() );
    ASSERT_FALSE(HrefIterator(href) != HrefIterator() );
}

TEST(HrefIteratorTest, construct_withHrefString_notEqualEnd)
{
    const std::string href = "<a href=\"some.link\">AAAA</a>";
    ASSERT_TRUE(HrefIterator(href) != HrefIterator());
    ASSERT_FALSE(HrefIterator(href) == HrefIterator());
}

TEST(HrefIteratorTest, construct_withHrefString_returnsFirstPosRight)
{
    const std::string href = "<a href=\"some.link\">AAAA</a>";
    ASSERT_EQ(HrefIterator(href).first, 9ul);
}

TEST(HrefIteratorTest, construct_withHrefString_returnsLastPosRight)
{
    const std::string href = "<a href=\"some.link\">AAAA</a>";
    ASSERT_EQ(HrefIterator(href).last, 18ul);
}

TEST(HrefIteratorTest, construct_withHrefString_returnsHrefValue)
{
    const std::string href = "<a href=\"some.link\">AAAA</a>";
    const HrefIterator i(href);
    ASSERT_EQ(*i, "some.link");
}

TEST(HrefIteratorTest, construct_withHrefStringFor2ndLink_returnsFirstPosRight)
{
    const std::string href = "<a href=\"some.link\">AAAA</a> <a href=\"second.link\">AAAA</a>";
    ASSERT_EQ((++HrefIterator(href)).first, 38ul);
}

TEST(HrefIteratorTest, construct_withHrefStringFor2ndLink_returnsLastPosRight)
{
    const std::string href = "<a href=\"some.link\">AAAA</a> <a href=\"second.link\">AAAA</a>";
    ASSERT_EQ((++HrefIterator(href)).last, 49ul);
}

TEST(HrefIteratorTest, construct_withHrefStringFor2ndLink_returnsHrefValue)
{
    const std::string href = "<a href=\"some.link\">AAAA</a> <a href=\"second.link\">AAAA</a>";
    HrefIterator i(href);
    ASSERT_EQ(*++i, "second.link");
}

}
