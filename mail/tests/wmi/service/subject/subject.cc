#include <gtest/gtest.h>
#include <mail/hound/include/internal/wmi/service/subject/subject.h>

namespace {

class SubjectInfoTest : public ::testing::TestWithParam<std::string> { };

TEST_P(SubjectInfoTest, rename_me) {
    const std::string subj = GetParam();

    SubjectInfo si1(subj);
    EXPECT_TRUE(si1.isSplitted());
    EXPECT_EQ(si1.subject(), subj);

    SubjectInfo si2("Re: "+subj);
    EXPECT_TRUE(si2.isSplitted());
    EXPECT_EQ(si2.subject(), subj);
    EXPECT_EQ(si2.type(), "replied");
    EXPECT_EQ(si2.prefix(), "Re: ");

    SubjectInfo si3("FwD: "+subj);
    EXPECT_TRUE(si3.isSplitted());
    EXPECT_EQ(si3.subject(), subj);
    EXPECT_EQ(si3.type(), "forwarded");
    EXPECT_EQ(si3.prefix(), "FwD: ");

    SubjectInfo si4("re[8]: fwd: "+subj);
    EXPECT_TRUE(si4.isSplitted());
    EXPECT_EQ(si4.subject(), subj);
    EXPECT_EQ(si4.type(), "replied");
    EXPECT_EQ(si4.prefix(), "re[8]: fwd: ");

    SubjectInfo si5(subj+" (fwd)");
    EXPECT_TRUE(si5.isSplitted());
    EXPECT_EQ(si5.subject(), subj);
    EXPECT_EQ(si5.postfix(), " (fwd)");
}

INSTANTIATE_TEST_SUITE_P(TestSubjectInfoWithVariousLanguages, SubjectInfoTest, ::testing::Values(
        "subj",
        "Ё! Русская тема",
        "Türkçe tema",
        "Қазақ тақырыбы",
        "Українська тема",
        "Беларуская тэма",
        "\xF2\xD5\xD3\xD3\xCB\xC1\xD1\x20\xD4\xC5\xCD\xC1\x20\xD3\x20\x6B\x6F\x69\x38\x2D\x72", // "Русская тема с koi8-r"
        "\xD0\xF3\xF1\xF1\xEA\xE0\xFF\x20\xF2\xE5\xEC\xE0\x20\xF1\x20\x63\x70\x31\x32\x35\x31", // "Русская тема с cp1251"
        "\x43\x70\x31\x32\x35\x34\x20\x69\x6C\x65\x20\x74\xFC\x72\x6B\x20\x74\x65\x6D\x61" // "Cp1254 ile türk tema"

));


}
