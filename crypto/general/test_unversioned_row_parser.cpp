#include <crypta/lib/native/yt/dyntables/kv_schema/unversioned_row_parser.h>
#include <crypta/lib/native/yt/dyntables/kv_schema/fields.h>

#include <yt/yt/client/table_client/unversioned_row.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta;
using namespace NCrypta::NYtDynTables;
using namespace NFields;
using namespace NYT::NTableClient;

namespace {
    const ui64 HASH_VALUE = 123456;
    const TString KEY_VALUE = "my_key";
    const TString VALUE_VALUE = "my_value";
}

Y_UNIT_TEST_SUITE(UnversionedRowParser) {
    Y_UNIT_TEST(Positive) {
        TUnversionedRowParser parser;

        TUnversionedOwningRowBuilder builder;

        builder.AddValue(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
        builder.AddValue(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));
        builder.AddValue(MakeUnversionedStringValue(VALUE_VALUE, EKvSchemaId::Value));

        const auto& row = builder.FinishRow();
        const auto& record = parser.Parse(row);

        UNIT_ASSERT_EQUAL(KEY_VALUE, record.Key);
        UNIT_ASSERT_EQUAL(VALUE_VALUE, record.Value);
        UNIT_ASSERT_EQUAL(TRecord::NO_CAS, record.Cas);
    }

    Y_UNIT_TEST(MissingKey) {
        TUnversionedRowParser parser;

        TUnversionedOwningRowBuilder builder;

        builder.AddValue(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
        builder.AddValue(MakeUnversionedStringValue(VALUE_VALUE, EKvSchemaId::Value));

        const auto& row = builder.FinishRow();
        UNIT_ASSERT_EXCEPTION_CONTAINS(parser.Parse(row), yexception, "Key or/and value is not defined");
    }

    Y_UNIT_TEST(MissingValue) {
        TUnversionedRowParser parser;

        TUnversionedOwningRowBuilder builder;

        builder.AddValue(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
        builder.AddValue(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));

        const auto& row = builder.FinishRow();
        UNIT_ASSERT_EXCEPTION_CONTAINS(parser.Parse(row), yexception, "Key or/and value is not defined");
    }

    Y_UNIT_TEST(MissingHashIsOk) {
        TUnversionedRowParser parser;

        TUnversionedOwningRowBuilder builder;

        builder.AddValue(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));
        builder.AddValue(MakeUnversionedStringValue(VALUE_VALUE, EKvSchemaId::Value));

        const auto& row = builder.FinishRow();
        const auto& record = parser.Parse(row);

        UNIT_ASSERT_EQUAL(KEY_VALUE, record.Key);
        UNIT_ASSERT_EQUAL(VALUE_VALUE, record.Value);
        UNIT_ASSERT_EQUAL(TRecord::NO_CAS, record.Cas);
    }

    Y_UNIT_TEST(InvalidType) {
        TUnversionedRowParser parser;

        TUnversionedOwningRowBuilder builder;

        builder.AddValue(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
        builder.AddValue(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Key));
        builder.AddValue(MakeUnversionedStringValue(VALUE_VALUE, EKvSchemaId::Value));

        const auto& row = builder.FinishRow();
        UNIT_ASSERT_EXCEPTION(parser.Parse(row), NYT::TErrorException);
    }

    Y_UNIT_TEST(InvalidId) {
        TUnversionedRowParser parser;

        TUnversionedOwningRowBuilder builder;

        builder.AddValue(MakeUnversionedUint64Value(HASH_VALUE, 9999));
        builder.AddValue(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));
        builder.AddValue(MakeUnversionedStringValue(VALUE_VALUE, EKvSchemaId::Value));

        const auto& row = builder.FinishRow();
        UNIT_ASSERT_EXCEPTION(parser.Parse(row), std::out_of_range);
    }

    Y_UNIT_TEST(MixedIds) {
        TUnversionedRowParser parser;

        TUnversionedOwningRowBuilder builder;

        builder.AddValue(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Key));
        builder.AddValue(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Hash));
        builder.AddValue(MakeUnversionedStringValue(VALUE_VALUE, EKvSchemaId::Value));

        const auto& row = builder.FinishRow();
        UNIT_ASSERT_EXCEPTION(parser.Parse(row), NYT::TErrorException);
    }

    Y_UNIT_TEST(DuplicateIds) {
        TUnversionedRowParser parser;

        TUnversionedOwningRowBuilder builder;

        builder.AddValue(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
        builder.AddValue(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));
        builder.AddValue(MakeUnversionedStringValue(VALUE_VALUE, EKvSchemaId::Value));
        builder.AddValue(MakeUnversionedStringValue("another_value", EKvSchemaId::Value));

        const auto& row = builder.FinishRow();
        UNIT_ASSERT_EXCEPTION(parser.Parse(row), yexception);
    }
}
