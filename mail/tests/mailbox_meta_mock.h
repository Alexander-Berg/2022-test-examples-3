#pragma once

#include <gmock/gmock.h>
#include <mailbox_oper/mailbox_meta.h>

namespace macs {
inline std::ostream& operator<<(std::ostream& str, const Envelope& e) {
    str << "{" << e.mid()
        << ";" << e.fid()
        << ";" << e.threadId()
        << ";" << e.revision()
        << ";" << e.date()
        << ";" << e.receiveDate()
        << ";" << e.from()
        << ";" << e.replyTo()
        << ";" << e.subject()
        << ";" << e.cc()
        << ";" << e.bcc()
        << ";" << e.to()
        << ";" << e.uidl()
        << ";" << e.imapId()
        << ";" << e.stid()
        << ";" << e.firstline()
        << ";" << e.inReplyTo()
        << ";" << e.references()
        << ";" << e.rfcId()
        << ";" << e.size()
        << ";" << e.threadCount()
        << ";" << e.extraData()
        << ";" << e.newCount()
    ;
    str << "[";
    for (auto a: e.attachments()) {
        str << "{" << a.m_hid
            << ";" << a.m_contentType
            << ";" << a.m_fileName
            << ";" << a.m_size
            << "},";
    }
    str << "];[";
    for (auto l: e.labels()) {
        str << l << ",";
    }
    str << "];[";
    for (auto t: e.types()) {
        str << t << ",";
    }
    str << "]}";
    return str;
}
}

namespace mbox_oper {

class MailboxMetaMock : public MailboxMeta {
public:
    MOCK_METHOD(Mids, getMids, (const Mids&, YieldCtx, const OptStatus&), (const, override));
    MOCK_METHOD(Mids, getMids, (const Tids&, const ResolveOptions&, YieldCtx), (const, override));
    MOCK_METHOD(Mids, getMids, (const Mids&, const Tids&, const ResolveOptions&, YieldCtx), (const, override));
    MOCK_METHOD(Mids, getMids, (const Fid&, const FidFilter&, YieldCtx, const OptStatus&), (const, override));
    MOCK_METHOD(Mids, getMids, (const Fid&, const FidFilter&, const std::optional<Mid>&, std::size_t, YieldCtx, const OptStatus&), (const, override));
    MOCK_METHOD(Mids, getMids, (const Lid&, YieldCtx), (const, override));
    MOCK_METHOD(Mids, getMids, (const macs::Tab::Type&, YieldCtx, const OptStatus&), (const, override));
    MOCK_METHOD(Mids, getMidsInFolderCascade, (const Fid& fid, const ExcludeFilters&, YieldCtx), (const, override));
    MOCK_METHOD(Mids, getMidsByTidAndWithSameHeaders, (const Tid&, YieldCtx), (const, override));
    MOCK_METHOD(Mids, getMids, (const macs::HdrDateAndMessageIdVec&, YieldCtx), (const, override));

    MOCK_METHOD(size_t, getMidsCount, (const Fid&, const OptStatus&, YieldCtx), (const, override));
    MOCK_METHOD(size_t, getMidsCount, (const Lid&, YieldCtx), (const, override));
    MOCK_METHOD(size_t, getMidsCount, (const macs::Tab::Type&, const OptStatus&, YieldCtx), (const, override));
    MOCK_METHOD(size_t, getMessageCountInThreads, (const Tids&, YieldCtx), (const, override));
    MOCK_METHOD(size_t, getMidsCountCascade, (const Fid&, const ExcludeFilters&, YieldCtx), (const, override));

    MOCK_METHOD(Fid, getFid, (const macs::Folder::Symbol&, YieldCtx), (const, override));
    MOCK_METHOD(macs::Label, getLabel, (const macs::Label::Symbol&, YieldCtx), (const, override));

    MOCK_METHOD(OptEnvelope, getEnvelope, (const Mid&, YieldCtx), (const, override));

    MOCK_METHOD(macs::LabelSet, getLabelSet, (YieldCtx), (const, override));

    MOCK_METHOD(macs::settings::ParametersPtr, getParameters, (std::vector<std::string>, YieldCtx), (const, override));
};

} // namespace mbox_oper
