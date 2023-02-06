#include <gtest/gtest.h>

#include <yplatform/encoding/base64.h>

#include "reflection/xyandexhint.h"

using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NReflection;

namespace {

std::string MakeHint(const std::string& data) {
    auto encoded = yplatform::base64_encode(data.begin(), data.end());
    return {encoded.begin(), encoded.end()};
}

} // namespace

TEST(TXYandexHint, Simple) {
    auto encodedHint = MakeHint(R"xyh(
filters=1
notify=0
source_stid=320.mail:0.E17:123
)xyh");
    auto hint = ParseXYHint(encodedHint);

    EXPECT_EQ(hint.filters, true);
    EXPECT_EQ(hint.notify, false);
    EXPECT_EQ(hint.source_stid, "320.mail:0.E17:123");
    EXPECT_TRUE(hint.email.empty());
}

TEST(TXYandexHint, InvalidFormat) {
    auto encodedHint = MakeHint("filters\nsave_to_sent=0\nskip_loop_prevention");
    auto hint = ParseXYHint(encodedHint);
    EXPECT_TRUE(hint.filters);
    EXPECT_FALSE(hint.skip_loop_prevention);
    ASSERT_TRUE(hint.save_to_sent);
    EXPECT_FALSE(*hint.save_to_sent);
}

TEST(TXYandexHint, InvalidBoolValues) {
    auto encodedHint = MakeHint("filters=aaaaa");
    auto hint = ParseXYHint(encodedHint);
    EXPECT_FALSE(hint.filters);

    encodedHint = MakeHint("filters=-1");
    hint = ParseXYHint(encodedHint);
    EXPECT_TRUE(hint.filters);

    encodedHint = MakeHint("filters=3");
    hint = ParseXYHint(encodedHint);
    EXPECT_TRUE(hint.filters);
}

TEST(TXYandexHint, InvalidUtf) {
    auto encodedHint = MakeHint("email=\x81\x93\xBE\n");
    auto hint = ParseXYHint(encodedHint);
    EXPECT_TRUE(hint.email.empty());

    encodedHint = MakeHint("email=valid\x81@\x93mail\xBE\n");
    hint = ParseXYHint(encodedHint);
    EXPECT_EQ(hint.email, "valid@mail");
}

bool operator==(const std::set<std::string>& lhs, const std::set<std::string>& rhs) {
    if (lhs.size() != rhs.size()) {
        return false;
    }

    return true;
}

TEST(TXYandexHint, Combine) {
    std::string first{R"xyh(
filters=0
notify=1
folder=\Spam
label=one
label=two
)xyh"};
    std::string second{R"xyh(
filters=1
notify=0
folder=\Inbox
label=three
label=two
)xyh"};

    auto one = ParseXYHint(MakeHint(first));
    auto two = ParseXYHint(MakeHint(second));

    TXYandexHint to = one;
    CombineXYHint(to, two);

    EXPECT_EQ(to.filters, one.filters);
    EXPECT_EQ(to.notify, two.notify);
    EXPECT_EQ(to.folder, one.folder);

    decltype(to.label) expected_labels{"one", "three", "two"};
    EXPECT_EQ(to.label, expected_labels);
}

TEST(TXYandexHint, ToString) {
    TXYandexHint hint;
    hint.filters = false;
    hint.fid = "1";
    hint.label = {"1", "2"};
    hint.save_to_sent = false;

    EXPECT_EQ(hint.ToString(), "fid=1 filters=0 label=1 label=2 save_to_sent=0");
}
