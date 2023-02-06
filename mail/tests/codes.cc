#include <iostream>
#include <ymod_webserver/codes.h>

#define FAILED_MESSAGE                                                                             \
    {                                                                                              \
        std::cerr << "[FAILED] " << __FILE__ << ":" << __LINE__ << "\n";                           \
        return 1;                                                                                  \
    }

#define TEST_EQUAL(code, correct)                                                                  \
    if (std::string(correct) != ymod_webserver::codes::reason::get(code)) FAILED_MESSAGE

int main()
{
    // check for crash
    for (int i = 0; i < 4000; ++i)
    {
        ymod_webserver::codes::reason::get(i);
    }
    // check for unknown
    TEST_EQUAL(13, "Unknown")
    TEST_EQUAL(927, "Unknown")
    TEST_EQUAL(ymod_webserver::codes::switching_protocols, "Switching")
    TEST_EQUAL(ymod_webserver::codes::partial_content, "PartialContent")
    TEST_EQUAL(ymod_webserver::codes::temporary_redirect, "TemporaryRedirect")
    TEST_EQUAL(ymod_webserver::codes::expectation_failed, "ExpectationFailed")
    TEST_EQUAL(ymod_webserver::codes::version_not_supported, "VersionNotSupported")
    TEST_EQUAL(ymod_webserver::codes::forbidden, "Forbidden")
    return 0;
}
