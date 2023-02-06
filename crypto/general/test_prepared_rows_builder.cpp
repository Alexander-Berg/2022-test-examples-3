#include <crypta/lib/native/yt/dyntables/kv_schema/fields.h>
#include <crypta/lib/native/yt/dyntables/kv_schema/key_rows_builder.h>
#include <crypta/lib/native/yt/dyntables/kv_schema/key_value_rows_builder.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta;
using namespace NYtDynTables;
using namespace NFields;

namespace {
    const TVector<TString> KEYS = {"key1", "key2"};
    const TVector<TRecord> RECORDS = {
            {"key1", "value1"},
            {"key2", "value2"}
    };

    void CheckField(const NYT::NTableClient::TUnversionedValue& unversionedValue, EKvSchemaId schemaId, const TString& refValue) {
        UNIT_ASSERT_EQUAL(schemaId, unversionedValue.Id);
        UNIT_ASSERT_EQUAL(NYT::NTableClient::EValueType::String, unversionedValue.Type);
        UNIT_ASSERT_EQUAL(refValue, unversionedValue.AsString());
    }

    void CheckKeyRow(const NYT::NTableClient::TUnversionedRow& row, const TString& refKey) {
        UNIT_ASSERT_EQUAL(1, row.GetCount());
        CheckField(row[0], EKvSchemaId::Key, refKey);
    }

    void CheckKeyValueRow(const NYT::NTableClient::TUnversionedRow& row, const TString& refKey, const TString& refValue) {
        UNIT_ASSERT_EQUAL(2, row.GetCount());
        CheckField(row[0], EKvSchemaId::Key, refKey);
        CheckField(row[1], EKvSchemaId::Value, refValue);
    }

    void CheckSchema(const NYT::NTableClient::TNameTable& nameTable) {
        UNIT_ASSERT_EQUAL(KEY, nameTable.GetName(EKvSchemaId::Key));
        UNIT_ASSERT_EQUAL(VALUE, nameTable.GetName(EKvSchemaId::Value));
    }
}

Y_UNIT_TEST_SUITE(PreparedRowsBuilder) {
    Y_UNIT_TEST(Key) {
        const auto& preparedRows = TKeyRowsBuilder().AddRow(KEY).Build();

        CheckSchema(*preparedRows.NameTable);

        UNIT_ASSERT_EQUAL(1, preparedRows.Rows.Size());
        CheckKeyRow(*preparedRows.Rows.Begin(), KEY);
    }

    Y_UNIT_TEST(MultipleKeys) {
        TKeyRowsBuilder builder;
        for (const auto& key: KEYS) {
            builder.AddRow(key);
        }

        const auto& preparedRows = builder.Build();

        CheckSchema(*preparedRows.NameTable);

        UNIT_ASSERT_EQUAL(2, preparedRows.Rows.Size());
        for (size_t i = 0; i < KEYS.size(); ++i) {
            CheckKeyRow(preparedRows.Rows[i], KEYS.at(i));
        }
    }

    Y_UNIT_TEST(KeyValue) {
        const auto& preparedRows = TKeyValueRowsBuilder().AddRow(KEY, VALUE).Build();

        CheckSchema(*preparedRows.NameTable);

        UNIT_ASSERT_EQUAL(1, preparedRows.Rows.Size());
        CheckKeyValueRow(*preparedRows.Rows.Begin(), KEY, VALUE);
    }

    Y_UNIT_TEST(MultipleKeyValues) {
        TKeyValueRowsBuilder builder;
        for (const auto& record: RECORDS) {
            builder.AddRow(record.Key, record.Value);
        }

        const auto& preparedRows = builder.Build();

        CheckSchema(*preparedRows.NameTable);

        UNIT_ASSERT_EQUAL(2, preparedRows.Rows.Size());
        for (size_t i = 0; i < RECORDS.size(); ++i) {
            CheckKeyValueRow(preparedRows.Rows[i], RECORDS.at(i).Key, RECORDS.at(i).Value);
        }
    }
}
