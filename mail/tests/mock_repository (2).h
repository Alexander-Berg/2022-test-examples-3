#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/spaniel/ymod_db/tests/mock_repository.h>

namespace spaniel {

struct MockRepository: public Repository {
    MOCK_METHOD(void, asyncOrganizationUids, (const OrganizationParams&, OnOrganizationUids), (const, override));
    MOCK_METHOD(void, asyncUpdateOrganizationUids, (const OrganizationParams&, Uids, OnUpdate), (const, override));
    MOCK_METHOD(void, asyncListActiveOrganizationIds, (const RequestId&, OnOrganizationIds), (const, override));

    MOCK_METHOD(void, asyncActivateOrganization, (const OrganizationParams&, OnExecute), (const, override));
    MOCK_METHOD(void, asyncGetOrganization, (const OrganizationParams&, OnOrganization), (const, override));
    MOCK_METHOD(void, asyncDisableOrganization, (const OrganizationParams&, std::time_t, OnExecute), (const, override));
    MOCK_METHOD(void, asyncDeactivateOrganization, (const OrganizationParams&, std::time_t, OnExecute), (const, override));
    MOCK_METHOD(void, asyncReserveSearchId, (const CommonParams&, OnSearchId), (const, override));
    MOCK_METHOD(void, asyncLastSearchId, (const CommonParams&, OnOptionalSearchId), (const, override));
    MOCK_METHOD(void, asyncCreateSearch, (const CommonParams&, SearchId, std::time_t, std::time_t, QueryJson, SearchName, Uids, OnExecute), (const, override));
    MOCK_METHOD(void, asyncFillSearch, (const CommonParams&, SearchId, SearchResults, SearchState, OnExecute), (const, override));
    MOCK_METHOD(void, asyncCacheSearchResults, (const CommonParams&, SearchId, SearchResults, OnExecute), (const, override));
    MOCK_METHOD(void, asyncSearchById, (const CommonParams&, SearchId, OnSearch), (const, override));
    MOCK_METHOD(void, asyncSearchList, (const CommonParams&, const StrongPageParams&, OnSearches), (const, override));
    MOCK_METHOD(void, asyncFailSearch, (const CommonParams&, SearchId, const Notice&, OnExecute), (const, override));
    MOCK_METHOD(void, asyncContinueSearch, (const CommonParams&, SearchId, OnExecute), (const, override));
    MOCK_METHOD(void, asyncSearchByOneUser, (const CommonParams&, SearchId, Uid, const StrongPageParams&, OnSearchResults), (const, override));
    MOCK_METHOD(void, asyncSearchByAllUsers, (const CommonParams&, SearchId, const StrongPageParams&, OnSearchResults), (const, override));
    MOCK_METHOD(void, asyncMinReceivedDateBySearchAndUid, (const CommonParams&, SearchId, Uid, OnOptionalReceivedDate), (const, override));
    MOCK_METHOD(void, asyncLogAction, (const CommonParams&, ActionHistoryType, std::string, OnExecute), (const, override));
    MOCK_METHOD(void, asyncGetActionHistory, (const CommonParams&, const StrongPageParams&, OnActionHistoryItems), (const, override));
    MOCK_METHOD(void, asyncRenameSearch, (const CommonParams&, SearchId, const std::string&, OnUpdate), (const, override));
    MOCK_METHOD(void, asyncArchiveSearch, (const CommonParams&, SearchId, OnExecute), (const, override));
    MOCK_METHOD(void, asyncMessagesInSearch, (const CommonParams&, const MessagesAccessParams&, OnMessagesInSearch), (const, override));

    MOCK_METHOD(void, asyncRegisterTaskId, (const OrganizationParams&, ymod_queuedb::TaskId, OnTaskCreated), (const, override));
    MOCK_METHOD(void, asyncRemoveTaskId, (const OrganizationParams&, ymod_queuedb::TaskId, OnExecute), (const, override));
};

}
