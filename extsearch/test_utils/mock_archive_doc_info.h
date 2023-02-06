#pragma once

#include <kernel/search_daemon_iface/cntintrf.h>
#include <kernel/urlid/doc_handle.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace NGeosearch::NTestUtils {

    class TMockArchiveDocInfo: public IArchiveDocInfo {
    public:
        TMockArchiveDocInfo();
        virtual ~TMockArchiveDocInfo();

        MOCK_METHOD(TString, DocTitle, (), (const, override));
        MOCK_METHOD(TString, DocHeadline, (), (const, override));
        MOCK_METHOD(int, DocPassageCount, (), (const, override));
        MOCK_METHOD(TString, DocPassage, (int), (const, override));
        MOCK_METHOD(bool, DocPassageAttrs, (int, TString*), (const, override));
        MOCK_METHOD(void, SerializeFirstStageAttribute, (const char*, IAttributeWriter&), (const, override));
        MOCK_METHOD(void, SerializeAttribute, (const char*, IAttributeWriter&), (const, override));
        MOCK_METHOD(TString, DocUrl, (int), (const, override));
        MOCK_METHOD(long, DocSize, (int), (const, override));
        MOCK_METHOD(TString, DocCharset, (int), (const, override));
        MOCK_METHOD(time_t, DocMTime, (int), (const, override));
        MOCK_METHOD(TDocHandle, DocHandle, (), (const, override));
        MOCK_METHOD(int, DocPropertyCount, (const char*), (const, override));
        MOCK_METHOD(bool, ReadDocProperty, (TStringBuf, ui32, TString*), (const, override));
        MOCK_METHOD(int, DocPropertyNameCount, (), (const, override));
        MOCK_METHOD(TString, DocPropertyName, (unsigned), (const, override));
        MOCK_METHOD(ui32, DocIndexGeneration, (), (const, override));
        MOCK_METHOD(ui32, DocSourceTimestamp, (), (const, override));
        MOCK_METHOD(bool, SerializeDocFactor, (IDocFactorSerializer*, IAttributeWriter&), (const, override));

        MOCK_METHOD(size_t, GetAllFactors, (float*, size_t), (const, override));
        MOCK_METHOD(TString, GetBinaryField, (size_t), (const, override));
    };

} // namespace NGeosearch::NTestUtils
