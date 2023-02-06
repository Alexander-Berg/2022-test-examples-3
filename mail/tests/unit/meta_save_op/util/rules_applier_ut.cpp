#include <mail/notsolitesrv/src/meta_save_op/util/rules_applier.h>

#include <mail/notsolitesrv/tests/unit/util/rules_applier.h>

#include <gtest/gtest.h>

namespace {

using namespace NNotSoLiteSrv::NMetaSaveOp;

using NNotSoLiteSrv::TEmailAddress;

TEST(TestMakeRulesApplierMessage, for_empty_from_email_address_list_must_return_empty_optional_from) {
    const auto rulesApplierMessage{MakeRulesApplierMessage(TMessage{})};
    EXPECT_FALSE(rulesApplierMessage.From);
}

TEST(TestMakeRulesApplierMessage, must_make_rules_applier_message) {
    NNotSoLiteSrv::NRulesApplier::TMessage expectedMessage;
    expectedMessage.From = TEmailAddress{};
    expectedMessage.From->Local = "Local";
    expectedMessage.From->Domain = "Domain";
    expectedMessage.From->DisplayName = "DisplayName";
    expectedMessage.Sender = "Sender";
    expectedMessage.ReplyTo = "ReplyTo";

    TMessage message;
    message.from.emplace_back(TEmailAddress{expectedMessage.From->Local, expectedMessage.From->Domain,
        expectedMessage.From->DisplayName});
    message.sender = *expectedMessage.Sender;
    message.reply_to = *expectedMessage.ReplyTo;
    EXPECT_EQ(expectedMessage, MakeRulesApplierMessage(message));
}

TEST(TestMakeRulesApplierRecipient, must_make_rules_applier_recipient) {
    const NNotSoLiteSrv::NRulesApplier::TRecipient expectedRecipient{.Uid = 1, .UseFilters = false};
    const TRecipient recipient{{.uid = expectedRecipient.Uid}, {.use_filters = expectedRecipient.UseFilters}};
    EXPECT_EQ(expectedRecipient, MakeRulesApplierRecipient(recipient));
}

TEST(TestMakeRulesApplierRecipientMap, must_make_rules_applier_recipient_map) {
    const NNotSoLiteSrv::NRulesApplier::TRecipientMap expectedRecipientMap{
        {"DeliveryId0", {.Uid = 1, .UseFilters = false}}, {"DeliveryId1", {.Uid = 2, .UseFilters = true}}};
    const TRecipientMap recipientMap {
        {"DeliveryId0", {{.uid = 1}, {.use_filters = false}}},
        {"DeliveryId1", {{.uid = 2}, {.use_filters = true}}}
    };

    EXPECT_EQ(expectedRecipientMap, MakeRulesApplierRecipientMap(recipientMap));
}

}
