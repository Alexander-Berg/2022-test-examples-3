#pragma once
#include "mocking-labels.h"
#include <macs/envelopes_repository.h>
#include <macs/threads_repository.h>
#include <macs/data_source_service.h>
#include <macs/connection_info.h>

#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Winconsistent-missing-override"
#endif

struct MockDataSourceService: public macs::DataSourceService {
    MOCK_METHOD(void, get_stats, (macs::data::stats::storage_info&), (override));
    MOCK_METHOD(void, init_mailbox, (const std::string&, const std::string&), (override));
};

struct MockEnvelopesRepository: public macs::EnvelopesRepository {
    MOCK_METHOD(uint64_t, syncListInThread, (const macs::EnvelopesSorting&,
                          const std::string&,
                          (const boost::variant<std::size_t, std::string>&),
                          const std::size_t,
                          macs::OnEnvelopeReceive) , (const, override));
    MOCK_METHOD(void, syncListInThreadWithLabel, (const std::string&,
                      const macs::Label&,
                      std::size_t,
                      std::size_t,
                      macs::OnEnvelopeReceive) , (const, override));
    MOCK_METHOD(void, syncListInThreadWithoutLabel, (const std::string&,
                      const macs::Label&,
                      std::size_t,
                      std::size_t,
                      macs::OnEnvelopeReceive) , (const, override));
    MOCK_METHOD(uint64_t, syncListWithNoAnswer, (const std::string&,
                          const std::string&,
                          macs::OnEnvelopeReceive) , (const, override));
    MOCK_METHOD(uint64_t, syncListInReplyTo, (const std::string&,
                          macs::OnEnvelopeReceive) , (const, override));
    MOCK_METHOD(uint64_t, syncListInReplyToByMid, (const std::string&,
                          macs::OnEnvelopeReceive) , (const, override));
    MOCK_METHOD(uint64_t, syncListFilterSearch, (bool,
                           bool,
                           const std::list<std::string>&,
                           const std::list<std::string>&,
                           const std::list<std::string>&,
                           const std::string&,
                           const macs::EnvelopesSorting&,
                           macs::OnEnvelopeReceive) , (const, override));
    MOCK_METHOD(void, syncErase, (const std::list<std::string>&,
                      macs::OnUpdate hook), (const, override));
    MOCK_METHOD(void, syncForceErase, (const std::list<std::string>&,
                 macs::OnUpdate hook), (const, override));
    MOCK_METHOD(void, syncAddLabels, (const std::list<macs::Label>&,
                      const std::list<std::string>&,
                      macs::OnUpdate hook), (const, override));
    MOCK_METHOD(void, syncAddLabelsByThreads, (const std::list<macs::Label>&,
                      const std::list<std::string>&,
                      macs::OnUpdateMessages hook), (const, override));
    MOCK_METHOD(void, syncRemoveLabels, (const std::list<macs::Label>&,
                      const std::list<std::string>&,
                      macs::OnUpdate hook), (const, override));
    MOCK_METHOD(void, syncRemoveLabelsByThreads, (const std::list<macs::Label>&,
                      const std::list<std::string>&,
                      macs::OnUpdateMessages hook), (const, override));
    MOCK_METHOD(void, syncChangeLabels, (const std::list<std::string>&,
                      const std::list<macs::Label>&,
                      const std::list<macs::Label>&,
                      macs::OnUpdate hook), (const, override));
    MOCK_METHOD(void, syncEntriesByIds, (const std::list<std::string>&,
                                              macs::Hook<macs::Sequence<macs::Envelope>>), (const, override));
    MOCK_METHOD(void, syncInsertEntry, (macs::Envelope, macs::MimeParts, ThreadMetaProvider, SaveOptions, macs::OnUpdateEnvelope), (const, override));
    MOCK_METHOD(void, syncUpdateEntry, (const macs::Envelope&, macs::Envelope, macs::MimeParts,
                         ThreadMetaProvider, bool, macs::OnUpdateEnvelope), (const, override));
    MOCK_METHOD(void, syncGetFirstEnvelopeDate, (const std::string&, macs::OnUnixTime), (const, override));
    MOCK_METHOD(void, syncGetFirstEnvelopeDateOptional, (const std::string&, macs::OnUnixTimeOptional), (const, override));
    MOCK_METHOD(void, syncGetFirstEnvelopeDateInTab, (const macs::Tab::Type& tab, macs::OnUnixTimeOptional), (const, override));

    MOCK_METHOD(void, syncUpdateMailAttributes, (const std::string&, const std::vector<std::string>, macs::OnExecute), (const, override));

    MOCK_METHOD(void, syncGetEnvelopesCount, (const std::string&,
                            std::time_t,
                            std::time_t,
                            macs::OnCountReceive), (const, override));

    MOCK_METHOD(void, syncResetFreshCounter, (), (const));
    MOCK_METHOD(void, syncResetFreshCounter, (macs::OnUpdate), (const, override));
    MOCK_METHOD(void, syncCopyMessages, (const std::string&,
                                        const std::vector<std::string>&,
                                        macs::OnCopy), (const, override));
    MOCK_METHOD(void, syncMoveMessages, (const std::string&,
                                        const std::optional<macs::Tab::Type>&,
                                        const std::list<std::string>&,
                                        macs::OnUpdate), (const, override));
    MOCK_METHOD(void, syncUpdateStatus, (const std::list<std::string>&,
                                        macs::Envelope::Status,
                                        macs::OnUpdate hook), (const, override));
    MOCK_METHOD(void, syncGetMidsByFolder, (const std::string&, macs::OnMidsReceive), (const, override));
    MOCK_METHOD(void, syncGetMidsRangeByFolder, (const std::string&, const std::optional<macs::Mid>&, size_t,
            macs::OnMidsReceive), (const, override));
    MOCK_METHOD(void, syncGetMidsByFolderWithoutStatus, (const std::string&,
                                                              macs::Envelope::Status,
                                                              macs::OnMidsReceive), (const, override));
    MOCK_METHOD(void, syncGetMidsRangeByFolderWithoutStatus, (const std::string&,
                                                              macs::Envelope::Status,
                                                              const std::optional<macs::Mid>&,
                                                              std::size_t,
                                                              macs::OnMidsReceive), (const, override));
    MOCK_METHOD(void, syncGetAttachments, (const macs::Mid&, macs::OnAttachmentsReceive), (const, override));
    MOCK_METHOD(void, syncGetMessageStIds, (const macs::Mids&, macs::OnMidsWithStidsReceive), (const, override));
    MOCK_METHOD(void, syncGetWindatMessageStId, (const std::string&, const std::string&, macs::OnStIdReceive), (const, override));
    MOCK_METHOD(void, syncGetMimes, (const macs::Mids&, macs::OnMidsWithMimes), (const, override));
    MOCK_METHOD(void, syncGetMimesWithDeleted, (const macs::Mids&, macs::OnMidsWithMimes), (const, override));
    MOCK_METHOD(void, syncGetWindatMimes, (const macs::Mids&, macs::OnMidsWithMimes), (const, override));
    MOCK_METHOD(void, syncGetMimesWithAttaches, (const macs::Mids&, macs::OnMidsWithMimesAndAttaches), (const, override));
    MOCK_METHOD(void, syncGetMimesWithAttachesWithDeleted, (const macs::Mids&, macs::OnMidsWithMimesAndAttaches), (const, override));

    MOCK_METHOD(void, syncCheckDuplicates, (const macs::Envelope &,
            macs::CheckDuplicates), (const, override));

    MOCK_METHOD(void, syncGetMidsByConditions, (const macs::EnvelopesQueryMidsByConditions&, macs::OnMidsReceive handler), (const, override));
    MOCK_METHOD(void, syncGetMidsByConditionsWithoutStatus, (const macs::EnvelopesQueryMidsByConditions&,
                                                                  macs::Envelope::Status,
                                                                  macs::OnMidsReceive), (const, override));
    MOCK_METHOD(void, syncDeleteFromStorage, (const macs::Stid&, macs::OnExecute), (const, override));

    MOCK_METHOD(uint64_t, syncThreadsByTids, (const macs::TidVector&, macs::OnEnvelopeReceive handler), (const, override));

    MOCK_METHOD(uint64_t, syncThreadsWithLabel, (const std::string&, size_t, size_t, macs::OnEnvelopeReceive handler), (const, override));
    MOCK_METHOD(void, syncThreadsInFolderWithoutLabels, (const std::string&,
        const std::list<std::string>&, size_t, size_t, macs::OnEnvelopeReceive handler), (const, override));
    MOCK_METHOD(void, syncThreadsInFolderWithLabels, (const std::string&,
        const std::list<std::string>&, size_t, size_t, macs::OnEnvelopeReceive handler), (const, override));
    MOCK_METHOD(void, syncMessagesInFolderWithoutLabels, (const std::string&,
        const std::list<std::string>&, size_t, size_t, const macs::EnvelopesSorting&, macs::OnEnvelopeReceive handler), (const, override));
    MOCK_METHOD(void, syncMessagesInFolderWithLabels, (const std::string&,
        const std::list<std::string>&, size_t, size_t, const macs::EnvelopesSorting&, macs::OnEnvelopeReceive handler), (const, override));

    MOCK_METHOD(void, syncGetMessageId, (const std::string&, macs::OnRfcMessageId), (const, override));
    MOCK_METHOD(void, syncGetFreshCounter, (macs::OnCountReceive handler), (const, override));
    MOCK_METHOD(void, syncGetAttachesCounters, (macs::OnAttachesCounters handler), (const, override));

    MOCK_METHOD(void, syncGetMidsWithoutStatus, (const macs::Mids&,
                                                      macs::Envelope::Status,
                                                      macs::OnMidsReceive), (const, override));

    MOCK_METHOD(void, syncGetMidsByTidsAndLids, (const macs::Tids&, const macs::Lids&,
            macs::OnMidsReceive), (const, override));

    MOCK_METHOD(void, syncEnvelopesQueryInMailbox, (const std::string& fromMid,
                                                         size_t from, size_t count, bool groups,
                                                         const macs::EnvelopesSorting& order,
                                                         const std::list<std::string>& labels,
                                                         const std::list<std::string>& excludeLabels,
                                                         macs::OnEnvelopeReceive handler), (const, override));

    MOCK_METHOD(void, syncEnvelopesQueryInFolder, (const macs::EnvelopesQueryInFolder::Params&,
                                                        macs::OnEnvelopeReceive), (const, override));

    MOCK_METHOD(void, syncGetByMessageId, (const macs::Fid& fid,
                                                const std::string& messageId,
                                                macs::OnMidsAndImapIds handler), (const, override));

    MOCK_METHOD(void, syncGetByMessageId, (const std::string& messageId,
                                                const macs::FidVec&,
                                                macs::OnMidsReceive handler), (const, override));

    MOCK_METHOD(void, syncEnvelopesInFolderWithMimes, (size_t, size_t, const macs::EnvelopesSorting&,
                                                         const macs::Fid&, macs::OnEnvelopeWithMimeReceive), (const, override));

    MOCK_METHOD(void, syncEnvelopesByMidsWithMimes, (const macs::MidList&, macs::OnEnvelopeWithMimeReceive), (const, override));

    MOCK_METHOD(void, syncMidsByThreadAndWithSameHeaders, (const macs::Tid&, macs::OnMidsReceive), (const, override));

    MOCK_METHOD(void, syncMidsByHdrDateAndMessageIdPairs, (const macs::EnvelopesQueryMidsByHdrPairs&, macs::OnMidsReceive), (const, override));

    MOCK_METHOD(void, syncEnvelopesInFolderWithMimesByChunks, (size_t, size_t, size_t, const macs::Fid&,
                                                                    macs::OnEnvelopeWithMimeChunkReceive), (const, override));

    MOCK_METHOD(void, syncGetDeletedMessages, (size_t, size_t, macs::OnEnvelopeReceive), (const, override));

    MOCK_METHOD(void, syncGetDeletedMessagesInInterval, (size_t, size_t,
                                                              std::time_t, std::time_t,
                                                              macs::OnEnvelopeReceive), (const, override));

    MOCK_METHOD(void, syncGetNewCountInFolderWithLabels, (
                           const macs::Fid&, const std::list<macs::Lid>&, size_t, macs::OnCountReceive), (const, override));

    MOCK_METHOD(void, syncGetNewCountInFolderWithoutLabels, (
                           const macs::Fid&, const std::list<macs::Lid>&, size_t, macs::OnCountReceive), (const, override));

    MOCK_METHOD(void, syncEnvelopesQueryInTab, (const macs::EnvelopesQueryInTab::Params&,
                                                     macs::OnEnvelopeReceive), (const, override));

    MOCK_METHOD(void, syncCountNewMessagesWithLids, (const std::list<macs::Lid>& labels,
                                                     size_t limit,
                                                     macs::OnCountReceive handler), (const, override));

    MOCK_METHOD(void, syncGetMidsByTab, (const macs::Tab::Type&, macs::OnMidsReceive), (const, override));
    MOCK_METHOD(void, syncGetMidsByTabWithoutStatus, (const macs::Tab::Type&,
                                                           macs::Envelope::Status,
                                                           macs::OnMidsReceive), (const, override));

    MOCK_METHOD(void, syncGetMidsByLabel, (const macs::Label&, macs::OnMidsReceive), (const, override));

    MockEnvelopesRepository() = default;
    MockEnvelopesRepository(macs::UserJournalPtr journal)
            : macs::EnvelopesRepository(journal) {}
};

struct MockThreadsRepository : macs::ThreadsRepository {
    MOCK_METHOD(void, syncThreadsParticipantsList, (const macs::TidVector&,
                                                         macs::OnParticipantsReceive), (const, override));
    MOCK_METHOD(void, syncThreadLabelsList, (const macs::TidVector&,
                                                  macs::OnThreadLabelsReceive), (const, override));
    MOCK_METHOD(void, syncFillIdsMap, (const macs::MidList&,
                                            const macs::TidList&,
                                            const macs::Lids&,
                                            macs::OnThreadMailboxItemsReceive), (const, override));
    MOCK_METHOD(void, syncFillIdsList, (const macs::MidList&,
                                             const macs::TidList&,
                                             macs::OnMidsReceive), (const, override));
    MOCK_METHOD(void, syncFillIdsListWithoutStatus, (const macs::MidList&,
                                                          const macs::TidList&,
                                                          macs::Envelope::Status,
                                                          macs::OnMidsReceive), (const, override));
    MOCK_METHOD(void, syncFillIdsFList, (const macs::TidList&,
                                              const macs::Fid&,
                                              macs::OnMidsReceive), (const, override));
    MOCK_METHOD(void, syncFindThreadsByHash, (const macs::ThreadHash&,
                                                   const macs::ThreadLimits&,
                                                   macs::OnThreadIdsReceive), (const, override));
    MOCK_METHOD(void, syncFindThreadsByReferences, (const macs::MidVec&,
                                                         const macs::MailRefVec&,
                                                         macs::OnThreadIdsReceive), (const, override));
    MOCK_METHOD(void, syncJoinThreads, (const macs::ThreadId&,
                                             const macs::TidVector&,
                                             macs::OnThreadIdReceive), (const, override));
    MOCK_METHOD(void, syncFilterThreadsByLid, (const macs::TidVector&,
                                                    const macs::Lid&,
                                                    macs::OnThreadIdsReceive), (const, override));
    MOCK_METHOD(void, syncMessageCountInThreads, (const macs::TidVector&,
                                                       macs::OnCountReceive), (const, override));
};

struct MockConnectionInfo: public macs::ConnectionInfo {
    MockConnectionInfo(void) : mdata(nullptr), mlabels(nullptr), menvelopes(nullptr) {}

    MockConnectionInfo(const std::string& suid, const std::string& mdb)
        : macs::ConnectionInfo(suid, "", false, mdb),
          mdata(nullptr), mlabels(nullptr), menvelopes(nullptr) {
    }

    macs::RepositoriesHolderPtr createRepositories(void) const {
        macs::RepositoriesHolderPtr holder(new macs::RepositoriesHolder());
        holder->service.reset(mdata = new MockDataSourceService());
        holder->labels.reset(mlabels = new MockLabelsRepository());
        holder->envelopes.reset(menvelopes = new MockEnvelopesRepository());
        return holder;
    }

    MockDataSourceService& data(void) { return *mdata; }
    MockLabelsRepository& labels(void) { return *mlabels; }
    MockEnvelopesRepository& envelopes(void) { return *menvelopes; }

private:
    mutable MockDataSourceService *mdata;
    mutable MockLabelsRepository *mlabels;
    mutable MockEnvelopesRepository *menvelopes;
};

#ifdef __clang__
#pragma clang diagnostic pop
#endif

