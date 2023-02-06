#pragma once

#include <mail/sendbernar/composer/include/sanitizer.h>

namespace sendbernar::tests {

struct MockSanitizer: public Sanitizer {
    MOCK_METHOD(std::optional<mail_getter::SanitizerParsedResponse>, sanitize, (std::string, const std::string&), (override));
};

}
