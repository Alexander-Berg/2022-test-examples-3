#pragma once

#include <yamail/data/serialization/json_writer.h>
#include <macs/mime_part.h>
#include <mail/hound/include/internal/wmi/yplatform/reflection/mimes.h>

namespace macs {

inline bool operator ==(const MimePart& lhs, const MimePart& rhs) {
    return lhs.hid() == rhs.hid()
            && lhs.contentType() == rhs.contentType()
            && lhs.contentSubtype() == rhs.contentSubtype()
            && lhs.boundary() == rhs.boundary()
            && lhs.name() == rhs.name()
            && lhs.charset() == rhs.charset()
            && lhs.encoding() == rhs.encoding()
            && lhs.contentDisposition() == rhs.contentDisposition()
            && lhs.fileName() == rhs.fileName()
            && lhs.cid() == rhs.cid()
            && lhs.offsetBegin() == rhs.offsetBegin()
            && lhs.offsetEnd() == rhs.offsetEnd();
}

} // namespace macs

namespace hound {

inline std::ostream& operator <<(std::ostream& stream, const Mimes& value) {
    return yamail::data::serialization::writeJson(stream, value);
}

inline bool operator ==(const RootMessagePart& lhs, const RootMessagePart& rhs) {
    return lhs.stid == rhs.stid && lhs.mimeParts == rhs.mimeParts;
}

inline bool operator ==(const StidHidMessagePart& lhs, const StidHidMessagePart& rhs) {
    return lhs.stidHid == rhs.stidHid && lhs.mimePart == rhs.mimePart;
}

inline bool operator ==(const MessageParts& lhs, const MessageParts& rhs) {
    return lhs.root == rhs.root && lhs.other == rhs.other;
}

} // namespace hound

