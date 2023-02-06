#pragma once


#include <search/plutonium/impl/file_system/separated_meta_and_content/separated_meta_and_content_read_only_file_system.h>

#include <util/folder/path.h>
#include <util/generic/guid.h>
#include <util/stream/file.h>



namespace NPlutonium {


class TLocalDiskForTestsReadOnlyFileSystem: public TSeparatedMetaAndContentReadOnlyFileSystem {
public:
    TLocalDiskForTestsReadOnlyFileSystem(const TFileSystemTree<TMetaInfo>* tree, TFsPath dir, NFsCache::TCacheStorage cacheStorage)
        : TSeparatedMetaAndContentReadOnlyFileSystem(tree, std::move(cacheStorage))
        , Dir_(std::move(dir))
    {
    }

protected:
    TSimpleSharedPtr<IInputStream> CreateFileReader(const TFileSystemPath& path, TAtomicSharedPtr<const TMetaInfo> metaInfo) const override {
        Y_UNUSED(path);
        return MakeSimpleShared<TIFStream>(Dir_ / metaInfo->FileId);
    }

private:
    TFsPath Dir_;
};


}
