#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/collie/unistat/cpp/meters.h>

using namespace ::testing;
using namespace ::unistat;
using namespace ::unistat::detail;


static const std::string STRANGE_CODE   = getStatusCode("very strange code");
static const std::string STRANGE_METHOD = getMethod("very strange method");


class StrangeStatusCodeTestFixture : public TestWithParam<std::string> {
};


TEST_P(StrangeStatusCodeTestFixture, shouldReturnStrangeCode) {
    const std::string code = GetParam();
    EXPECT_EQ(getStatusCode(code), STRANGE_CODE);
}

INSTANTIATE_TEST_SUITE_P(
        GetStatusCodeTests,
        StrangeStatusCodeTestFixture,
        Values(
            "",
            "12",
            "000",
            "010",
            "654",
            "9999",
            "20000",
            "020000",
            "3000000"
        )
);



class CorrectStatusCodeTestFixture : public TestWithParam<std::string> {
};


TEST_P(CorrectStatusCodeTestFixture, shouldReturnCorrectCode) {
    const std::string code = GetParam();
    EXPECT_EQ(getStatusCode(code), code);
}

INSTANTIATE_TEST_SUITE_P(
        GetStatusCodeTests,
        CorrectStatusCodeTestFixture,
        Values(
            "100",
            "200",
            "201",
            "302",
            "312",
            "400",
            "404",
            "500",
            "501",
            "599"
        )
);



class StrangeMethodTestFixture : public TestWithParam<std::string> {
};


TEST_P(StrangeMethodTestFixture, shouldReturnStrangeMethod) {
    const std::string method = GetParam();
    EXPECT_EQ(getMethod(method), STRANGE_METHOD);
}

INSTANTIATE_TEST_SUITE_P(
        GetMethodTests,
        StrangeMethodTestFixture,
        Values(
            "",
            "geeet",
            "method?",
            "pet",
            "ppppppost",
            "GoT",
            "Deleted!",
            "patches",
            "3000000"
        )
);



class CorrectMethodTestFixture : public TestWithParam<std::string> {
};


TEST_P(CorrectMethodTestFixture, shouldReturnCorrectMethod) {
    std::string method = GetParam();
    std::string res = getMethod(method);
    boost::to_upper(method);
    EXPECT_EQ(res, method);
}

INSTANTIATE_TEST_SUITE_P(
        GetMethodTests,
        CorrectMethodTestFixture,
        Values(
            "GeT", "HEaD", "pOST", "PUT", "DELETe", "cONNECT", "OPTiONs", "TRACE", "PaTCh"
        )
);



TEST(CountByPath, shouldThrowInvalidRegex) {
    EXPECT_THROW(CountByPath("(:?", "", ""), std::invalid_argument);
}

TEST(CountByPath, shouldThrowEmptyCapturingGroups) {
    EXPECT_THROW(CountByPath(".*", "", ""), std::invalid_argument);
}



TEST(CountByPath, shouldUpdateCorrectly) {
    const std::string prefix = "pref";
    const std::string suffix = "suff";
    const std::string first = "first_signal";
    const std::string second = "2nd_sig";
    const std::string third = "tretii_vot";
    const std::string namedRe = fmt::format("(?:(?P<{}>v1)|(?P<{}>v2)|(?P<{}>v3))", first, second, third);
    const std::string signalFmt = "{}_{}_{}_{}_{}";
    const auto signalName = [&](const std::string& path, const std::string& method, const std::string& status) {
        return fmt::format(signalFmt, prefix, path, method, status, suffix);
    };
    CountByPath counter(namedRe, prefix, suffix);
    
    using NV = NamedValue<std::size_t>;

    counter.update({{"request", "that doesn't match"}});
    counter.update({{"that doesn't match", "request"}});

    EXPECT_TRUE(counter.get().empty());

    const std::string firstSigWithStrangeMethodAndCode = signalName(first, STRANGE_METHOD, STRANGE_CODE);
    const std::string thirdSigWithStrangeMethodAndCode = signalName(third, STRANGE_METHOD, STRANGE_CODE);
    const std::string secondSigWithStrangeMethodAndCode = signalName(second, STRANGE_METHOD, STRANGE_CODE);

    counter.update({{"request", "v1"}});
    EXPECT_THAT(counter.get(), UnorderedElementsAreArray({
        NV{firstSigWithStrangeMethodAndCode, 1},
    }));


    counter.update({{"request", "v3"}});
    EXPECT_THAT(counter.get(), UnorderedElementsAreArray({
        NV{firstSigWithStrangeMethodAndCode, 1},

        NV{thirdSigWithStrangeMethodAndCode, 1},
    }));


    counter.update({{"request", "v2"}});
    EXPECT_THAT(counter.get(), UnorderedElementsAreArray({
        NV{firstSigWithStrangeMethodAndCode, 1},
        NV{thirdSigWithStrangeMethodAndCode, 1},

        NV{secondSigWithStrangeMethodAndCode, 1},
    }));


    counter.update({{"request", "v1"}});
    EXPECT_THAT(counter.get(), UnorderedElementsAreArray({
        NV{thirdSigWithStrangeMethodAndCode, 1},
        NV{secondSigWithStrangeMethodAndCode, 1},

        NV{firstSigWithStrangeMethodAndCode, 2},
    }));


    const std::string firstSigWithCorrectMethodAndStrangeCode = signalName(first, "patch", STRANGE_CODE);

    counter.update({{"request", "v1"}, {"method", "patch"}});
    EXPECT_THAT(counter.get(), UnorderedElementsAreArray({
        NV{firstSigWithStrangeMethodAndCode, 2},
        NV{thirdSigWithStrangeMethodAndCode, 1},
        NV{secondSigWithStrangeMethodAndCode, 1},

        NV{firstSigWithCorrectMethodAndStrangeCode, 1},
    }));

    const std::string thirdSigWithStrangeMethodAndCorrectCode = signalName(third,  STRANGE_METHOD, "234");

    counter.update({{"request", "v3"}, {"status_code", "234"}});
    EXPECT_THAT(counter.get(), UnorderedElementsAreArray({
        NV{firstSigWithCorrectMethodAndStrangeCode, 1},
        NV{firstSigWithStrangeMethodAndCode, 2},
        NV{thirdSigWithStrangeMethodAndCode, 1},
        NV{secondSigWithStrangeMethodAndCode, 1},

        NV{thirdSigWithStrangeMethodAndCorrectCode, 1},
    }));
}
