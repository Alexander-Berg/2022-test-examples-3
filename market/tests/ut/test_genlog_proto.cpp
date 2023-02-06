#include <market/proto/indexer/GenerationLog.pb.h>

#include <google/protobuf/message.h>

#include <library/cpp/testing/unittest/gtest.h>

using google::protobuf::Descriptor;
using google::protobuf::FieldDescriptor;

TEST(GenlogProto, Fixed) {
    const Descriptor* descriptor = MarketIndexer::GenerationLog::Record::GetDescriptor();
    for (auto i = 0; i < descriptor->field_count(); ++i) {
        const FieldDescriptor* field = descriptor->FindFieldByNumber(i);
        if (!field) {
            continue;
        }
        ASSERT_NE(field->type(), FieldDescriptor::Type::TYPE_FIXED64);
        ASSERT_NE(field->type(), FieldDescriptor::Type::TYPE_FIXED32);
        ASSERT_NE(field->type(), FieldDescriptor::Type::TYPE_SFIXED32);
        ASSERT_NE(field->type(), FieldDescriptor::Type::TYPE_SFIXED64);
    }
}

TEST(GenlogProto, Repeated) {
    const Descriptor* descriptor = MarketIndexer::GenerationLog::Record::GetDescriptor();
    for (auto i = 0; i < descriptor->field_count(); ++i) {
        const FieldDescriptor* field = descriptor->FindFieldByNumber(i);
        if (!field) {
            continue;
        }
        ASSERT_TRUE(not field->is_packable() or field->is_packed());
    }
}

TEST(GenlogProto, Required) {
    const Descriptor* descriptor = MarketIndexer::GenerationLog::Record::GetDescriptor();
    for (auto i = 0; i < descriptor->field_count(); ++i) {
        const FieldDescriptor* field = descriptor->FindFieldByNumber(i);
        if (!field) {
            continue;
        }
        ASSERT_FALSE(field->is_required());
    }
}

TEST(GenlogProto, SnippetType) {
    const Descriptor* descriptor = MarketIndexer::GenerationLog::Record::GetDescriptor();
    for (auto i = 0; i < descriptor->field_count(); ++i) {
        const FieldDescriptor* field = descriptor->FindFieldByNumber(i);
        if (!field) {
            continue;
        }
        if (!field->options().HasExtension(Market::idx_snippet)) {
            continue;
        }
        ASSERT_TRUE(field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_INT32 ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_INT64 ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_UINT32 ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_UINT64 ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_DOUBLE ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_FLOAT ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_BOOL ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_ENUM ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_STRING);
    }
}

TEST(GenlogProto, GroupAttrType) {
    const Descriptor* descriptor = MarketIndexer::GenerationLog::Record::GetDescriptor();
    for (auto i = 0; i < descriptor->field_count(); ++i) {
        const FieldDescriptor* field = descriptor->FindFieldByNumber(i);
        if (!field) {
            continue;
        }
        if (!field->options().HasExtension(Market::idx_group_attr)) {
            continue;
        }
        ASSERT_TRUE(field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_INT32 ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_UINT32 ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_UINT64 ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_ENUM ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_STRING ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_BOOL);
    }
}

TEST(GenlogProto, SearchLiteralType) {
    const Descriptor* descriptor = MarketIndexer::GenerationLog::Record::GetDescriptor();
    for (auto i = 0; i < descriptor->field_count(); ++i) {
        const FieldDescriptor* field = descriptor->FindFieldByNumber(i);
        if (!field) {
            continue;
        }
        if (!field->options().HasExtension(Market::idx_search_literal)) {
            continue;
        }
        ASSERT_TRUE(field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_INT32 ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_INT64 ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_UINT32 ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_UINT64 ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_DOUBLE ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_FLOAT ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_BOOL ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_ENUM ||
                    field->cpp_type() == FieldDescriptor::CppType::CPPTYPE_STRING);
    }
}
