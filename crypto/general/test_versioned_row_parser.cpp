#include <crypta/lib/native/yt/dyntables/kv_schema/versioned_row_parser.h>
#include <crypta/lib/native/yt/dyntables/kv_schema/fields.h>

#include <yt/yt/client/table_client/row_buffer.h>
#include <yt/yt/client/table_client/versioned_row.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta;
using namespace NCrypta::NYtDynTables;
using namespace NFields;
using namespace NYT::NTableClient;

namespace {
    const ui64 HASH_VALUE = 100500;
    const TString KEY_VALUE = "v_key";
    const TString VALUE_VALUE = "v_value";
    const TTimestamp TIMESTAMP_VALUE = NYT::NTransactionClient::MaxTimestamp;
}

Y_UNIT_TEST_SUITE(VersionedRowParser) {
    void WithParserAndBuilder(std::function<void(TVersionedRowParser&, TVersionedRowBuilder&)> testBody) {
        TVersionedRowParser parser;

        auto rowBuffer = NYT::New<TRowBuffer>();
        NYT::NTableClient::TVersionedRowBuilder builder(rowBuffer);

        testBody(parser, builder);
    }

    Y_UNIT_TEST(Positive) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddKey(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
            builder.AddKey(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));
            builder.AddValue(MakeVersionedStringValue(VALUE_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Value));

            const auto& row = builder.FinishRow();
            const auto& record = parser.Parse(row);

            UNIT_ASSERT_EQUAL(KEY_VALUE, record.Key);
            UNIT_ASSERT_EQUAL(VALUE_VALUE, record.Value);
            UNIT_ASSERT_EQUAL(TRecord::NO_CAS, record.Cas);
        });
    }

    Y_UNIT_TEST(MissingKey) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddKey(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
            builder.AddValue(MakeVersionedStringValue(VALUE_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Value));

            const auto& row = builder.FinishRow();
            UNIT_ASSERT_EXCEPTION_CONTAINS(parser.Parse(row), yexception, "Key or/and value is not defined");
        });
    }

    Y_UNIT_TEST(MissingValue) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddKey(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
            builder.AddKey(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));

            const auto& row = builder.FinishRow();
            UNIT_ASSERT_EXCEPTION_CONTAINS(parser.Parse(row), yexception, "Key or/and value is not defined");
        });
    }

    Y_UNIT_TEST(MissingHashIsOk) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddKey(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));
            builder.AddValue(MakeVersionedStringValue(VALUE_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Value));

            const auto& row = builder.FinishRow();
            const auto& record = parser.Parse(row);

            UNIT_ASSERT_EQUAL(KEY_VALUE, record.Key);
            UNIT_ASSERT_EQUAL(VALUE_VALUE, record.Value);
            UNIT_ASSERT_EQUAL(TRecord::NO_CAS, record.Cas);
        });
    }

    Y_UNIT_TEST(InvalidType) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddKey(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
            builder.AddKey(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Key));
            builder.AddValue(MakeVersionedStringValue(VALUE_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Value));

            const auto& row = builder.FinishRow();
            UNIT_ASSERT_EXCEPTION(parser.Parse(row), NYT::TErrorException);
        });
    }

    Y_UNIT_TEST(InvalidId) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddKey(MakeUnversionedUint64Value(HASH_VALUE, 9999));
            builder.AddKey(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));
            builder.AddValue(MakeVersionedStringValue(VALUE_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Value));

            const auto& row = builder.FinishRow();
            UNIT_ASSERT_EXCEPTION(parser.Parse(row), std::out_of_range);
        });
    }

    Y_UNIT_TEST(MixedIds) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddKey(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Key));
            builder.AddKey(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Hash));
            builder.AddValue(MakeVersionedStringValue(VALUE_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Value));

            const auto& row = builder.FinishRow();
            UNIT_ASSERT_EXCEPTION(parser.Parse(row), NYT::TErrorException);
        });
    }

    Y_UNIT_TEST(DuplicateKey) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddKey(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
            builder.AddKey(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));
            builder.AddKey(MakeUnversionedStringValue("another_key", EKvSchemaId::Key));
            builder.AddValue(MakeVersionedStringValue(VALUE_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Value));

            const auto& row = builder.FinishRow();
            UNIT_ASSERT_EXCEPTION_CONTAINS(parser.Parse(row), yexception, "Duplicate key found");
        });
    }

    Y_UNIT_TEST(DuplicateValue) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddKey(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
            builder.AddKey(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));
            builder.AddValue(MakeVersionedStringValue(VALUE_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Value));
            builder.AddValue(MakeVersionedStringValue("another_value", TIMESTAMP_VALUE, EKvSchemaId::Value));

            const auto& row = builder.FinishRow();
            const auto& record = parser.Parse(row);
            UNIT_ASSERT_EQUAL(KEY_VALUE, record.Key);
            UNIT_ASSERT_EQUAL(VALUE_VALUE, record.Value);
            UNIT_ASSERT_EQUAL(TRecord::NO_CAS, record.Cas);
        });
    }

    Y_UNIT_TEST(VersionedHashIsNotOk) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddValue(MakeVersionedUint64Value(HASH_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Hash));
            builder.AddKey(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));
            builder.AddValue(MakeVersionedStringValue(VALUE_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Value));

            const auto& row = builder.FinishRow();
            UNIT_ASSERT_EXCEPTION_CONTAINS(parser.Parse(row), yexception, "Unknown field in values, id = 0");
        });
    }

    Y_UNIT_TEST(VersionedKeyIsNotOk) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddKey(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
            builder.AddValue(MakeVersionedStringValue(KEY_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Key));
            builder.AddValue(MakeVersionedStringValue(VALUE_VALUE, TIMESTAMP_VALUE, EKvSchemaId::Value));

            const auto& row = builder.FinishRow();
            UNIT_ASSERT_EXCEPTION_CONTAINS(parser.Parse(row), yexception, "Unknown field in values, id = 1");
        });
    }

    Y_UNIT_TEST(UnversionedValueIsNotOk) {
        WithParserAndBuilder([](TVersionedRowParser& parser, TVersionedRowBuilder& builder) {
            builder.AddKey(MakeUnversionedUint64Value(HASH_VALUE, EKvSchemaId::Hash));
            builder.AddKey(MakeUnversionedStringValue(KEY_VALUE, EKvSchemaId::Key));
            builder.AddKey(MakeUnversionedStringValue(VALUE_VALUE, EKvSchemaId::Value));

            const auto& row = builder.FinishRow();
            UNIT_ASSERT_EXCEPTION_CONTAINS(parser.Parse(row), yexception, "Unknown field in keys, id = 2");
        });
    }
}
