#ifndef MACS_TESTS_COMPARE_ENVELOPES_H_
#define MACS_TESTS_COMPARE_ENVELOPES_H_

#include <macs/envelope.h>

namespace macs {
inline bool operator == (const AttachmentDescriptor& lhs, const AttachmentDescriptor& rhs) {
    return lhs.m_contentType == rhs.m_contentType && lhs.m_fileName == rhs.m_fileName
            && lhs.m_hid == rhs.m_hid && lhs.m_size == rhs.m_size;
}

#define EQ_(name) (lhs.name() == rhs.name())
template <typename T1, typename T2>
inline bool operator == (const EnvelopeDataInterface<T1>& lhs, const EnvelopeDataInterface<T2>& rhs) {
    return EQ_(mid) && EQ_(fid) && EQ_(threadId) && EQ_(revision)
        && EQ_(date) && EQ_(receiveDate) && EQ_(from) && EQ_(replyTo)
        && EQ_(subject) && EQ_(cc) && EQ_(bcc) && EQ_(to) && EQ_(uidl)
        && EQ_(imapId) && EQ_(stid) && EQ_(firstline) && EQ_(inReplyTo)
        && EQ_(references) && EQ_(rfcId) && EQ_(size) && EQ_(threadCount)
        && EQ_(extraData) && EQ_(newCount) && EQ_(attachments)
        && EQ_(labels) && EQ_(types);
}
#undef EQ_
}


#endif /* MACS_TESTS_COMPARE_ENVELOPES_H_ */
