#pragma once


#include <search/plutonium/core/file_system/meta_info.h>
#include <search/plutonium/helpers/hasher/hasher.h>
#include <search/plutonium/impl/file_system/separated_meta_and_content/separated_meta_and_content_file_system.h>

#include <util/folder/path.h>
#include <util/generic/guid.h>
#include <util/stream/file.h>


namespace NPlutonium {


class TLocalDiskForTestsFileSystem: public TSeparatedMetaAndContentFileSystem {
public:
    TLocalDiskForTestsFileSystem(TFileSystemTree<TMetaInfo>* tree, TFsPath dir, NFsCache::TCacheStorage cacheStorage)
        : TSeparatedMetaAndContentFileSystem(tree, std::move(cacheStorage))
        , Dir_(std::move(dir))
    {
    }

protected:
    TSimpleSharedPtr<IInputStream> CreateFileReader(const TFileSystemPath& path, TAtomicSharedPtr<const TMetaInfo> meta) const override {
        Y_UNUSED(path);
        return MakeSimpleShared<TIFStream>(Dir_ / meta->FileId);
    }

    THolder<IFileOutputStreamWithMetaInfo<TMetaInfo>> CreateFileWriter(const TFileSystemPath& path) override {
        Y_UNUSED(path);
        TGUID fileId;
        CreateGuid(&fileId);
        TMetaInfo metaInfo{
            .FileId = GetGuidAsString(fileId),
            .CreatedAt = TInstant::Now()
        };
        return MakeHolder<TOutputStreamWithMetaInfo>(Dir_ / metaInfo.FileId, metaInfo);
    }

private:
    class TOutputStreamWithMetaInfo: public IFileOutputStreamWithMetaInfo<TMetaInfo> {
    public:
        TOutputStreamWithMetaInfo(const TFsPath& path, const TMetaInfo& metaInfo)
            : Output_(path)
            , MetaInfo_(metaInfo)
        {
        }

        TMetaInfo GenerateFileMetaInfo() override {
            return MetaInfo_;
        }

    protected:
        void DoWrite(const void* buf, size_t len) override {
            Output_.Write(buf, len);
            Hasher_.Update(buf, len);
        }

        void DoFlush() override {
            Output_.Flush();
        }

        void DoFinish() override {
            Output_.Finish();
            Hasher_.Finalize();
            MetaInfo_.Hash = Hasher_.GetHumanReadable();
            MetaInfo_.Size = Hasher_.GetInputSize();
        }

    private:
        TOFStream Output_;
        TMetaInfo MetaInfo_;
        THasher Hasher_;
    };

    TFsPath Dir_;
};


}
