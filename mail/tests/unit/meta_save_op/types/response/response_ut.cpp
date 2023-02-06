#include <mail/notsolitesrv/src/meta_save_op/types/response.h>
#include <mail/notsolitesrv/src/user/storage.h>

#include <mail/notsolitesrv/tests/unit/fakes/context.h>

#include <util/generic/is_in.h>
#include <util/generic/xrange.h>

#include <gtest/gtest.h>

using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NMetaSaveOp;
using namespace testing;

struct TMetaSaveOpResponseTest: public Test {
    void SetUp() override {
        Ctx = GetContext();

        for (auto i: xrange(1, 3)) {
            auto id = std::to_string(i);
            AddUser(id, id);
        }
    }

    NUser::TUser& AddUser(const std::string& uid, const std::string& dlvId) {
        NUser::TUser user;
        user.Status = NUser::ELoadStatus::Found;
        user.Uid = uid;
        user.DeliveryParams.DeliveryId = dlvId;
        user.DeliveryParams.IsFromRcptTo = true;
        user.DeliveryParams.NeedDelivery = true;

        return Users.GetUsers().emplace(uid + "@" + dlvId, user)->second;
    }

    TRecipientResponse MakeTestRecipientResponse(EStatus status, std::string uid,
        EStatus mdbCommitResponseStatus) const
    {
        return {
            .status = status,
            .reply = {{{.address = "a@a.ru", .body = "b1"}, {.address = "b@b.ru", .body = "b2"}}},
            .forward = {{{.address = "c@a.ru"}, {.address = "d@b.ru"}}},
            .notify = {{{.address = "e@a.ru"}, {.address = "f@b.ru"}}},
            .rule_ids = {{"7", "8", "9"}},
            .mdb_commit = {{
                .uid = std::move(uid),
                .status = mdbCommitResponseStatus,
                .mid = {{"1"}},
                .imap_id = {{2}},
                .duplicate = {{false}},
                .tid = {{"3"}},
                .folder = {{.fid = "4", .name = {{"Inbox"}}, .type = {{"inbox"}}, .type_code = 5}},
                .labels = {{{.lid = "17"}, {.lid = "18", .symbol = {{"seen_label"}}}}}
            }}
        };
    }

    TContextPtr Ctx;
    NUser::TStorage Users;
};

TEST_F(TMetaSaveOpResponseTest, NoMdbSaveResponses) {
    const std::string deliveryIds[] = {"1", "2"};
    const TResponse response{
        {
            {deliveryIds[0], {.status = EStatus::Ok}},
            {deliveryIds[1], {.status = EStatus::Ok}}
        }
    };

    UpdateRecipientsStatusFromResponse(Ctx, Users, response);
    int i{0};
    for (const auto& [login, user]: Users.GetFilteredUsers(NUser::Found)) {
        i++;
        EXPECT_EQ(user.DeliveryResult.ErrorCode, EError::MetaSaveOpTemporaryError);
    }
    EXPECT_EQ(i, 2);
}

TEST_F(TMetaSaveOpResponseTest, OneUserHasSuccessMdbSaveResponseOtherPermanentMetaSaveOpError) {
    const std::string deliveryIds[] = {"1", "2"};
    const std::string uid{"1"};
    const TResponse response{
        {
            {deliveryIds[0], MakeTestRecipientResponse(EStatus::Ok, uid, EStatus::Ok)},
            {deliveryIds[1], {.status = EStatus::PermanentError}}
        }
    };

    UpdateRecipientsStatusFromResponse(Ctx, Users, response);
    EXPECT_FALSE(Users.GetUserByEmail("1@1")->DeliveryResult.ErrorCode);
    EXPECT_EQ(Users.GetUserByEmail("2@2")->DeliveryResult.ErrorCode, EError::MetaSaveOpPermanentError);
}

TEST_F(TMetaSaveOpResponseTest, IncorrectDeliveryIdToUidMapping) {
    const std::string deliveryIds[] = {"1", "2"};
    const std::string uid{"2"};
    const TResponse response{
        {
            {deliveryIds[0], MakeTestRecipientResponse(EStatus::Ok, uid, EStatus::Ok)},
            {deliveryIds[1], MakeTestRecipientResponse(EStatus::Ok, uid, EStatus::Ok)}
        }
    };

    UpdateRecipientsStatusFromResponse(Ctx, Users, response);
    int i{0};
    for (const auto& [login, user]: Users.GetFilteredUsers(NUser::Found)) {
        i++;
        EXPECT_EQ(user.DeliveryResult.ErrorCode, EError::MetaSaveOpTemporaryError);
    }
    EXPECT_EQ(i, 2);
}

TEST_F(TMetaSaveOpResponseTest, IgnoreGlobalStatusIfMdbCommitIsOk) {
    const std::string deliveryIds[] = {"1", "2"};
    const std::string uids[] = {"1", "2"};
    const TResponse response{
        {
            {deliveryIds[0], MakeTestRecipientResponse(EStatus::TemporaryError, uids[0], EStatus::Ok)},
            {deliveryIds[1], MakeTestRecipientResponse(EStatus::PermanentError, uids[1], EStatus::Ok)}
        }
    };

    UpdateRecipientsStatusFromResponse(Ctx, Users, response);
    int i{0};
    for (const auto& [login, user]: Users.GetFilteredUsers(NUser::Found)) {
        i++;
        EXPECT_FALSE(user.DeliveryResult.ErrorCode);
    }
    EXPECT_EQ(i, 2);
}

TEST_F(TMetaSaveOpResponseTest, OneUserMdbOkOtherFail) {
    const std::string deliveryIds[] = {"1", "2", "3"};
    const std::string uids[] = {"1", "2", "3"};
    const TResponse response{
        {
            {deliveryIds[0], MakeTestRecipientResponse(EStatus::Ok, uids[0], EStatus::Ok)},
            {deliveryIds[1], MakeTestRecipientResponse(EStatus::Ok, uids[1], EStatus::TemporaryError)},
            {deliveryIds[2], MakeTestRecipientResponse(EStatus::Ok, uids[2], EStatus::PermanentError)}
        }
    };

    AddUser(uids[2], deliveryIds[2]);
    UpdateRecipientsStatusFromResponse(Ctx, Users, response);
    EXPECT_FALSE(Users.GetUserByEmail("1@1")->DeliveryResult.ErrorCode);
    EXPECT_EQ(Users.GetUserByEmail("2@2")->DeliveryResult.ErrorCode, EError::MetaSaveOpTemporaryError);
    EXPECT_EQ(Users.GetUserByEmail("3@3")->DeliveryResult.ErrorCode, EError::MetaSaveOpPermanentError);
}

TEST_F(TMetaSaveOpResponseTest, ExceptionNotOverridesPermErrorForUser) {
    Users.GetUserByEmail("2@2")->DeliveryResult.ErrorCode = EError::UserInvalid;

    const std::string deliveryIds[] = {"1", "2"};
    const std::string uid{"2"};
    const TResponse response{
        {
            {deliveryIds[0], MakeTestRecipientResponse(EStatus::Ok, uid, EStatus::Ok)},
            {deliveryIds[1], MakeTestRecipientResponse(EStatus::Ok, uid, EStatus::Ok)}
        }
    };

    UpdateRecipientsStatusFromResponse(Ctx, Users, response);
    EXPECT_EQ(Users.GetUserByEmail("1@1")->DeliveryResult.ErrorCode, EError::MetaSaveOpTemporaryError);
    EXPECT_EQ(Users.GetUserByEmail("2@2")->DeliveryResult.ErrorCode, EError::UserInvalid);
}

TEST_F(TMetaSaveOpResponseTest, FillResult) {
    const std::string deliveryId{"1"};
    const std::string uid{"1"};
    const TResponse response{{{deliveryId, MakeTestRecipientResponse(EStatus::Ok, uid, EStatus::Ok)}}};

    UpdateRecipientsStatusFromResponse(Ctx, Users, response);
    const auto& result = Users.GetUserByEmail("1@1")->DeliveryResult;
    EXPECT_FALSE(result.ErrorCode);
    EXPECT_EQ(result.Mid, "1");
    EXPECT_EQ(result.ImapId, "2");
    EXPECT_EQ(result.Tid, "3");
    EXPECT_EQ(result.IsDuplicate, false);
    EXPECT_EQ(result.Fid, "4");
    EXPECT_EQ(result.FolderName, "Inbox");
    EXPECT_EQ(result.FolderType, "inbox");
    EXPECT_EQ(result.FolderTypeCode, 5);
    EXPECT_EQ(result.Lids, std::vector<std::string>({"17", "18"}));
    EXPECT_EQ(result.LabelSymbols, std::vector<std::string>({"seen_label"}));
    EXPECT_EQ(result.FilterIds, std::vector<std::string>({"7","8","9"}));
    EXPECT_EQ(result.AutoReplies, (std::vector<std::pair<std::string, std::string>>{
        {"a@a.ru", "b1"}, {"b@b.ru", "b2"}}));
    EXPECT_EQ(result.Forwards, std::vector<std::string>({"c@a.ru", "d@b.ru"}));
    EXPECT_EQ(result.Notifies, std::vector<std::string>({"e@a.ru", "f@b.ru"}));
}
