#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/barbet/service/include/helpers.h>


using namespace ::testing;

namespace barbet::tests {

struct isMulcaStidFalseTest : TestWithParam<std::string_view> {
};

INSTANTIATE_TEST_SUITE_P(stids, isMulcaStidFalseTest, Values(
    "",
    "without dots",
    "with.single",
    "320.mds.stid",
    "with.colon.Eeee:rock"
    "with.multilple.dots.and.E:because it's simple checker"
    "there are other (>2) checkers in arcadia but they are poorly written"
    "you can't reuse them, so, here we are",

    "1000016.valid.E763:40279059181369915969536533942",
    "320.valid.E763:40279059181369915969536533942",
    "320.valid.40279059181369915969536533942",
    "1000016.valid.E763:INVALID",
    "INVALID.valid.E763:40279059181369915969536533942",
    "1000016.not-valid.E763:40279059181369915969536533942",
    "1000016.not.valid.E763:40279059181369915969536533942",
    "1000016.valid.EINVALID:40279059181369915969536533942"
));

TEST_P(isMulcaStidFalseTest, shouldReturnFalse) {
    EXPECT_FALSE(helpers::isMulcaStid(GetParam()));
}


struct isMulcaStidTrueTest : TestWithParam<std::string_view> {
};

INSTANTIATE_TEST_SUITE_P(stids, isMulcaStidTrueTest, Values(
    "2739.yadisk:uploader.97099719030826319143612335895",
    "1000016.yadisk:valid.4027905918136991596953653",
    "1000016.yadisk:valid.INVALID4027905918136991596953653",
    "1000016.valid.40279059181369915969536533942",
    "1000016.valid.INVALID40279059181369915969536533942",
    "3200000000.valid.40279059181369915969536533942",

    "We do not check . characters between . dots",
    "and don't . check allowed chars . ",
    "we check that starts from '320.'. ",
    "or presence of E{couple-id}: . in the last . part"
));

TEST_P(isMulcaStidTrueTest, shouldReturnTrue) {
    EXPECT_TRUE(helpers::isMulcaStid(GetParam()));
}


}
