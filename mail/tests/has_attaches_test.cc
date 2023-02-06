#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/composer/include/has_attaches.h>

using namespace testing;

namespace sendbernar {

struct WithNarodAttach {
    std::string narod;
    const std::string& narodAttach() const {
        return narod;
    }
};

params::SendMessage get(const params::Attaches& attaches) {
    return params::SendMessage {
        .attaches=attaches
    };
}

const RemoteAttachments empty;
const RemoteAttachments notEmpty = {
    RemoteAttachment{}
};

boost::optional<std::vector<std::string>> notEmptyArray = std::vector<std::string>{"1"};
boost::optional<params::Parts> partsJson = std::vector<params::IdetifiableMessagePart>{params::IdetifiableMessagePart("mid", "hid")};

TEST(HasAttachesTest, shouldReturnFalseIfThereIsNoAttaches) {
    EXPECT_FALSE(HasAttaches(get(params::Attaches{}), WithNarodAttach{}).hasAttaches(empty));
}

TEST(HasAttachesTest, shouldReturnTrueIfThereAreMids) {
    EXPECT_TRUE(HasAttaches(get(params::Attaches{ .forward_mids=notEmptyArray }), WithNarodAttach{}).hasAttaches(empty));
}

TEST(HasAttachesTest, shouldReturnTrueIfThereAreStids) {
    EXPECT_TRUE(HasAttaches(get(params::Attaches{ .uploaded_attach_stids=notEmptyArray }), WithNarodAttach{}).hasAttaches(empty));
}

TEST(HasAttachesTest, shouldReturnTrueIfThereArePartsJson) {
    EXPECT_TRUE(HasAttaches(get(params::Attaches{ .parts_json=partsJson }), WithNarodAttach{}).hasAttaches(empty));
}

TEST(HasAttachesTest, shouldReturnTrueIfThereAreRemoteAttaches) {
    EXPECT_TRUE(HasAttaches(get(params::Attaches{}), WithNarodAttach{}).hasAttaches(notEmpty));
}

TEST(HasAttachesTest, shouldReturnTrueIfThereAreNarodAttaches) {
    EXPECT_TRUE(HasAttaches(get(params::Attaches{}), WithNarodAttach{.narod="narod"}).hasAttaches(empty));
}

}
