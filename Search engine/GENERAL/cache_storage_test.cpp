#include "cache_storage.h"

#include <search/plutonium/helpers/hasher/calc_hash.h>
#include <search/plutonium/impl/fs_cache/proto/file_entry_meta.pb.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/folder/tempdir.h>
#include <util/generic/map.h>
#include <util/stream/file.h>
#include <util/string/subst.h>
#include <util/system/mutex.h>

#include <thread>

namespace NPlutonium::NFsCache {

bool CheckCounter(TStringBuf, ui64 v1, ui64 v2) {
    return v1 == v2;
}

struct TMockLoader {
    void Fetch(const TString& path, const TMaybe<TString>& customContent = Nothing(), TDuration loadDelay = TDuration::Zero()) {
        if (loadDelay != TDuration::Zero()) {
            Sleep(loadDelay);
        }

        const TString name = TFsPath{path}.Basename();
        const TString content = customContent.Defined() ? *customContent : name;

        IncrementCounter(name);

        TFileOutput out(path);
        out << content;
        out.Finish();
    }

    void ExpectCounter(const TString& fileId, ui64 expectedCount) const {
        const ui64* cnt = Counters_.FindPtr(fileId);
        ui64 actualCount = cnt != nullptr ? *cnt : 0;
        ASSERT_PRED3(CheckCounter, fileId, expectedCount, actualCount);
    }

private:
    void IncrementCounter(const TString& key) {
        auto g = Guard(CountersMutex_);
        auto inserted = Counters_.emplace(key, 1);
        if (!inserted.second) {
            ++inserted.first->second;
        }
    }

    TMutex CountersMutex_;
    TMap<TString, ui64> Counters_;
};

TFileEntryMeta GenTestFileMeta(const TString& fileId, const TString& content) {
    TFileEntryMeta meta;
    meta.SetFileId(fileId);
    meta.SetHash(CalcHash(content));
    meta.SetSize(content.size());

    return meta;
}

struct TTestFile {
    const TString Id;
    const TString Content;
    const TFileEntryMeta Meta;
    const TDuration LoadDelay;

    TTestFile(TString id, TDuration loadDelay = TDuration::Zero())
        : Id(id)
        , Content(std::move(id))
        , Meta(GenTestFileMeta(Id, Content))
        , LoadDelay(loadDelay)
    {
    }

    TTestFile(TString id, TString content, TDuration loadDelay = TDuration::Zero())
        : Id(std::move(id))
        , Content(std::move(content))
        , Meta(GenTestFileMeta(Id, Content))
        , LoadDelay(loadDelay)
    {
    }
};

void CheckBlob(const TBlob& blob, TStringBuf expectedContent) {
    ASSERT_FALSE(blob.IsNull());
    TStringBuf content{blob.AsCharPtr(), blob.Size()};
    ASSERT_EQ(content, expectedContent);
}

struct TCacheStorageTest : ::testing::Test {
    const TFileAllocatorConfig& Config() {
        return Config_;
    }
    void SetCacheConfigLimits(ui64 maxByteSize, ui32 maxFileCount) {
        Config_.SizeCapacity = maxByteSize;
        Config_.MaxFileCount = maxFileCount;
    }

    TFileLoader Loader() {
        return [this](const auto& path) {
            Mock_.Fetch(path);
        };
    }
    TFileLoader Loader(const TTestFile& f) {
        return [this, content = f.Content, loadDelay = f.LoadDelay](const auto& path) {
            Mock_.Fetch(path, content, loadDelay);
        };
    }

    const TMockLoader& Mock() {
        return Mock_;
    }

    void SetUp() override {
        TempDir_.ConstructInPlace();
        Config_.DirPath = TempDir_->Path();
        TVector<TString> children;
        Config_.DirPath.ListNames(children);
        ASSERT_TRUE(children.empty());
        Config_.SignalCollector = MakeAtomicShared<TBlackHole>();
    }

    void TearDown() override {
        TempDir_.Clear();
    }

private:
    TMaybe<TTempDir> TempDir_;
    TFileAllocatorConfig Config_;
    TMockLoader Mock_;
};

TEST_F(TCacheStorageTest, DoubleDoubleRead) {
    const TTestFile file1("my_file_id");

    TMaybe<TCacheStorage> cacheStorage;
    cacheStorage.ConstructInPlace(Config());

    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 1);

    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 1);

    // recreate cache
    cacheStorage.ConstructInPlace(Config());

    auto blob1 = cacheStorage->GetFileBlob(file1.Meta, {}, Loader());
    CheckBlob(blob1, file1.Content);
    Mock().ExpectCounter(file1.Id, 1);

    auto blob2 = cacheStorage->GetFileBlob(file1.Meta, {}, Loader());
    CheckBlob(blob2, file1.Content);
    Mock().ExpectCounter(file1.Id, 1);
}

TEST_F(TCacheStorageTest, WithLimits) {
    const TTestFile file1("file_id_11");
    const TTestFile file2("file_id_222");

    TMaybe<TCacheStorage> cacheStorage;
    auto tryLoad = [&](const TTestFile& f) {
        Y_UNUSED(cacheStorage->GetFileBlob(f.Meta, {}, Loader()));
    };

    SetCacheConfigLimits(Max<ui64>(), 0);
    cacheStorage.ConstructInPlace(Config());

    ASSERT_ANY_THROW(tryLoad(file1));
    Mock().ExpectCounter(file1.Id, 0);

    SetCacheConfigLimits(0, Max<ui32>());
    cacheStorage.ConstructInPlace(Config());

    ASSERT_ANY_THROW(tryLoad(file1));
    Mock().ExpectCounter(file1.Id, 0);

    SetCacheConfigLimits(Max(file1.Id.size(), file2.Id.size()), Max<ui32>());
    cacheStorage.ConstructInPlace(Config());

    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 1);
    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 1);

    CheckBlob(cacheStorage->GetFileBlob(file2.Meta, {}, Loader()), file2.Content);
    Mock().ExpectCounter(file2.Id, 1);
    CheckBlob(cacheStorage->GetFileBlob(file2.Meta, {}, Loader()), file2.Content);
    Mock().ExpectCounter(file2.Id, 1);

    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 2);
    TBlob blob2 = cacheStorage->GetFileBlob(file2.Meta, {}, Loader());
    CheckBlob(blob2, file2.Content);
    Mock().ExpectCounter(file2.Id, 2);

    // blob2 is still out there - thus its underlying file cannot be removed
    ASSERT_ANY_THROW(tryLoad(file1));
    Mock().ExpectCounter(file1.Id, 2);

    SetCacheConfigLimits(file1.Id.size() + file2.Id.size(), 2);
    cacheStorage.ConstructInPlace(Config());

    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 3);
    CheckBlob(cacheStorage->GetFileBlob(file2.Meta, {}, Loader()), file2.Content);
    Mock().ExpectCounter(file2.Id, 2);
    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 3);
}

TEST_F(TCacheStorageTest, RemoveJunkOnStart) {
    const TTestFile file1("my_file_id");

    TMaybe<TCacheStorage> cacheStorage;
    cacheStorage.ConstructInPlace(Config());

    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 1);

    cacheStorage.Clear();

    // put some junk into the cache dir
    (Config().DirPath / "some_dir").MkDirs();
    TFileOutput(Config().DirPath / "junk_file");
    TFileOutput(Config().DirPath / "junk.meta");

    // recreate cache
    cacheStorage.ConstructInPlace(Config());

    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 1);
}

TEST_F(TCacheStorageTest, FetchAfterVerify) {
    const TTestFile file1("my_file_id");

    TMaybe<TCacheStorage> cacheStorage;
    cacheStorage.ConstructInPlace(Config());

    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 1);

    cacheStorage.Clear();

    // corrupt file
    {
        TFileOutput f(Config().DirPath / file1.Id);
        f << SubstGlobalCopy(file1.Content, '_', '-');
    }

    // recreate cache
    cacheStorage.ConstructInPlace(Config());

    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 2);

    const TTestFile file2(file1.Id, "new_content");
    CheckBlob(cacheStorage->GetFileBlob(file2.Meta, {}, Loader(file2)), file2.Content);
    Mock().ExpectCounter(file2.Id, 3);
}

TEST_F(TCacheStorageTest, ParallelRead) {
    const TTestFile file1("my_file_id", TDuration::Seconds(1));

    TMaybe<TCacheStorage> cacheStorage;
    cacheStorage.ConstructInPlace(Config());

    TFileLoader loader = Loader(file1);
    TBlob bgBlob;
    TString bgError;
    auto loadBlob = [&cacheStorage, loader, &file1, &bgBlob, &bgError]()  {
        try {
            bgBlob = cacheStorage->GetFileBlob(file1.Meta, {}, loader);
        } catch (...) {
            bgError = CurrentExceptionMessage();
        }
    };

    std::thread bgThread(loadBlob);
    TBlob mainBlob = cacheStorage->GetFileBlob(file1.Meta, {}, loader);
    bgThread.join();

    ASSERT_EQ(bgError, "");

    // one of the threads is expected to wait another
    Mock().ExpectCounter(file1.Id, 1);

    CheckBlob(bgBlob, file1.Content);
    CheckBlob(mainBlob, file1.Content);
}

TEST_F(TCacheStorageTest, TryToGetFileBlob) {
    const TTestFile file1("my_file_id");

    TMaybe<TCacheStorage> cacheStorage;
    cacheStorage.ConstructInPlace(Config());

    TBlob blob = cacheStorage->TryToGetFileBlob(file1.Meta, {});
    ASSERT_TRUE(blob.IsNull());
    Mock().ExpectCounter(file1.Id, 0);

    CheckBlob(cacheStorage->GetFileBlob(file1.Meta, {}, Loader()), file1.Content);
    Mock().ExpectCounter(file1.Id, 1);

    TBlob blob2 = cacheStorage->TryToGetFileBlob(file1.Meta, {});
    ASSERT_FALSE(blob2.IsNull());
    CheckBlob(blob2, file1.Content);
    Mock().ExpectCounter(file1.Id, 1);
}

}
