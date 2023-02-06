#include <mail/notsolitesrv/src/mdbsave/client.h>

#include <mail/notsolitesrv/tests/unit/fakes/context.h>
#include <mail/notsolitesrv/tests/unit/mdbsave/expected.h>
#include <mail/notsolitesrv/tests/unit/mocks/cluster_call.h>
#include <mail/notsolitesrv/tests/unit/mocks/ymod_tvm.h>
#include <mail/notsolitesrv/tests/unit/util/mdbsave.h>
#include <mail/notsolitesrv/tests/unit/util/ymod_httpclient.h>

#include <gtest/gtest.h>

namespace {

using namespace testing;

using NNotSoLiteSrv::EError;
using NNotSoLiteSrv::make_error_code;
using NNotSoLiteSrv::NMdbSave::TActions;
using NNotSoLiteSrv::NMdbSave::TAttachment;
using NNotSoLiteSrv::NMdbSave::TFolderActions;
using NNotSoLiteSrv::NMdbSave::TMdbSaveCallback;
using NNotSoLiteSrv::NMdbSave::TMdbSaveClient;
using NNotSoLiteSrv::NMdbSave::TMdbSaveRequestRcptNode;
using NNotSoLiteSrv::NMdbSave::TMdbSaveResponseRcpt;
using NNotSoLiteSrv::NMdbSave::TMdbSaveRequest;
using NNotSoLiteSrv::NMdbSave::TMdbSaveResult;
using NNotSoLiteSrv::NMdbSave::TMdbSaveResponse;
using NNotSoLiteSrv::NMdbSave::TMessage;
using NNotSoLiteSrv::NMdbSave::TMimePart;
using NNotSoLiteSrv::NMdbSave::TPath;
using NNotSoLiteSrv::NMdbSave::TRequestAddedLids;
using NNotSoLiteSrv::NMdbSave::TRequestAddedSymbols;
using NNotSoLiteSrv::NMdbSave::TRequestFolder;
using NNotSoLiteSrv::NMdbSave::TRequestLabels;
using NNotSoLiteSrv::NMdbSave::TRequestLabelSymbols;
using NNotSoLiteSrv::NMdbSave::TRequestLids;
using NNotSoLiteSrv::NMdbSave::TRequestLabel;
using NNotSoLiteSrv::NMdbSave::TResponseLabel;
using NNotSoLiteSrv::NMdbSave::TResolvedFolder;
using NNotSoLiteSrv::TEmailAddress;
using NNotSoLiteSrv::THttpRequest;
using NNotSoLiteSrv::THttpResponse;

struct TTestMdbSaveClient : Test {
    std::vector<TEmailAddress> MakeAddress(const std::string& address, const std::string& rcptId) const {
        std::vector<TEmailAddress> result;
        for (const auto& addressId : IdRange) {
            TEmailAddress email;
            email.Local += "local_" + address + addressId + rcptId;
            email.Domain += "domain_" + address + addressId + rcptId;
            email.DisplayName += "display_name_" + address + addressId + rcptId;
            result.emplace_back(std::move(email));
        }
        return result;
    }

    TMessage MakeMessage(const std::string& rcptId) const {
        TMessage message;
        message.OldMid = std::string("old_mid_") + rcptId;
        message.ExtImapId = 0;
        message.Firstline += "fl_" + rcptId;
        message.Size = 1;
        message.Lids = TRequestLids();
        message.LabelSymbols = TRequestLabelSymbols();
        message.Labels = TRequestLabels();
        for (const auto& labelId : IdRange) {
            message.Lids->emplace_back("lid_" + labelId + rcptId);
            message.LabelSymbols->emplace_back("sym_" + labelId + rcptId);
            TRequestLabel label;
            label.Name += "label_name_" + labelId + rcptId;
            label.Type += "label_type_" + labelId + rcptId;
            message.Labels->emplace_back(std::move(label));
        }
        message.Tab = std::string("tab_") + rcptId;
        message.Storage.Stid += "stid_" + rcptId;
        message.Storage.Offset = 2;
        message.Headers.RecievedDate = 3;
        message.Headers.Date = 4;
        message.Headers.Subject += "subject_" + rcptId;
        message.Headers.MsgId = std::string("msg_id_") + rcptId;
        message.Headers.ReplyTo = std::string("reply_to_") + rcptId;
        message.Headers.InReplyTo = std::string("in_reply_to_") + rcptId;
        message.Headers.From = MakeAddress("from_", rcptId);
        message.Headers.To = MakeAddress("to_", rcptId);
        message.Headers.Cc = MakeAddress("cc_", rcptId);
        message.Headers.Bcc = MakeAddress("bcc_", rcptId);

        message.Attachments = std::vector<TAttachment>();
        for (const auto& attachmentId : IdRange) {
            TAttachment attachment;
            attachment.Hid += "hid_" + attachmentId + rcptId;
            attachment.Name += "name_" + attachmentId + rcptId;
            attachment.Type += "type_" + attachmentId + rcptId;
            attachment.Size = 5;
            message.Attachments->emplace_back(std::move(attachment));
        }

        message.MimeParts = std::vector<TMimePart>();
        for (const auto& partId : IdRange) {
            TMimePart part;
            part.Hid += "hid_" + partId + rcptId;
            part.ContentType += "type_" + partId + rcptId;
            part.ContentSubtype += "subtype_" + partId + rcptId;
            part.Boundary = std::string("boundary_") + partId + rcptId;
            part.Name = std::string("name_") + partId + rcptId;
            part.Charset = std::string("charset_") + partId + rcptId;
            part.Encoding = std::string("encoding_") + partId + rcptId;
            part.ContentDisposition = std::string("dispos_") + partId + rcptId;
            part.FileName = std::string("file_") + partId + rcptId;
            part.ContentId = std::string("id_") + partId + rcptId;
            part.Offset = 6;
            part.Length = 7;
            message.MimeParts->emplace_back(std::move(part));
        }

        message.ThreadInfo.Hash.Namespace += "ns_" + rcptId;
        message.ThreadInfo.Hash.Value += "value_" + rcptId;
        message.ThreadInfo.Limits.Days = 8;
        message.ThreadInfo.Limits.Count = 9;
        message.ThreadInfo.Rule += "rule_" + rcptId;
        for (const auto& id : IdRange) {
            message.ThreadInfo.ReferenceHashes.emplace_back("hash_" + id + rcptId);
            message.ThreadInfo.MessageIds.emplace_back("msg_" + id + rcptId);
        }
        message.ThreadInfo.InReplyToHash += "in_reply_to_hash_" + rcptId;
        message.ThreadInfo.MessageIdHash += "msg_id_hash_" + rcptId;

        return message;
    }

    TRequestFolder MakeFolder(const std::string& name, const std::string& rcptId) const {
        TRequestFolder folder;
        folder.Fid = std::string("fid_") + name + rcptId;
        folder.Path = TPath();
        folder.Path->Path = std::string("path_") + name + rcptId;
        folder.Path->Delimeter = "|";
        return folder;
    }

    TActions MakeActions() const {
        TActions actions;
        actions.Duplicates.Ignore = true;
        actions.Duplicates.Remove = false;
        actions.UseFilters = true;
        actions.DisablePush = false;
        actions.Original.StoreAsDeleted = true;
        actions.Original.NoSuchFolder = "fail";
        actions.RulesApplied = TFolderActions();
        actions.RulesApplied->StoreAsDeleted = false;
        actions.RulesApplied->NoSuchFolder = "create";
        return actions;
    }

    TMdbSaveRequest MakeMdbSaveRequest() const {
        TMdbSaveRequest request;
        for (const auto& rcptId : IdRange) {
            TMdbSaveRequestRcptNode node;
            node.Id = rcptId;

            node.Rcpt.User.Uid += "uid_" + rcptId;
            node.Rcpt.User.Suid = std::string("suid_") + rcptId;
            node.Rcpt.Message = MakeMessage(rcptId);
            node.Rcpt.Folders.Destination = MakeFolder("dest_", rcptId);
            node.Rcpt.Folders.Original = MakeFolder("orig_", rcptId);
            node.Rcpt.Actions = MakeActions();

            node.Rcpt.AddedLids = TRequestAddedLids();
            node.Rcpt.AddedSymbols = TRequestAddedSymbols();
            for (const auto& added_id : IdRange) {
                node.Rcpt.AddedLids->emplace_back("lid_" + added_id + rcptId);
                node.Rcpt.AddedSymbols->emplace_back("sym_" + added_id + rcptId);
            }

            node.Rcpt.Imap = true;
            request.Rcpts.emplace_back(std::move(node));
        }

        request.Sync = false;

        return request;
    }

    TMdbSaveResponse MakeMdbSaveResponse() const {

        TMdbSaveResponseRcpt successNode;
        successNode.Uid = "uid_0";
        successNode.Status = "ok";
        successNode.Mid = "mid_0";
        successNode.ImapId = "imap_id_0";
        successNode.Tid = "tid_0";
        successNode.Duplicate = false;
        successNode.Folder = TResolvedFolder{"fid_0", "name_0", "type_0", 123};
        successNode.Labels = {
            {"lid_0", "symbol_0"},
            {"lid_1", "symbol_1"}
        };

        TMdbSaveResponseRcpt permErrorNode;
        permErrorNode.Uid = "uid_1";
        permErrorNode.Status = "perm error";
        permErrorNode.Description = "perm error happened";

        TMdbSaveResponseRcpt tempErrorNode;
        tempErrorNode.Uid = "uid_2";
        tempErrorNode.Status = "temp error";
        tempErrorNode.Description = "temp error happened";

        TMdbSaveResponse response;
        response.Rcpts = {
            {"0", std::move(successNode)},
            {"1", std::move(permErrorNode)},
            {"2", std::move(tempErrorNode)}
        };
        return response;
    }



    std::string MakeErrorResponseBody() const {
        return R"({
            "error": "Error",
            "message": "Message"
        })";
    }

    THttpRequest MakeMdbSaveHttpRequest(bool useTvm = true) const {
        const auto url = "/1/save?service=nsls&session_id=" + SessionId;
        return useTvm ?
            THttpRequest::POST(url, "X-Ya-Service-Ticket: TvmServiceTicket\r\n", MakeMdbSaveRequestBody()) :
            THttpRequest::POST(url, MakeMdbSaveRequestBody());
    }

    void TestMdbSave(TMdbSaveCallback callback, bool useTvm = true) {
        auto context = GetContext({{"mdbsave_use_tvm", std::to_string(useTvm)}});
        const TMdbSaveClient mdbsaveClient{std::move(context), ClusterCall, TvmModule};
        mdbsaveClient.MdbSave(IoContext, SessionId, MakeMdbSaveRequest(), std::move(callback));
        IoContext.run();
    }

    const std::shared_ptr<StrictMock<TClusterCallMock>> ClusterCall{std::make_shared<StrictMock<
        TClusterCallMock>>()};
    const std::shared_ptr<StrictMock<TYmodTvmMock>> TvmModule{std::make_shared<StrictMock<TYmodTvmMock>>()};
    const std::string SessionId{"SessionId"};
    boost::asio::io_context IoContext;
    const std::string ServiceName{"mdbsave_service_name"};
    const std::string ServiceTicket{"TvmServiceTicket"};
    const int StatusOk{200};
    const int StatusNonRetryable{404};
    const int StatusRetryable{500};
    const std::vector<std::string> IdRange{"0", "1"};
};

TEST_F(TTestMdbSaveClient, for_tvm_module_error_mdbsave_must_return_error) {
    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(Return(
        ymod_tvm::error::make_error_code(ymod_tvm::error::tickets_not_loaded)));
    TestMdbSave([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(EError::TvmServiceTicketError), errorCode);
        EXPECT_EQ((TMdbSaveResult{}), result);
    });
}

TEST_F(TTestMdbSaveClient, for_cluster_call_error_mdbsave_must_return_error) {
    const auto expectedErrorCode = ymod_httpclient::http_error::make_error_code(yhttp::errc::connect_error);
    const InSequence sequence;
    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(DoAll(SetArgReferee<1>(
        ServiceTicket), Return(ymod_tvm::error::make_error_code(ymod_tvm::error::success))));
    EXPECT_CALL(*ClusterCall, async_run(_, MakeMdbSaveHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(expectedErrorCode, THttpResponse{}));
    TestMdbSave([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(expectedErrorCode, errorCode);
        EXPECT_EQ((TMdbSaveResult{}), result);
    });
}

TEST_F(TTestMdbSaveClient, for_mdbsave_nonretryable_error_mdbsave_must_return_error) {
    const InSequence sequence;
    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(DoAll(SetArgReferee<1>(
        ServiceTicket), Return(ymod_tvm::error::make_error_code(ymod_tvm::error::success))));
    EXPECT_CALL(*ClusterCall, async_run(_, MakeMdbSaveHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{StatusNonRetryable, {}, {}, {}}));
    TestMdbSave([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(EError::HttpNonRetryableStatus), errorCode);
        EXPECT_EQ((TMdbSaveResult{}), result);
    });
}

TEST_F(TTestMdbSaveClient, for_mdbsave_retries_exceeded_error_mdbsave_must_return_error) {
    const InSequence sequence;
    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(DoAll(SetArgReferee<1>(
        ServiceTicket), Return(ymod_tvm::error::make_error_code(ymod_tvm::error::success))));
    EXPECT_CALL(*ClusterCall, async_run(_, MakeMdbSaveHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{StatusRetryable, {}, MakeErrorResponseBody(), {}}));
    TestMdbSave([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(EError::HttpRetriesExceeded), errorCode);
        EXPECT_EQ((TMdbSaveResult{}), result);
    });
}

TEST_F(TTestMdbSaveClient, for_mdbsave_response_parse_error_mdbsave_must_return_error) {
    const InSequence sequence;
    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(DoAll(SetArgReferee<1>(
        ServiceTicket), Return(ymod_tvm::error::make_error_code(ymod_tvm::error::success))));
    EXPECT_CALL(*ClusterCall, async_run(_, MakeMdbSaveHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{StatusOk, {}, {}, {}}));
    TestMdbSave([&](auto errorCode, auto result) {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(make_error_code(EError::MdbSaveResponseParseError), errorCode);
        EXPECT_EQ((TMdbSaveResult{}), result);
    });
}

TEST_F(TTestMdbSaveClient,
    for_tvm_not_in_use_and_correct_mdbsave_response_mdbsave_must_return_parsed_response)
{
    const auto dontUseTvm = false;
    EXPECT_CALL(*ClusterCall, async_run(_, MakeMdbSaveHttpRequest(dontUseTvm), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{StatusOk, {}, MakeMdbSaveResponseBody(), {}}));
    auto callback = [&](auto errorCode, auto result) {
        ASSERT_FALSE(errorCode);
        EXPECT_EQ((TMdbSaveResult{MakeMdbSaveResponse()}), result);
    };

    TestMdbSave(std::move(callback), dontUseTvm);
}

TEST_F(TTestMdbSaveClient,
    for_tvm_in_use_and_correct_mdbsave_response_mdbsave_must_return_parsed_response)
{
    const InSequence sequence;
    EXPECT_CALL(*TvmModule, get_service_ticket(ServiceName, _)).WillOnce(DoAll(SetArgReferee<1>(
        ServiceTicket), Return(ymod_tvm::error::make_error_code(ymod_tvm::error::success))));
    EXPECT_CALL(*ClusterCall, async_run(_, MakeMdbSaveHttpRequest(), _, _)).
        WillOnce(InvokeArgument<3>(ymod_httpclient::http_error::make_error_code(yhttp::errc::success),
        THttpResponse{StatusOk, {}, MakeMdbSaveResponseBody(), {}}));
    TestMdbSave([&](auto errorCode, auto result) {
        ASSERT_FALSE(errorCode);
        EXPECT_EQ((TMdbSaveResult{MakeMdbSaveResponse()}), result);
    });
}

}
