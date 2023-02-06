#include <crypta/lib/native/rtmr/tskv_parser_mapper/tskv_parser_mapper.h>
#include <crypta/lib/native/yt/test_utils/test_yamr_reader.h>

#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/interface/io-inl.h>
#include <mapreduce/yt/interface/io.h>

using namespace NCrypta;
using namespace NCrypta::NYtTestUtils;

namespace {
    class TTestWriter: public ::NYT::IYaMRWriterImpl {
    public:
        using TRows = TVector<TTestYaMRRow>;

        explicit TTestWriter(TRows rows)
            : ReferenceRows(std::move(rows)) {

        }
        size_t GetTableCount() const override {
            return 1;
        };

        void FinishTable(size_t) override {}

        void AddRow(const ::NYT::TYaMRRow& row, size_t tableIndex) override {
            OutputRows.emplace_back(row, tableIndex);
        };

        void AddRow(::NYT::TYaMRRow&& row, size_t tableIndex) override {
            OutputRows.emplace_back(std::move(row), tableIndex);
        }

        ~TTestWriter() override {
            UNIT_ASSERT_EQUAL(ReferenceRows.size(), OutputRows.size());
            for (size_t i = 0; i < ReferenceRows.size(); ++i) {
                Cerr << (i > 0 ? " - " : "");
                Cerr << ReferenceRows.at(i).Key << " " << ReferenceRows.at(i).Subkey << " " << ReferenceRows.at(i).Value;
                UNIT_ASSERT_EQUAL(ReferenceRows.at(i), OutputRows.at(i));
            }
        }

    private:
        TRows ReferenceRows;
        TRows OutputRows;
    };

    class TTestMapper : public TTskvParserMapper {
        void DoParsed(const ::NYT::TNode& parsed, TWriter* writer) override {
            writer->AddRow({
                GetSafe<TString>(parsed, "key"),
                GetSafe<TString>(parsed, "subkey"),
                GetSafe<TString>(parsed, "value")
            });
        }
    };

    using TReaderPtr = TIntrusivePtr<TTestMapper::TReader>;
    using TWriterPtr = TIntrusivePtr<TTestMapper::TWriter>;
    using TRows = TVector<TTestYaMRRow>;

    TReaderPtr CreateReader(const TRows& input) {
        return MakeIntrusive<TTestMapper::TReader>(MakeIntrusive<TTestYamrReader<TRows, TTestYaMRRow::TGetter>>(std::move(input)));
    }

    TWriterPtr CreateWriter(const TTestWriter::TRows& reference) {
        return MakeIntrusive<TTestMapper::TWriter>(MakeIntrusive<TTestWriter>(std::move(reference)));
    }

    void Test(TRows input, TTestWriter::TRows reference) {
        TTestMapper mapper;
        mapper.Do(
            CreateReader(input).Get(),
            CreateWriter(reference).Get()
        );
    }
}

Y_UNIT_TEST_SUITE(TTskvParserMapper) {
    Y_UNIT_TEST(Basic) {
        Test(
            {
                {"key1", "subkey1", "tskv\tkey=key_1\tsubkey=subkey_1\tvalue=value_1"},
                {"key2", "subkey2", "key=key_2\tsubkey=subkey_2\tvalue=value_2"}
            },
            {
                {"key_1", "subkey_1", "value_1"},
                {"key_2", "subkey_2", "value_2"}
            }
        );
    }

    Y_UNIT_TEST(TestUnicode) {
        Test(
            {
                {"key1", "subkey1", "tskv\tkey=key_1\tsubkey=subkey_1\tvalue=value_1\\\\u0420\\\\u0423\\\\u0421"},
                {"key2", "subkey2", "key=key_2\tsubkey=subkey_2\tvalue=value_2"}
            },
            {
                {"key_1", "subkey_1", "value_1\\u0420\\u0423\\u0421"},
                {"key_2", "subkey_2", "value_2"}
            }
        );
    }

    Y_UNIT_TEST(SkipMalformedTskv) {
        Test(
            {
                {"key1", "subkey1", "not_kv"},
                {"key2", "subkey2", "=no_key"},
                {"key3", "subkey3", "key=ok\tsubkey=ok\tvalue=ok"}
            },
            {
                {"ok", "ok", "ok"}
            }
        );
    }

    Y_UNIT_TEST(NoMergeToTNode) {
        Test(
            {
                {"key", "subkey", "key=1\tsubkey=2\tvalue=3"},
                {"key", "subkey", "key=1\tvalue=4"}
            },
            {
                {"1", "2", "3"},
                {"1", "", "4"}
            }
        );
    }
}
