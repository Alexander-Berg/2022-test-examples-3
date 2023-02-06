#ifndef DOBERMAN_TESTS_MAILBOX_MOCKS_H_
#define DOBERMAN_TESTS_MAILBOX_MOCKS_H_

#include "wrap_yield.h"
#include <gmock/gmock.h>
#include <src/meta/types.h>
#include <boost/optional/optional_io.hpp>

namespace doberman {
namespace testing {

using ::doberman::Uid;
using ::doberman::Fid;
using ::doberman::Mid;

struct MailboxWithMimesMock {
    MOCK_METHOD(const MailboxWithMimesMock&, inFolder, (::macs::Fid), (const));
    MOCK_METHOD(const MailboxWithMimesMock&, byImapId, (::macs::Fid), (const));
    MOCK_METHOD(const MailboxWithMimesMock&, byMids, (::macs::MidList), (const));
    MOCK_METHOD(const MailboxWithMimesMock&, count, (size_t), (const));
    MOCK_METHOD(const MailboxWithMimesMock&, from, (int64_t), (const));
    MOCK_METHOD(const MailboxWithMimesMock&, to, (int64_t), (const));
    MOCK_METHOD(std::vector<EnvelopeWithMimes>, get, (Yield), (const));
};

struct MailboxMock {
    const MailboxMock& folders() const { return *this;}
    const MailboxMock& labels() const {return *this;}
    const MailboxMock& envelopes() const {return *this;}
    const MailboxMock& query() const {return *this;}
    const MailboxMock& subscribedFolders() const {return *this;}

    const MailboxMock* operator()(const Uid& uid) const {
        return mailbox(uid);
    }
    MOCK_METHOD(const MailboxMock*, mailbox, (const Uid&), (const));
    MOCK_METHOD(void, resetFoldersCache, (), (const));
    MOCK_METHOD(void, resetLabelsCache, (), (const));
    MOCK_METHOD(const MailboxMock&, inFolder, (::macs::Folder), (const));
    MOCK_METHOD(const MailboxMock&, byImapId, (::macs::Fid), (const));
    MOCK_METHOD(const MailboxWithMimesMock&, withMimes, (), (const));
    MOCK_METHOD(std::vector<::macs::Envelope>, get, (Yield), (const));
    MOCK_METHOD(const MailboxMock&, from, (int64_t), (const));
    MOCK_METHOD(const MailboxMock&, to, (int64_t), (const));
    MOCK_METHOD(const MailboxMock&, count, (size_t), (const));
    MOCK_METHOD(std::vector<::macs::Envelope>, getByIds, (::macs::MidList, Yield), (const));
    MOCK_METHOD(::macs::MidsWithMimes, getMimes, (::macs::MidList, Yield), (const));
    MOCK_METHOD(::macs::FolderSet, getAllFolders, (Yield), (const));
    MOCK_METHOD(::macs::LabelSet, getAllLabels, (Yield), (const));
    MOCK_METHOD(Label, getOrCreateLabel, (const std::string&,
                                               const std::string&,
                                               const Label::Type&,
                                               Yield), (const));

    MOCK_METHOD(boost::optional<Revision>, getSyncedRevision, (const std::string&, const ::macs::Fid&, Yield), (const));
    MOCK_METHOD(SyncMessage, syncMessage, (const std::string&,
                                                const Envelope&,
                                                const ::macs::MimeParts&,
                                                const std::vector<::macs::Hash>& referenceHashes,
                                                const ::macs::Hash& inReplyToHash,
                                                Yield), (const));
    MOCK_METHOD(SyncMessage, syncMessageQuiet, (const std::string&,
                                                const Envelope&,
                                                const ::macs::MimeParts&,
                                                const std::vector<::macs::Hash>& referenceHashes,
                                                const ::macs::Hash& inReplyToHash,
                                                Yield), (const));
    MOCK_METHOD(::macs::UpdateMessagesResult, deleteMessages, (std::string, Fid, ::macs::MidVec, Revision, Yield), (const));
    MOCK_METHOD(::macs::UpdateMessagesResult, labelMessages, (const std::string&, const Fid&, const ::macs::MidVec&,
                                         const Revision&, const std::vector<Label>&, Yield), (const));
    MOCK_METHOD(::macs::UpdateMessagesResult, unlabelMessages, (const std::string&, const Fid&, const ::macs::MidVec&,
                                         const Revision&, const std::vector<Label>&, Yield), (const));
    MOCK_METHOD(::macs::UpdateMessagesResult, joinThreads, (const Uid&, const Fid&, const ThreadId&,
                                         const std::vector<ThreadId>&, const Revision, Yield), (const));
    MOCK_METHOD(std::vector<::macs::Envelope>, getEnvelopes, (const std::string&, const ::macs::Fid&, Yield), (const));

    MOCK_METHOD(::macs::Mids, getSyncedMids, (std::string, Fid, std::size_t, Yield), (const));
    MOCK_METHOD(Revision, remove, (::macs::Mids, Yield), (const));
};

} // namespace testing
} // namespace doberman

#endif /* DOBERMAN_TESTS_MAILBOX_MOCKS_H_ */
