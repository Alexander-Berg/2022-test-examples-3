#pragma once

#include <macs/mime_part_factory.h>

namespace macs {

inline std::ostream & operator << (std::ostream & s, const macs::MimePart& v) {
    s << v.hid() << ", " << v.boundary() << ", " << v.charset() << ", " <<  v.cid() << ", " <<
            v.contentDisposition() << ", " << v.contentType() << ", " <<  v.contentSubtype() <<
            ", " << v.encoding() << ", " << v.fileName() << ", " <<  v.name() << ", " <<
            v.offsetBegin() << ", " << v.offsetEnd();
    return s;
}

inline  bool operator == (const MimePart& l, const MimePart& r) {
    return l.hid() == r.hid() && l.boundary() == r.boundary() && l.charset() == r.charset() &&
            l.cid() == r.cid() && l.contentDisposition() == r.contentDisposition() &&
            l.contentType() == r.contentType() && l.contentSubtype() == r.contentSubtype() &&
            l.encoding() == r.encoding() && l.fileName() == r.fileName() && l.name() == r.name() &&
            l.offsetBegin() == r.offsetBegin() && l.offsetEnd() == r.offsetEnd();
}

}
