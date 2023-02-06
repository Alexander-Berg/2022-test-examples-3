#include <extsearch/images/robot/mrdb/library/imagedb/read_kiwi_rec.h>

#include <extsearch/images/robot/library/mrstream/mr.h>
#include <extsearch/images/robot/library/kiwi/record_helper.h>

#include <extsearch/images/chunks/md5chunk.h>
#include <extsearch/images/chunks/thumbnail.h>
#include <extsearch/images/chunks/thumbnailinfo.h>
#include <extsearch/images/chunks/imageproperties.h>
#include <extsearch/images/chunks/thumbproperties.h>
#include <extsearch/images/chunks/thumbidchunk.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <util/stream/file.h>
#include <util/folder/path.h>

class TTesterExportData {
private:
    TVector<TFsPath> Files;

    TVector<int> RequiredIds;
    TVector<int> ForbidenIds;

public:
    TTesterExportData(const TFsPath& dir) {
        dir.List(Files);
    }

    ui32 TablesInTest() {
        return Files.size();
    }

    void Test() {
        ApplyChecksForAllExaples([&](NImages::NImageDB::TRecordPtr record) { return DemandChunkSetPresents(RequiredIds, record); });
        ApplyChecksForAllExaples([&](NImages::NImageDB::TRecordPtr record) { return DemandChunkSetEmpty(ForbidenIds, record); });
    }

    void SetAdditionalThumbnailsIds(int thumbnailIdChunk) {
        RequiredIds = {TThumbnail::CHUNK_ID, TImagePropertiesChunk::CHUNK_ID, thumbnailIdChunk};

        ForbidenIds = {TThumbIdChunk::CHUNK_ID};
    }

    TVector<int>& AddRequiredIds(int id) {
        RequiredIds.push_back(id);
        return RequiredIds;
    }

    TVector<int>& AddForbidenIds(int id) {
        ForbidenIds.push_back(id);
        return ForbidenIds;
    }

    void SetCommonThumbnailsIds() {
        RequiredIds = {TThumbnail::CHUNK_ID, TThumbnailInfo::CHUNK_ID, TImageMD5Chunk::CHUNK_ID, TThumbIdChunk::CHUNK_ID};

        ForbidenIds = {TBigThumbPropertiesChunk::CHUNK_ID, TSResolutionThumbPropertiesChunk::CHUNK_ID};
    }

private:
    using TValueCheckFunc = std::function<void(NImages::NImageDB::TRecordPtr record)>;

    void ApplyChecksForAllExaples(TValueCheckFunc check) {
        for (const auto& fileName : Files) {
            TUnbufferedFileInput input(fileName.GetPath());
            NMr::TTableReader reader(input);

            NMr::TBuf key;
            NMr::TBuf value;
            reader.ReadRow(key, value);

            CheckKey(key);
            CheckValue(value, check);
        }
    }

    void CheckKey(const NMr::TBuf& key) {
        TKiwiKeyChunk kiwikey = FromString(TStringBuf(key.data(), key.size()));
        UNIT_ASSERT_EQUAL(32, ToString(kiwikey).size());
    }

    void CheckValue(const NMr::TBuf& value, TValueCheckFunc check) {
        NKiwiWorm::TRecord kwlogRaw;
        NImages::Read(&kwlogRaw, value);

        NImages::TRecordHelper kwlog(kwlogRaw);

        NImages::NImageDB::TRecordPtr record = NImages::NImageDB::MakeChunkedRecord(kwlog);

        check(record);
    }

    static void DemandChunkSetPresents(const TVector<int>& requiredIds, NImages::NImageDB::TRecordPtr record) {
        for (auto id : requiredIds) {
            NImages::NImageDB::TImageV2PB::TChunk* chunk = record->FindChunk(id);
            UNIT_ASSERT_UNEQUAL(chunk, nullptr);
        }
    }

    static void DemandChunkSetEmpty(const TVector<int>& forbidenIds, NImages::NImageDB::TRecordPtr record) {
        for (auto id : forbidenIds) {
            NImages::NImageDB::TImageV2PB::TChunk* chunk = record->FindChunk(id);
            UNIT_ASSERT_EQUAL(chunk, nullptr);
        }
    }
};

Y_UNIT_TEST_SUITE(TestExportData){
    Y_UNIT_TEST(SeparatedChunksV1){
        TFsPath dataDir = TFsPath(GetWorkPath()) / "separated_chunks.v1";
TTesterExportData tester(dataDir);

UNIT_ASSERT_EQUAL(1, tester.TablesInTest());

tester.SetCommonThumbnailsIds();
tester.Test();
}

Y_UNIT_TEST(SupperResolutionThumbnailsV3) {
    TFsPath dataDir = TFsPath(GetWorkPath()) / "super_resolution.v3";
    TTesterExportData tester(dataDir);

    UNIT_ASSERT_EQUAL(1, tester.TablesInTest());

    tester.SetAdditionalThumbnailsIds(TSResolutionThumbPropertiesChunk::CHUNK_ID);
    tester.Test();
}

Y_UNIT_TEST(SupperResolutionThumbnails20dot02dotV3) {
    TFsPath dataDir = TFsPath(GetWorkPath()) / "super_resolution.20.02.v1";
    TTesterExportData tester(dataDir);

    UNIT_ASSERT_EQUAL(1, tester.TablesInTest());

    tester.SetAdditionalThumbnailsIds(TSResolutionThumbPropertiesChunk::CHUNK_ID);
    tester.AddForbidenIds(TImageMD5Chunk::CHUNK_ID);
    tester.Test();
}

Y_UNIT_TEST(SupperResolutionThumbnails15dot03dotV1) {
    TFsPath dataDir = TFsPath(GetWorkPath()) / "super_resolution.15.03";
    TTesterExportData tester(dataDir);

    UNIT_ASSERT_EQUAL(1, tester.TablesInTest());

    tester.SetAdditionalThumbnailsIds(TSResolutionThumbPropertiesChunk::CHUNK_ID);
    tester.AddForbidenIds(TImageMD5Chunk::CHUNK_ID);
    tester.Test();
}

Y_UNIT_TEST(BigThumbsV1) {
    TFsPath dataDir = TFsPath(GetWorkPath()) / "big_thumbs.v1";
    TTesterExportData tester(dataDir);

    UNIT_ASSERT_EQUAL(1, tester.TablesInTest());

    tester.SetAdditionalThumbnailsIds(TBigThumbPropertiesChunk::CHUNK_ID);
    tester.Test();
}
}
;
