#pragma once

#include <mail_getter/content_type.h>

namespace msg_body::testing {

struct ContentTypeDetectorMock : IContentTypeDetector {
    MimeType detectByFilename(const std::string&) const {
        return MimeType("type", "subtype");
    }
    MimeType detectByContent(const std::string&) const {
        return MimeType("type", "subtype");
    }
    MimeType detect(const std::string&, const std::string&) const {
        return MimeType("type", "subtype");
    }
};

} // namespace msg_body::testing
