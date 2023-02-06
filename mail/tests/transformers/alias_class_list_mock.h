#pragma once

#include <mail_getter/alias_class_list.h>

namespace msg_body::testing {

struct AliasClassListMock : AliasClassList {
    const std::string& getAlias(const std::string& ext, const MimeType&) const {
        return ext;
    }
    bool canBeThumbnailed(const MimeType&) const {
        return false;
    };
    bool canBeBrowsed(const MimeType&) const {
        return false;
    };
};

} // namespace msg_body::testing
