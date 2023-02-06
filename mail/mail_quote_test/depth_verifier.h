#ifndef MAIL_QUOTE_TEST_DEPTH_VERIFIER_H
#define MAIL_QUOTE_TEST_DEPTH_VERIFIER_H

#include <gtest/gtest.h>

namespace msg_body {
namespace mail_quote {

class DepthVerifier {
public:
    ~DepthVerifier() {
        EXPECT_EQ(value, 0);
    }

    DepthVerifier& operator ++() {
        ++value;
        return *this;
    }

    DepthVerifier& operator --() {
        --value;
        return *this;
    }

private:
    long long value = 0;
};

} // namespace mail_quote
} // namespace msg_body

#endif // MAIL_QUOTE_DEPTH_VERIFIER_H
