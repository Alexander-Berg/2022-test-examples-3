#ifndef MAIL_QUOTE_TEST_BASE_VERIFIED_VISITOR_H
#define MAIL_QUOTE_TEST_BASE_VERIFIED_VISITOR_H

#include "depth_verifier.h"

namespace msg_body {
namespace mail_quote {

class BaseVerifiedVisitor {
public:
    BaseVerifiedVisitor(const bool debug) : debug(debug) {}

protected:
    void beforeVisit(const std::string& function) {
        if (debug) {
            std::cout << "before visit " << function << std::endl;
        }
        ++depth;
    }

    void afterLeave(const std::string& function) {
        --depth;
        if (debug) {
            std::cout << "after leave " << function << std::endl;
        }
    }

private:
    const bool debug;
    DepthVerifier depth;
};

} // namespace mail_quote
} // namespace msg_body

#endif // MAIL_QUOTE_TEST_BASE_VERIFIED_VISITOR_H
