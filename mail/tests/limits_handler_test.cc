#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/core/include/limits.h>

#include <mail/sendbernar/client/include/internal/result_reflection.h>

using namespace testing;

namespace sendbernar {
using boost::fusion::operators::operator==;

constexpr std::size_t maxRecipients = 2;
constexpr std::size_t attachmentsMaxSize = 100;
constexpr std::size_t messageMaxSize = 1000;
constexpr double limitsFactor = 0.5;
constexpr LimitsResponse::Config config{
    static_cast<std::size_t>(attachmentsMaxSize * limitsFactor),
    static_cast<std::size_t>(messageMaxSize * limitsFactor),
    maxRecipients
};
const LimitsResponse::DomainLimit yaWithoutLimits{
    "ya.ru",
    std::nullopt
};

LimitsResponse callTestFunction(const params::LimitsParams& params = {}, const SmtpLimits& l = {}) {
    return limits(params, l, maxRecipients, attachmentsMaxSize, messageMaxSize, limitsFactor).value();
}

TEST(LimitsResponse, shouldReturnConfigLimitsWithEmptyParams) {
    LimitsResponse response{config, std::nullopt};
    EXPECT_EQ(callTestFunction(), response);
}

TEST(LimitsResponse, shouldReturnErrorOnGarbageInToCcOrBccParam) {
    const auto testcase = [](const params::LimitsParams& params) {
        const auto response = limits(params, SmtpLimits(),
                                     maxRecipients, attachmentsMaxSize,
                                     messageMaxSize, limitsFactor).error();

        EXPECT_EQ(response.category(), sendbernar::getSendbernarCategory());
        EXPECT_EQ(response.value(), static_cast<int>(ErrorResult::invalidParam));
    };

    {
        params::LimitsParams params;
        params.recipients.to = "asdf";
        testcase(params);
    }

    {
        params::LimitsParams params;
        params.recipients.cc = "asdf";
        testcase(params);
    }

    {
        params::LimitsParams params;
        params.recipients.bcc = "asdf";
        testcase(params);
    }
}

TEST(LimitsResponse, shouldSetFlagInCaseOfTooManyRecipients) {
    params::LimitsParams params;

    LimitsResponse notSet{
        config, LimitsResponse::Domains{ false, { yaWithoutLimits } }
    };

    params.recipients.to = "a@ya.ru";

    EXPECT_EQ(callTestFunction(params), notSet);

    LimitsResponse set = notSet;
    set.domains->maxRecipientsReached = true;

    params.recipients.to = "a@ya.ru, b@ya.ru, c@ya.ru";
    EXPECT_EQ(callTestFunction(params), set);
}

TEST(LimitsResponse, shouldReturnOnlyUniqueDomains) {
    LimitsResponse resp{
        config, LimitsResponse::Domains{ true, { LimitsResponse::DomainLimit{ "mail.ru", std::nullopt }, yaWithoutLimits } }
    };

    params::LimitsParams params;
    params.recipients.to = "a@ya.ru, b@ya.ru, c@mail.ru";

    EXPECT_EQ(callTestFunction(params), resp);
}

TEST(LimitsResponse, shouldNotFailInCaseOfEmptyRecipients) {
    LimitsResponse resp{
        config, LimitsResponse::Domains{ false, { } }
    };

    params::LimitsParams params;
    params.recipients.to = " ";

    EXPECT_EQ(callTestFunction(params), resp);
}

TEST(LimitsResponse, shouldReturnSizeInCaseOfDomainInConfig) {
    const std::size_t smtplimit = 10;
    const auto mailruLimit = std::make_optional(static_cast<std::size_t>(smtplimit*limitsFactor));
    LimitsResponse resp{
        config, LimitsResponse::Domains{ true, { LimitsResponse::DomainLimit{ "mail.ru", mailruLimit }, yaWithoutLimits } }
    };

    params::LimitsParams params;
    params.recipients.to = "a@ya.ru, b@ya.ru, c@mail.ru";

    EXPECT_EQ(callTestFunction(params, SmtpLimits{{"mail.ru", smtplimit}}), resp);
}

}
