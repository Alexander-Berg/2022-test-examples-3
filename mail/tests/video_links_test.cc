#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/video_links.h>

namespace msg_body {

using namespace testing;

typedef Test VideoLinksTest;

TEST(VideoLinksTest, makeParamString_validParams_fullParamString) {
    const EmbedInfo link = {0, "url", "vars", 100, 100, "http://url", "YaVideo"};
    ASSERT_EQ("flashvars=vars&height=100&hosting_name=YaVideo&player_url=url&width=100", makeParamString(link));
}

TEST(VideoLinksTest, makeParamString_emptyParam_missParam) {
    const EmbedInfo link = {0, "url", "", 100, 100, "http://url", "YaVideo"};
    ASSERT_EQ("height=100&hosting_name=YaVideo&player_url=url&width=100", makeParamString(link));
}

TEST(VideoLinksTest, makeParamString_uriInParams_encodeUri) {
    const EmbedInfo link = {0, "http://www.youtube.com/v/onjt2L2lDEY", "vars", 100, 100, "", "YaVideo"};
    ASSERT_EQ("flashvars=vars&height=100&hosting_name=YaVideo&player_url=http%3A%2F%2Fwww.youtube.com%2Fv%2Fonjt2L2lDEY&width=100", makeParamString(link));
}

TEST(VideoLinksTest, getVideoParams_wellFormedParams_formParamStrings) {
    EmbedInfos links;
    const EmbedInfo link1 = {0, "url1", "vars1", 100, 100, "http://url1", "YaVideo"};
    links.push_back(link1);
    const EmbedInfo link2 = {1, "url2", "vars2", 200, 200, "http://url2", "YouTube"};
    links.push_back(link2);
    VideoParams params = getVideoParams(links);

    VideoParams expected;
    expected[0] = "flashvars=vars1&height=100&hosting_name=YaVideo&player_url=url1&width=100";
    expected[1] = "flashvars=vars2&height=200&hosting_name=YouTube&player_url=url2&width=200";
    ASSERT_TRUE(expected == params);
}

} // namespace msg_body
