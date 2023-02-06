#include "connection_provider_mock.hpp"
#include "transaction_mock.hpp"

#include <src/logic/db/update_contacts_impl.hpp>
#include <src/logic/interface/types/directory_entry_id.hpp>
#include <src/logic/interface/types/org_id.hpp>
#include <src/logic/interface/types/reflection/vcard.hpp>
#include <yamail/data/serialization/yajl.h>

#include <tests/unit/generic_operators.hpp>
#include <tests/unit/services/db/contacts/types/new_contacts_email.hpp>
#include <tests/unit/services/db/contacts/types/updated_contact.hpp>
#include <tests/unit/services/db/contacts/types/updated_contacts_email.hpp>
#include <tests/unit/test_with_task_context.hpp>

namespace {

using collie::error_code;
using collie::logic::ContactId;
using collie::logic::db::contacts::DbUpdatedContact;
using collie::logic::DirectoryEntryId;
using collie::logic::EmailId;
using collie::logic::Error;
using collie::logic::ListId;
using collie::logic::OrgId;
using collie::logic::Revision;
using collie::logic::TagId;
using collie::logic::UpdatedContact;
using collie::logic::UpdatedContacts;
using collie::logic::Vcard;
using collie::make_expected;
using collie::make_expected_from_error;
using collie::services::db::ConstUserType;
using collie::services::db::contacts::query::AcquireRevision;
using collie::services::db::contacts::query::CreateContactsEmails;
using collie::services::db::contacts::query::GetContactsEmailIdsByContactIdAndTagIds;
using collie::services::db::contacts::query::GetContactsEmailIdsByContactsIds;
using collie::services::db::contacts::query::GetContactsIdsByContactsEmailIds;
using collie::services::db::contacts::query::GetEmailIdsTagIdsByEmailIds;
using collie::services::db::contacts::query::RemoveContactsEmails;
using collie::services::db::contacts::query::TagContacts;
using collie::services::db::contacts::query::TagContactsEmails;
using collie::services::db::contacts::query::UntagContacts;
using collie::services::db::contacts::query::UntagContactsEmails;
using collie::services::db::contacts::query::UpdateContacts;
using collie::services::db::contacts::query::UpdateContactsEmails;
using collie::services::db::contacts::NewContactsEmail;
using collie::services::db::contacts::UpdatedContactsEmail;
using collie::services::db::PassportUserId;
using collie::TaskContextPtr;
using yamail::data::serialization::toJson;
using yamail::unexpected_type;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<
        AcquireRevision,
        CreateContactsEmails,
        GetContactsEmailIdsByContactIdAndTagIds,
        GetContactsEmailIdsByContactsIds,
        GetContactsIdsByContactsEmailIds,
        GetEmailIdsTagIdsByEmailIds,
        RemoveContactsEmails,
        TagContacts,
        TagContactsEmails,
        UntagContacts,
        UntagContactsEmails,
        UpdateContacts,
        UpdateContactsEmails>;

using ConnectionProvider = collie::tests::ConnectionProvider<
        AcquireRevision,
        CreateContactsEmails,
        GetContactsEmailIdsByContactIdAndTagIds,
        GetContactsEmailIdsByContactsIds,
        GetContactsIdsByContactsEmailIds,
        GetEmailIdsTagIdsByEmailIds,
        RemoveContactsEmails,
        TagContacts,
        TagContactsEmails,
        UntagContacts,
        UntagContactsEmails,
        UpdateContacts,
        UpdateContactsEmails>;

using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<
        AcquireRevision,
        CreateContactsEmails,
        GetContactsEmailIdsByContactIdAndTagIds,
        GetContactsEmailIdsByContactsIds,
        GetContactsIdsByContactsEmailIds,
        GetEmailIdsTagIdsByEmailIds,
        RemoveContactsEmails,
        TagContacts,
        TagContactsEmails,
        UntagContacts,
        UntagContactsEmails,
        UpdateContacts,
        UpdateContactsEmails>;

using MakeConnectionProvider = collie::tests::MakeConnectionProvider<
        AcquireRevision,
        CreateContactsEmails,
        GetContactsEmailIdsByContactIdAndTagIds,
        GetContactsEmailIdsByContactsIds,
        GetContactsIdsByContactsEmailIds,
        GetEmailIdsTagIdsByEmailIds,
        RemoveContactsEmails,
        TagContacts,
        TagContactsEmails,
        UntagContacts,
        UntagContactsEmails,
        UpdateContacts,
        UpdateContactsEmails>;

using AcquireRevisionRows = std::vector<AcquireRevision::result_type>;
using CreateContactsEmailsRows = std::vector<CreateContactsEmails::result_type>;
using GetContactsEmailIdsByContactIdAndTagIdsRows = std::vector<
        GetContactsEmailIdsByContactIdAndTagIds::result_type>;
using GetContactsEmailIdsByContactsIdsRows = std::vector<GetContactsEmailIdsByContactsIds::result_type>;
using GetContactsIdsByContactsEmailIdsRows = std::vector<GetContactsIdsByContactsEmailIds::result_type>;
using GetEmailIdsTagIdsByEmailIdsRows = std::vector<GetEmailIdsTagIdsByEmailIds::result_type>;
using RemoveContactsEmailsRows = std::vector<RemoveContactsEmails::result_type>;
using TagContactsRows = std::vector<TagContacts::result_type>;
using TagContactsEmailsRows = std::vector<TagContactsEmails::result_type>;
using UntagContactsRows = std::vector<UntagContacts::result_type>;
using UntagContactsEmailsRows = std::vector<UntagContactsEmails::result_type>;
using UpdateContactsRows = std::vector<UpdateContacts::result_type>;
using UpdateContactsEmailsRows = std::vector<UpdateContactsEmails::result_type>;

using UpdateContactsImpl = collie::logic::db::UpdateContactsImpl<MakeConnectionProvider>;

struct TestLogicDbUpdateContacts : TestWithTaskContext {
    Vcard makeVcardWithoutEmails() const {
        Vcard vcard;
        vcard.names = {{"First1", "Middle1", "Last1", {}, {}}};
        vcard.telephone_numbers = {{"0123456789", {}, {}, {}}};
        vcard.organizations = {{"company1", "department1", "title1", "summary1", {{"type1"}}}};
        vcard.directory_entries = {{orgId + 1, "Name1", directoryEntryId + 1, {{"type1"}}}};
        return vcard;
    }

    Vcard makeExtendedVcardWithoutEmails() const {
        Vcard vcard;
        vcard.names = {{"First0", "Middle0", "Last0", {}, {}}};
        vcard.telephone_numbers = {{"+79031234567", {}, {}, {}}, {"+79657654321", {}, {}, {}}};
        vcard.organizations = {{"company0", "department0", "title0", "summary0", {{"type0"}}}};
        vcard.directory_entries = {{orgId, "Name0", directoryEntryId, {{"type0"}}}};
        return vcard;
    }

    Vcard makeVcard() const {
        auto vcard{makeVcardWithoutEmails()};
        vcard.emails = {{"local1@domain1.ru", {{"t1"}}, "label1"}};
        return vcard;
    }

    Vcard makeExtendedVcard() const {
        auto vcard{makeExtendedVcardWithoutEmails()};
        vcard.emails = {{"local1@domain1.ru", {{"t1"}}, "label1"}, {"local2@domain2.ru", {{"t2"}}, "label2"},
                {"local3@domain3.ru", {{"t3"}}, "label3"}, {"local4@domain4.ru", {{"t4"}}, "label4"}};
        return vcard;
    }

    UpdatedContacts makeUpdatedContacts() const {
        UpdatedContacts contacts;
        UpdatedContact contact;
        contact.contact_id = contactId;
        contact.list_id = listId;
        contact.vcard = makeExtendedVcard();
        contact.uri = uri + "0";
        contacts.updated_contacts.emplace_back(contact);

        contact.contact_id = contactId + 1;
        contact.vcard = makeVcard();
        contact.uri = uri + "1";
        contacts.updated_contacts.emplace_back(std::move(contact));

        return contacts;
    }

    UpdatedContacts makeUpdatedContactsWithTagIdsToAdd() const {
        auto contacts{makeUpdatedContacts()};
        contacts.updated_contacts[0].add_tag_ids = {tagId};
        contacts.updated_contacts[1].add_tag_ids = {tagId + 2};
        return contacts;
    }

    UpdatedContacts makeUpdatedContactsWithTagIdsToAddAndRemove() const {
        auto contacts{makeUpdatedContactsWithTagIdsToAdd()};
        contacts.updated_contacts[0].remove_tag_ids = {tagId + 1};
        contacts.updated_contacts[1].remove_tag_ids = {tagId + 3};
        return contacts;
    }

    DbUpdatedContact makeDbUpdatedContact() const {
        DbUpdatedContact contact;
        contact.contact_id = contactId;
        contact.list_id = listId;
        contact.format = "vcard_v1";
        contact.vcard = ozo::pg::jsonb(toJson(makeExtendedVcardWithoutEmails()));
        contact.uri = uri + "0";
        return contact;
    }

    enum class EmailUpdateMode {
        UpdateCreateRemove,
        CreateRemove,
        UpdateCreate,
        UpdateRemove,
        UpdateCreateContact0,
        UpdateCreateContact1,
        UpdateCreateContact0Chunk0,
        UpdateCreateContact0Chunk1,
        UpdateCreateContact1Chunk0
    };

    std::vector<ContactId> makeContactIds(EmailUpdateMode emailUpdateMode) const {
        switch (emailUpdateMode) {
            case EmailUpdateMode::UpdateCreateContact0:
                return {contactId};
            case EmailUpdateMode::UpdateCreateContact1:
                return {contactId + 1};
            default:
                return {contactId, contactId + 1};
        }
    }

    std::vector<DbUpdatedContact> makeDbUpdatedContacts(EmailUpdateMode emailUpdateMode) const {
        std::vector<DbUpdatedContact> contacts;
        if (emailUpdateMode != EmailUpdateMode::UpdateCreateContact1) {
            contacts.emplace_back(makeDbUpdatedContact());
        }

        if (emailUpdateMode != EmailUpdateMode::UpdateCreateContact0) {
            auto contact{makeDbUpdatedContact()};
            contact.contact_id = contactId + 1;
            contact.vcard = ozo::pg::jsonb(toJson(makeVcardWithoutEmails()));
            contact.uri = uri + "1";
            contacts.emplace_back(std::move(contact));
        }

        return contacts;
    }

    std::vector<UpdatedContactsEmail> makeUpdatedContactsEmails(EmailUpdateMode emailUpdateMode) const {
        std::unordered_map<EmailUpdateMode, std::vector<UpdatedContactsEmail>> map{
                {EmailUpdateMode::UpdateCreateRemove,
                 {{emailId + 1, {}, "local1@domain1.ru", {{"t1"}}, "label1"}}},
                {EmailUpdateMode::CreateRemove,
                 {}},
                {EmailUpdateMode::UpdateCreate,
                 {{emailId, {}, "local1@domain1.ru", {{"t1"}}, "label1"},
                  {emailId + 1, {}, "local1@domain1.ru", {{"t1"}}, "label1"}}},
                {EmailUpdateMode::UpdateRemove,
                 {{emailId + 1, {}, "local1@domain1.ru", {{"t1"}}, "label1"},
                  {emailId + 2, {}, "local2@domain2.ru", {{"t2"}}, "label2"},
                  {emailId + 3, {}, "local3@domain3.ru", {{"t3"}}, "label3"},
                  {emailId + 4, {}, "local4@domain4.ru", {{"t4"}}, "label4"},
                  {emailId + 5, {}, "local1@domain1.ru", {{"t1"}}, "label1"}}},
                {EmailUpdateMode::UpdateCreateContact0Chunk0,
                 {{emailId, {}, "local1@domain1.ru", {{"t1"}}, "label1"}}},
                {EmailUpdateMode::UpdateCreateContact0Chunk1,
                 {{emailId + 1, {}, "local2@domain2.ru", {{"t2"}}, "label2"}}},
                {EmailUpdateMode::UpdateCreateContact1Chunk0,
                 {{emailId + 2, {}, "local1@domain1.ru", {{"t1"}}, "label1"}}}};
        return map.at(emailUpdateMode);
    }

    GetContactsEmailIdsByContactsIdsRows makeGetContactsEmailIdsByContactsIdsRows(
            EmailUpdateMode emailUpdateMode) const {
        std::unordered_map<EmailUpdateMode, GetContactsEmailIdsByContactsIdsRows> map{
                {EmailUpdateMode::UpdateCreateRemove,
                 {{contactId, emailId, "local0@domain0.ru"},
                  {contactId, emailId + 1, "local1@domain1.ru"},
                  {contactId + 1, emailId + 2, "local0@domain0.ru"}}},
                {EmailUpdateMode::CreateRemove,
                 {{contactId, emailId, "local0@domain0.ru"},
                  {contactId, emailId + 1, "localA@domainA.ru"},
                  {contactId + 1, emailId + 2, "local0@domain0.ru"}}},
                {EmailUpdateMode::UpdateCreate,
                 {{contactId, emailId, "local1@domain1.ru"},
                  {contactId + 1, emailId + 1, "local1@domain1.ru"}}},
                {EmailUpdateMode::UpdateRemove,
                 {{contactId, emailId, "local0@domain0.ru"},
                  {contactId, emailId + 1, "local1@domain1.ru"},
                  {contactId, emailId + 2, "local2@domain2.ru"},
                  {contactId, emailId + 3, "local3@domain3.ru"},
                  {contactId, emailId + 4, "local4@domain4.ru"},
                  {contactId + 1, emailId + 5, "local1@domain1.ru"}}},
                {EmailUpdateMode::UpdateCreateContact0,
                 {{contactId, emailId, "local1@domain1.ru"},
                  {contactId, emailId + 1, "local2@domain2.ru"}}},
                {EmailUpdateMode::UpdateCreateContact1,
                 {{contactId + 1, emailId + 2, "local1@domain1.ru"}}}};
        return map.at(emailUpdateMode);
    }

    std::vector<EmailId> makeRemovedEmailIds(EmailUpdateMode emailUpdateMode) const {
        std::unordered_map<EmailUpdateMode, std::vector<EmailId>> map{
                {EmailUpdateMode::UpdateCreateRemove, {emailId, emailId + 2}},
                {EmailUpdateMode::CreateRemove, {emailId, emailId + 1, emailId + 2}},
                {EmailUpdateMode::UpdateCreate, {}},
                {EmailUpdateMode::UpdateRemove, {emailId}}};
        return map.at(emailUpdateMode);
    }

    std::vector<NewContactsEmail> makeNewContactsEmails(EmailUpdateMode emailUpdateMode) const {
        std::unordered_map<EmailUpdateMode, std::vector<NewContactsEmail>> map{
                {EmailUpdateMode::UpdateCreateRemove,
                 {{contactId, "local2@domain2.ru", {{"t2"}}, "label2"},
                  {contactId, "local3@domain3.ru", {{"t3"}}, "label3"},
                  {contactId, "local4@domain4.ru", {{"t4"}}, "label4"},
                  {contactId + 1, "local1@domain1.ru", {{"t1"}}, "label1"}}},
                {EmailUpdateMode::CreateRemove,
                 {{contactId, "local1@domain1.ru", {{"t1"}}, "label1"},
                  {contactId, "local2@domain2.ru", {{"t2"}}, "label2"},
                  {contactId, "local3@domain3.ru", {{"t3"}}, "label3"},
                  {contactId, "local4@domain4.ru", {{"t4"}}, "label4"},
                  {contactId + 1, "local1@domain1.ru", {{"t1"}}, "label1"}}},
                {EmailUpdateMode::UpdateCreate,
                 {{contactId, "local2@domain2.ru", {{"t2"}}, "label2"},
                  {contactId, "local3@domain3.ru", {{"t3"}}, "label3"},
                  {contactId, "local4@domain4.ru", {{"t4"}}, "label4"}}},
                {EmailUpdateMode::UpdateRemove,
                 {}},
                {EmailUpdateMode::UpdateCreateContact0Chunk0,
                 {{contactId, "local3@domain3.ru", {{"t3"}}, "label3"}}},
                {EmailUpdateMode::UpdateCreateContact0Chunk1,
                 {{contactId, "local4@domain4.ru", {{"t4"}}, "label4"}}}};
        return map.at(emailUpdateMode);
    }

    void prepareBaseExpectations(const TaskContextPtr& context) const {
        EXPECT_CALL(makeProviderMock, call(context, PassportUserId{uid})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, begin(_));
    }

    void prepareBaseUserExpectations() const {
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
    }

    void prepareAcquireRevisionExpectations() const {
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(AcquireRevision{uid, userType})).WillOnce(Return(
                AcquireRevisionRows{revision}));
    }

    void prepareLocalUpdateContactsExpectations(const TaskContextPtr& context) const {
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, uid()).WillOnce(Return(uid));
        EXPECT_CALL(providerMock, userType()).WillOnce(Return(userType));
    }

    void prepareBaseUpdateContactsExpectations(const TaskContextPtr& context) const {
        prepareBaseExpectations(context);
        prepareAcquireRevisionExpectations();
        prepareLocalUpdateContactsExpectations(context);
    }

    void prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode emailUpdateMode) const {
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactsIds{uid, userType, makeContactIds(
                emailUpdateMode)})).WillOnce(Return(makeGetContactsEmailIdsByContactsIdsRows(
                        emailUpdateMode)));
    }

    void prepareUpdateContactsExpectations(EmailUpdateMode emailUpdateMode) const {
        EXPECT_CALL(providerMock, request(UpdateContacts{uid, userType, makeDbUpdatedContacts(
                emailUpdateMode), requestId, {}})).WillOnce(Return(UpdateContactsRows{revision}));
    }

    void prepareBaseUpdateContactsEmailsExpectations(const TaskContextPtr& context) const {
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
    }

    void prepareUpdateContactsEmailsExpectations(const TaskContextPtr& context,
            EmailUpdateMode emailUpdateMode) const {
        prepareBaseUpdateContactsEmailsExpectations(context);
        EXPECT_CALL(providerMock, request(UpdateContactsEmails{uid, userType, makeUpdatedContactsEmails(
                emailUpdateMode), requestId, {}})).WillOnce(Return(UpdateContactsEmailsRows{revision}));
    }

    void prepareGetEmailIdsTagIdsByEmailIdsExpectations(const TaskContextPtr& context,
            EmailUpdateMode emailUpdateMode) const {
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetEmailIdsTagIdsByEmailIds{uid, userType, makeRemovedEmailIds(
                emailUpdateMode)})).WillOnce(Return(GetEmailIdsTagIdsByEmailIdsRows{{emailId, tagId}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
    }

    void prepareUntagContactsAndContactsEmailsExpectationsForEmailUntagging(
            const TaskContextPtr& context) const {
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetContactsIdsByContactsEmailIds{uid, userType, {emailId}})).
                WillOnce(Return(GetContactsIdsByContactsEmailIdsRows{{contactId, emailId}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(UntagContactsEmails{uid, userType, tagId, {emailId}, requestId})).
                WillOnce(Return(UntagContactsEmailsRows{revision}));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactIdAndTagIds{uid, userType, contactId,
                {tagId}})).WillOnce(Return(GetContactsEmailIdsByContactIdAndTagIdsRows{}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(UntagContacts{uid, userType, tagId, {contactId}, requestId})).
                WillOnce(Return(UntagContactsRows{revision}));
    }

    void prepareUntagContactsAndContactsEmailsExpectationsForContactUntagging(
            const TaskContextPtr& context) const {
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactsIds{uid, userType, {contactId}})).
                WillOnce(Return(GetContactsEmailIdsByContactsIdsRows{
                        {contactId, emailId + 1, "local1@domain1.ru"},
                        {contactId, emailId + 5, "local2@domain2.ru"}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(UntagContactsEmails{uid, userType, tagId + 1,
                {emailId + 1, emailId + 5}, requestId})).WillOnce(Return(UntagContactsEmailsRows{revision}));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(UntagContacts{uid, userType, tagId + 1, {contactId},
                requestId})).WillOnce(Return(UntagContactsEmailsRows{revision}));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactsIds{uid, userType, {contactId + 1}})).
                WillOnce(Return(GetContactsEmailIdsByContactsIdsRows{
                        {contactId + 1, emailId + 5, "local1@domain1.ru"}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(UntagContactsEmails{uid, userType, tagId + 3,
                {emailId + 5}, requestId})).WillOnce(Return(UntagContactsEmailsRows{revision}));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(UntagContacts{uid, userType, tagId + 3, {contactId + 1},
                requestId})).WillOnce(Return(UntagContactsEmailsRows{revision}));
    }

    void prepareTagContactsAndContactsEmailsExpectations(const TaskContextPtr& context) const {
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactsIds{uid, userType, {contactId}})).
                WillOnce(Return(GetContactsEmailIdsByContactsIdsRows{
                        {contactId, emailId + 1, "local1@domain1.ru"},
                        {contactId, emailId + 5, "local2@domain2.ru"}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(TagContacts{uid, userType, tagId, {contactId}, requestId})).
                WillOnce(Return(TagContactsRows{revision}));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(TagContactsEmails{uid, userType, tagId, {emailId + 1, emailId + 5},
                requestId})).WillOnce(Return(TagContactsEmailsRows{revision}));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactsIds{uid, userType, {contactId + 1}})).
                WillOnce(Return(GetContactsEmailIdsByContactsIdsRows{
                        {contactId + 1, emailId + 5, "local1@domain1.ru"}}));
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(TagContacts{uid, userType, tagId + 2, {contactId + 1}, requestId})).
                WillOnce(Return(TagContactsRows{revision}));

        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(TagContactsEmails{uid, userType, tagId + 2, {emailId + 5},
                requestId})).WillOnce(Return(TagContactsEmailsRows{revision}));
    }

    void prepareRemoveContactsEmailsExpectations(const TaskContextPtr& context,
            EmailUpdateMode emailUpdateMode) const {
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(RemoveContactsEmails{uid, userType, makeRemovedEmailIds(
                emailUpdateMode), requestId})).WillOnce(Return(RemoveContactsEmailsRows{revision}));
    }

    void prepareBaseCreateContactsEmailsExpectations(const TaskContextPtr& context) const {
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
    }

    void prepareCreateContactsEmailsExpectations(const TaskContextPtr& context,
            EmailUpdateMode emailUpdateMode) const {
        prepareBaseCreateContactsEmailsExpectations(context);
        EXPECT_CALL(providerMock, request(CreateContactsEmails{uid, userType, makeNewContactsEmails(
                emailUpdateMode), requestId})).WillOnce(Return(CreateContactsEmailsRows{{{revision, {}}}}));
    }

    void prepareFinalExpectations() const {
        EXPECT_CALL(providerMock, commit(_));
    }

    void testWithBadRequestResult(const TaskContextPtr& context, UpdatedContacts updatedContacts) const {
        const auto result{updateContacts(context, std::to_string(uid), std::move(updatedContacts))};
        EXPECT_EQ(make_expected_from_error<Revision>(error_code{Error::badRequest}), result);
    }

    void testWithCorrectResult(const TaskContextPtr& context, UpdatedContacts updatedContacts) const {
        const auto result{updateContacts(context, std::to_string(uid), std::move(updatedContacts))};
        EXPECT_EQ(make_expected(revision), result);
    }

    void testInChunks(UpdatedContacts updatedContacts) const {
        const auto chunkSize{1};
        const auto result{collie::logic::db::contacts::updateContacts(std::move(updatedContacts), provider,
                chunkSize)};
        EXPECT_EQ(make_expected(revision), result);
    }

    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider{&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider{&makeProviderMock};
    const UpdateContactsImpl updateContacts{makeProvider};
    const std::int64_t uid{42};
    const ConstUserType userType{"passport_user"};
    const ContactId contactId{1};
    const DirectoryEntryId directoryEntryId{1};
    const EmailId emailId{1};
    const ListId listId{1};
    const Revision revision{3};
    const OrgId orgId{1};
    const TagId tagId{5};
    const std::string uri{"URI"};
    const std::string requestId{"request_id"};
    const unexpected_type<error_code> badRequest{error_code{Error::badRequest}};
};

TEST_F(TestLogicDbUpdateContacts, for_empty_UpdatedContacts_must_return_default_revision) {
    withSpawn([&](const auto& context) {
        const auto result{updateContacts(context, std::to_string(uid), {})};
        EXPECT_EQ(make_expected(Revision{}), result);
    });
}

TEST_F(TestLogicDbUpdateContacts, for_nonexistent_user_must_return_error_userNotFound) {
    withSpawn([&](const auto& context) {
        const std::string nonexistentUid{"uid"};
        const auto result{updateContacts(context, nonexistentUid, makeUpdatedContacts())};
        EXPECT_EQ(make_expected_from_error<Revision>(error_code{Error::userNotFound}), result);
    });
}

TEST_F(TestLogicDbUpdateContacts, for_AcquireRevision_which_ended_with_error_must_return_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseExpectations(context);
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(AcquireRevision{uid, userType})).WillOnce(Return(badRequest));
        testWithBadRequestResult(context, makeUpdatedContacts());
    });
}

TEST_F(TestLogicDbUpdateContacts,
        for_GetContactsEmailIdsByContactsIds_which_ended_with_error_must_return_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactsIds{uid, userType, makeContactIds(
                EmailUpdateMode::UpdateCreateRemove)})).WillOnce(Return(badRequest));
        testWithBadRequestResult(context, makeUpdatedContacts());
    });
}

TEST_F(TestLogicDbUpdateContacts, for_UpdateContacts_which_ended_with_error_must_return_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateCreateRemove);
        EXPECT_CALL(providerMock, request(UpdateContacts{uid, userType, makeDbUpdatedContacts(
                EmailUpdateMode::UpdateCreateRemove), requestId, {}})).WillOnce(Return(badRequest));
        testWithBadRequestResult(context, makeUpdatedContacts());
    });
}

TEST_F(TestLogicDbUpdateContacts, for_UpdateContactsEmails_which_ended_with_error_must_return_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareBaseUpdateContactsEmailsExpectations(context);
        EXPECT_CALL(providerMock, request(UpdateContactsEmails{uid, userType, makeUpdatedContactsEmails(
                EmailUpdateMode::UpdateCreateRemove), requestId, {}})).WillOnce(Return(badRequest));
        testWithBadRequestResult(context, makeUpdatedContacts());
    });
}

TEST_F(TestLogicDbUpdateContacts, for_no_updated_emails_must_not_update_contacts_emails) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::CreateRemove);
        prepareUpdateContactsExpectations(EmailUpdateMode::CreateRemove);
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetEmailIdsTagIdsByEmailIds{uid, userType, makeRemovedEmailIds(
                EmailUpdateMode::CreateRemove)})).WillOnce(Return(badRequest));
        testWithBadRequestResult(context, makeUpdatedContacts());
    });
}

TEST_F(TestLogicDbUpdateContacts, for_no_removed_emails_must_not_remove_contacts_emails) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateCreate);
        prepareUpdateContactsExpectations(EmailUpdateMode::UpdateCreate);
        prepareUpdateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreate);
        prepareBaseCreateContactsEmailsExpectations(context);
        EXPECT_CALL(providerMock, request(CreateContactsEmails{uid, userType, makeNewContactsEmails(
                EmailUpdateMode::UpdateCreate), requestId})).WillOnce(Return(badRequest));
        testWithBadRequestResult(context, makeUpdatedContacts());
    });
}

TEST_F(TestLogicDbUpdateContacts,
        for_UntagContactsAndContactsEmails_for_email_untagging_which_ended_with_error_must_return_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareGetEmailIdsTagIdsByEmailIdsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetContactsIdsByContactsEmailIds{uid, userType, {emailId}})).
                WillOnce(Return(badRequest));
        testWithBadRequestResult(context, makeUpdatedContacts());
    });
}

TEST_F(TestLogicDbUpdateContacts, for_RemoveContactsEmails_which_ended_with_error_must_return_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareGetEmailIdsTagIdsByEmailIdsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareUntagContactsAndContactsEmailsExpectationsForEmailUntagging(context);
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, context()).WillOnce(Return(context));
        EXPECT_CALL(providerMock, request(RemoveContactsEmails{uid, userType, makeRemovedEmailIds(
                EmailUpdateMode::UpdateCreateRemove), requestId})).WillOnce(Return(badRequest));
        testWithBadRequestResult(context, makeUpdatedContacts());
    });
}

TEST_F(TestLogicDbUpdateContacts, for_no_new_emails_must_not_create_contacts_emails) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateRemove);
        prepareUpdateContactsExpectations(EmailUpdateMode::UpdateRemove);
        prepareUpdateContactsEmailsExpectations(context, EmailUpdateMode::UpdateRemove);
        prepareGetEmailIdsTagIdsByEmailIdsExpectations(context, EmailUpdateMode::UpdateRemove);
        prepareUntagContactsAndContactsEmailsExpectationsForEmailUntagging(context);
        prepareRemoveContactsEmailsExpectations(context, EmailUpdateMode::UpdateRemove);
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactsIds{uid, userType, {contactId}})).
                WillOnce(Return(badRequest));
        testWithBadRequestResult(context, makeUpdatedContactsWithTagIdsToAdd());
    });
}

TEST_F(TestLogicDbUpdateContacts,
        for_no_tagged_and_untagged_contacts_must_not_tag_and_untag_contacts_and_contacts_emails) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareGetEmailIdsTagIdsByEmailIdsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareUntagContactsAndContactsEmailsExpectationsForEmailUntagging(context);
        prepareRemoveContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareCreateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareFinalExpectations();
        testWithCorrectResult(context, makeUpdatedContacts());
    });
}

TEST_F(TestLogicDbUpdateContacts,
        for_TagContactsAndContactsEmails_which_ended_with_error_must_return_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareGetEmailIdsTagIdsByEmailIdsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareUntagContactsAndContactsEmailsExpectationsForEmailUntagging(context);
        prepareRemoveContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareCreateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactsIds{uid, userType, {contactId}})).
                WillOnce(Return(badRequest));
        testWithBadRequestResult(context, makeUpdatedContactsWithTagIdsToAdd());
    });
}

TEST_F(TestLogicDbUpdateContacts,
        for_UntagContactsAndContactsEmails_for_contact_untagging_which_ended_with_error_must_return_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareGetEmailIdsTagIdsByEmailIdsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareUntagContactsAndContactsEmailsExpectationsForEmailUntagging(context);
        prepareRemoveContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareCreateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareTagContactsAndContactsEmailsExpectations(context);
        prepareBaseUserExpectations();
        EXPECT_CALL(providerMock, request(GetContactsEmailIdsByContactsIds{uid, userType, {contactId}})).
                WillOnce(Return(badRequest));
        testWithBadRequestResult(context, makeUpdatedContactsWithTagIdsToAddAndRemove());
    });
}

TEST_F(TestLogicDbUpdateContacts,
        for_tagged_and_untagged_contacts_must_tag_and_untag_contacts_and_contacts_emails) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareBaseUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsExpectations(EmailUpdateMode::UpdateCreateRemove);
        prepareUpdateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareGetEmailIdsTagIdsByEmailIdsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareUntagContactsAndContactsEmailsExpectationsForEmailUntagging(context);
        prepareRemoveContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareCreateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateRemove);
        prepareTagContactsAndContactsEmailsExpectations(context);
        prepareUntagContactsAndContactsEmailsExpectationsForContactUntagging(context);
        prepareFinalExpectations();
        testWithCorrectResult(context, makeUpdatedContactsWithTagIdsToAddAndRemove());
    });
}

TEST_F(TestLogicDbUpdateContacts, for_in_chunks_request_must_process_contacts_emails_in_chunks) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        prepareLocalUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateCreateContact0);
        prepareUpdateContactsExpectations(EmailUpdateMode::UpdateCreateContact0);
        prepareUpdateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateContact0Chunk0);
        prepareUpdateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateContact0Chunk1);
        prepareCreateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateContact0Chunk0);
        prepareCreateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateContact0Chunk1);
        prepareLocalUpdateContactsExpectations(context);
        prepareGetContactsEmailIdsByContactsIdsExpectations(EmailUpdateMode::UpdateCreateContact1);
        prepareUpdateContactsExpectations(EmailUpdateMode::UpdateCreateContact1);
        prepareUpdateContactsEmailsExpectations(context, EmailUpdateMode::UpdateCreateContact1Chunk0);
        testInChunks(makeUpdatedContacts());
    });
}

}
