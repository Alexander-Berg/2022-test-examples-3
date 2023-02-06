#pragma once

#include <kernel/search_daemon_iface/cntintrf.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace NGeosearch::NTestUtils {

    class TMockReqEnv: public IReqEnv {
    public:
        TMockReqEnv();
        virtual ~TMockReqEnv();

        MOCK_METHOD(int, FormFieldTest, (TStringBuf, TStringBuf), (const, override));
        MOCK_METHOD(int, FormFieldCount, (TStringBuf key), (const, override));
        MOCK_METHOD(const char*, FormField, (TStringBuf, int), (override));
        MOCK_METHOD(void, FormFieldInsert, (TStringBuf, TStringBuf), (override));
        MOCK_METHOD(void, FormFieldRemove, (TStringBuf, int), (override));
        MOCK_METHOD(const char*, Environment, (const char*), (override));
        MOCK_METHOD(const TString*, HeaderIn, (TStringBuf), (final));
        MOCK_METHOD(size_t, HeadersCount, (), (override));
        MOCK_METHOD(const char*, HeaderByIndex, (size_t), (override));
        MOCK_METHOD(IClientRequestAdjuster*, GetClientRequestAdjuster, (const char*), (override));
        MOCK_METHOD(const char*, QueryString, (), (override));
        MOCK_METHOD(const char*, SearchUrl, (), (override));
        MOCK_METHOD(const char*, SearchPageUrl, (int, const char*), (override));
        MOCK_METHOD(const char*, HighlightedDocUrl, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(const char*, HitReportUrl, (), (override));
        MOCK_METHOD(void, PrintS, (const char*, size_t), (override));
        MOCK_METHOD(void, Print, (const google::protobuf::Message&), (override));
        MOCK_METHOD(const char*, ConvertArchiveText, (const char*, unsigned, int, const char*, const char*, const char*, const char*, const char*, const char*), (override));
        MOCK_METHOD(const TString&, ClientDocServerDescr, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(const char*, ClientDocServerIcon, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(const char*, ClientStat, (), (override));
        MOCK_METHOD(int, ClientBadCount, (), (override));
        MOCK_METHOD(size_t, GetClientsCount, (), (const, override));
        MOCK_METHOD(size_t, BaseSearchCount, (), (const, override));
        MOCK_METHOD(size_t, BaseSearchNotRespondCount, (), (const, override));
        MOCK_METHOD(size_t, NotRespondedSourcesCount, (), (const, override));
        MOCK_METHOD(size_t, FailedPrimusCount, (), (const, override));
        MOCK_METHOD(const char*, NotRespondedSourceName, (size_t), (const, override));
        MOCK_METHOD(const char*, FailedPrimus, (size_t), (const, override));
        MOCK_METHOD(int, NotRespondedClientsCount, (const char*), (const, override));
        MOCK_METHOD(int, IncompleteClientsCount, (const char*), (const, override));
        MOCK_METHOD(int, NotGatheredBaseSearchAnswers, (const char*), (const, override));
        MOCK_METHOD(const IArchiveDocInfo*, GetArchiveAccessor, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(ui64, RequestBeginTime, (), (const, override));
        MOCK_METHOD(IFactorSerializerFactory*, GetFactorSerializerFactory, (), (override));
        MOCK_METHOD(size_t, ChildrenSuperMindsCount, (), (const, override));
        MOCK_METHOD(void, ChildSuperMind, (size_t, TDocRoute&, float&, bool&), (const, override));
    };

} // namespace NGeosearch::NTestUtils
