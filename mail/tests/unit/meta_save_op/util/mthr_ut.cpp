#include <mail/notsolitesrv/src/meta_save_op/util/mthr.h>

#include <mail/message_types/lib/message_types.h>

#include <mail/notsolitesrv/tests/unit/util/mthr.h>

#include <gtest/gtest.h>

namespace {

using namespace NNotSoLiteSrv::NMetaSaveOp;

using NNotSoLiteSrv::TEmailAddress;

TEST(TestMakeMthrRequest, must_make_mthr_request) {
    NNotSoLiteSrv::NMthr::TMthrRequest expectedMthrRequest;
    expectedMthrRequest.HdrFromDomain = "Domain";
    expectedMthrRequest.Subject = "Subject";
    expectedMthrRequest.MsgTypes = {NMail::MT_DELIVERY, NMail::MT_REGISTRATION};
    expectedMthrRequest.MessageId = "MessageId";
    expectedMthrRequest.References = {{"Reference0", "Reference1"}};
    expectedMthrRequest.InReplyTo = "InReplyTo";
    expectedMthrRequest.DomainLabel = "DomainLabel";

    TRequest request;
    request.message.subject = expectedMthrRequest.Subject;
    request.message.from.emplace_back(TEmailAddress{.Domain = expectedMthrRequest.HdrFromDomain});
    request.message.message_id = *expectedMthrRequest.MessageId;
    request.message.in_reply_to = *expectedMthrRequest.InReplyTo;
    request.message.references = *expectedMthrRequest.References;
    request.domain_label = *expectedMthrRequest.DomainLabel;
    request.types = expectedMthrRequest.MsgTypes;
    EXPECT_EQ(expectedMthrRequest, MakeMthrRequest(request));
}

}
