#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/services/xml.hpp>
#include <src/services/db/contacts/types/reflection/existing_contact_row.hpp>

#include <string>

BOOST_FUSION_DEFINE_STRUCT(, WithStringField,
    (std::string, value)
)

BOOST_FUSION_DEFINE_STRUCT(, WithIntField,
    (int, value)
)

BOOST_FUSION_DEFINE_STRUCT(, WithTwoStringFields,
    (std::string, a)
    (std::string, b)
)

BOOST_FUSION_DEFINE_STRUCT(, WithVectorOfStringField,
    (std::vector<std::string>, value)
)

namespace {

using namespace testing;

using collie::services::toXml;

struct TestServicesToXml : Test {};

TEST(TestServicesToXml, for_struct_with_string_field) {
    EXPECT_EQ(
        toXml(WithStringField {"foo"}, "root").str(),
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        "<root><value>foo</value></root>\n"
    );
}

TEST(TestServicesToXml, for_struct_with_int_field) {
    EXPECT_EQ(
        toXml(WithIntField {42}, "root").str(),
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        "<root><value>42</value></root>\n"
    );
}

TEST(TestServicesToXml, for_struct_with_two_string_fields) {
    EXPECT_EQ(
        toXml(WithTwoStringFields {"foo", "bar"}, "root").str(),
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        "<root><a>foo</a><b>bar</b></root>\n"
    );
}

TEST(TestServicesToXml, for_struct_with_vector_of_string_field) {
    EXPECT_EQ(
        toXml(WithVectorOfStringField {{"foo", "bar"}}, "root").str(),
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        "<root><value>foo</value><value>bar</value></root>\n"
    );
}

TEST(TestServicesToXml, for_add_request) {
    using collie::services::db::contacts::ExistingContactRow;
    using collie::services::db::contacts::ContactsEmailWithTags;
    ContactsEmailWithTags contactsEmailWithTags1;
    contactsEmailWithTags1.email_id=6;
    contactsEmailWithTags1.tag_ids=std::vector<std::int64_t>({7, 7});
    contactsEmailWithTags1.email="email1";
    ContactsEmailWithTags contactsEmailWithTags2;
    contactsEmailWithTags2.email_id=8;
    contactsEmailWithTags2.tag_ids=std::vector<std::int64_t>({9, 9});
    contactsEmailWithTags2.email="email2";
    ExistingContactRow existingContactRow;
    existingContactRow.contact_id=1;
    existingContactRow.list_id=2;
    existingContactRow.revision=3;
    existingContactRow.vcard="vcard";
    existingContactRow.tag_ids=std::vector<std::int64_t>({4, 5});
    existingContactRow.uri="uri";
    existingContactRow.emails=existingContactRow.emails=std::vector<ContactsEmailWithTags>({contactsEmailWithTags1, contactsEmailWithTags2});
    EXPECT_EQ(
        toXml(existingContactRow, "add-request").str(),
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        "<add-request>"
            "<contact_id>1</contact_id>"
            "<list_id>2</list_id>"
            "<revision>3</revision>"
            "<vcard>vcard</vcard>"
            "<tag_ids>4</tag_ids>"
            "<tag_ids>5</tag_ids>"
            "<uri>uri</uri>"
            "<emails>"
                "<email_id>6</email_id>"
                "<email>email1</email>"
                "<tag_ids>7</tag_ids>"
                "<tag_ids>7</tag_ids>"
            "</emails>"
            "<emails>"
                "<email_id>8</email_id>"
                "<email>email2</email>"
                "<tag_ids>9</tag_ids>"
                "<tag_ids>9</tag_ids>"
            "</emails>"
        "</add-request>\n"
    );
}

} // namespace
