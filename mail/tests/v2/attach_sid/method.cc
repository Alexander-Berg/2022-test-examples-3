#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/v2/attach_sid/method.h>
#include "../helpers.h"

namespace {

using namespace hound::server::handlers::v2::attach_sid;
using namespace hound::testing;
using namespace mail_getter::part_id;
using namespace ::testing;

struct AttachSidPackerMock final {
    MOCK_METHOD(std::string, ProcessSinglePart, (SingleMessagePart), (const));
    MOCK_METHOD(std::string, ProcessMultiplePart, (MultipleMessagePart), (const));

    std::string operator()(SingleMessagePart partId) const {
        return ProcessSinglePart(std::move(partId));
    }
    std::string operator()(MultipleMessagePart partId) const {
        return ProcessMultiplePart(std::move(partId));
    }
};

struct AttachSidMethod : public TestWithParam<Request> {
    Method<AttachSidPackerMock> method{AttachSidPackerMock{}};
};

INSTANTIATE_TEST_SUITE_P(test_attachsid_all_incorrect_args, AttachSidMethod, ::testing::Values(
    Request{"",  "",  {}},
    Request{"1", "",  {}},
    Request{"",  "2", {}},
    Request{"1", "2", {}},
    Request{"",  "",  {"3"}},
    Request{"",  "2", {"3"}},
    Request{"1", "",  {"3"}}
));

TEST_P(AttachSidMethod, should_return_invalid_argument) {
    const auto req = GetParam();
    const auto res = method(req);
    EXPECT_EQ(res.error(), error_code{libwmi::error::invalidArgument});
}

TEST_F(AttachSidMethod, should_return_single_sid) {
    constexpr auto hid = "1.2";
    Request req{"uid", "mid", {hid}};
    const std::string securedId = "secured_id";
    EXPECT_CALL(method.attachShield, ProcessSinglePart).WillOnce(Return(securedId));

    const auto res = method(req);
    const auto r = res.value();

    EXPECT_TRUE(r.count(method.MULTIPLE_PART_KEY) < 1);
    EXPECT_TRUE(r.count(hid) == 1);

    EXPECT_EQ(r.at(hid), securedId);
    EXPECT_EQ(r.size(), req.hids.size());
}

TEST_F(AttachSidMethod, should_return_multiple_sid) {
    Request req{"uid", "mid", {"1.2", "1.3", "1.4"}};
    const std::string securedIdS = "secured_id_single";
    const std::string securedIdM = "secured_id_multiple";

    EXPECT_CALL(method.attachShield, ProcessSinglePart)
            .Times(Exactly(static_cast<int>(req.hids.size())))
            .WillRepeatedly(Return(securedIdS));

    EXPECT_CALL(method.attachShield, ProcessMultiplePart)
            .WillOnce(Return(securedIdM));

    const auto res = method(req);
    const auto r = res.value();

    EXPECT_EQ(r.size(), 1 + req.hids.size());

    EXPECT_TRUE(r.count(method.MULTIPLE_PART_KEY) == 1);
    EXPECT_EQ(r.at(method.MULTIPLE_PART_KEY), securedIdM);

    for (const auto& hid : req.hids) {
        EXPECT_TRUE(r.count(hid) == 1);
        EXPECT_EQ(r.at(hid), securedIdS);
    }
}

}
