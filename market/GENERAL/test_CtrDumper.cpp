#include <market/idx/generation/genlog_dumper/dumpers/CtrDumper.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/tempdir.h>

namespace {
    class TCtrRecordMock : public ICtrRecord {
    public:
        MOCK_METHOD(void, Write, (int ts, TFileOutput & ofs), (const override));
    };

    // N.B. We can set only fields from EXPECTED_GENLOG_FIELDS
    void AddRecord(TVector<MarketIndexer::GenerationLog::Record>& records,
                   const TString& wareMD5,
                   const ui32 ts) {
        records.emplace_back();
        MarketIndexer::GenerationLog::Record& record = records.back();
        record.set_ware_md5(wareMD5);
        record.set_ts(ts);
    }

    void RunDumper(const TVector<MarketIndexer::GenerationLog::Record>& records,
                   const TVector<std::pair<TString, TCtrRecords>>& ctrMappers) {
        auto dumper = NDumpers::MakeCtrDumper(ctrMappers);
        for (size_t i = 0; i < records.size(); ++i) {
            dumper->ProcessGenlogRecord(records[i], i);
        }
        dumper->Finish();
    }
}

Y_UNIT_TEST_SUITE(CtrDumper) {
    Y_UNIT_TEST(Match) {
        TTempDir dir;
        TVector<MarketIndexer::GenerationLog::Record> records;
        TVector<std::pair<TString, TCtrRecords>> ctrMappers;
        TCtrRecords ctrRecords;

        {
            auto mock = MakeHolder<testing::StrictMock<TCtrRecordMock>>();
            EXPECT_CALL(*mock, Write(100, testing::_));
            ctrRecords["match_md5"] = std::move(mock);
            AddRecord(records, "match_md5", 100);
        }

        ctrMappers.emplace_back(dir.Path() / "some_file", std::move(ctrRecords));
        RunDumper(records, ctrMappers);
    }

    Y_UNIT_TEST(NotMatch) {
        TTempDir dir;
        TVector<MarketIndexer::GenerationLog::Record> records;
        TVector<std::pair<TString, TCtrRecords>> ctrMappers;
        TCtrRecords ctrRecords;

        ctrRecords["not_match_md5"] = MakeHolder<testing::StrictMock<TCtrRecordMock>>();
        AddRecord(records, "some_md5", 100);

        ctrMappers.emplace_back(dir.Path() / "some_file", std::move(ctrRecords));
        RunDumper(records, ctrMappers);
    }

    Y_UNIT_TEST(MatchAndNotMatch) {
        TTempDir dir;
        TVector<MarketIndexer::GenerationLog::Record> records;
        TVector<std::pair<TString, TCtrRecords>> ctrMappers;
        TCtrRecords ctrRecords;

        ctrRecords["not_match_md5"] = MakeHolder<testing::StrictMock<TCtrRecordMock>>();
        {
            auto mock = MakeHolder<testing::StrictMock<TCtrRecordMock>>();
            EXPECT_CALL(*mock, Write(100, testing::_));
            ctrRecords["match_md5"] = std::move(mock);
            AddRecord(records, "match_md5", 100);
        }

        ctrMappers.emplace_back(dir.Path() / "some_file", std::move(ctrRecords));
        RunDumper(records, ctrMappers);
    }

    Y_UNIT_TEST(MatchTwoFiles) {
        TTempDir dir;
        TVector<MarketIndexer::GenerationLog::Record> records;
        TVector<std::pair<TString, TCtrRecords>> ctrMappers;
        TCtrRecords firstCtrRecords, secondCtrRecords;

        AddRecord(records, "match_md5", 100);
        {
            auto mock = MakeHolder<testing::StrictMock<TCtrRecordMock>>();
            EXPECT_CALL(*mock, Write(100, testing::_));
            firstCtrRecords["match_md5"] = std::move(mock);
        }
        {
            auto mock = MakeHolder<testing::StrictMock<TCtrRecordMock>>();
            EXPECT_CALL(*mock, Write(100, testing::_));
            secondCtrRecords["match_md5"] = std::move(mock);
        }
        AddRecord(records, "not_match_md5", 101);

        ctrMappers.emplace_back(dir.Path() / "first_file", std::move(firstCtrRecords));
        ctrMappers.emplace_back(dir.Path() / "second_file", std::move(secondCtrRecords));
        RunDumper(records, ctrMappers);
    }

    Y_UNIT_TEST(MatchAndNotMatchInDifferentFiles) {
        TTempDir dir;
        TVector<MarketIndexer::GenerationLog::Record> records;
        TVector<std::pair<TString, TCtrRecords>> ctrMappers;
        TCtrRecords firstCtrRecords, secondCtrRecords;

        {
            auto mock = MakeHolder<testing::StrictMock<TCtrRecordMock>>();
            EXPECT_CALL(*mock, Write(100, testing::_));
            firstCtrRecords["match_md5"] = std::move(mock);
            AddRecord(records, "match_md5", 100);
        }
        {
            secondCtrRecords["not_match_md5"] = MakeHolder<testing::StrictMock<TCtrRecordMock>>();
        }

        ctrMappers.emplace_back(dir.Path() / "first_file", std::move(firstCtrRecords));
        ctrMappers.emplace_back(dir.Path() / "second_file", std::move(secondCtrRecords));
        auto dumper = NDumpers::MakeCtrDumper(ctrMappers);
        RunDumper(records, ctrMappers);
    }
}
