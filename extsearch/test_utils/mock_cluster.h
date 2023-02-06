#pragma once

#include <kernel/search_daemon_iface/cntintrf.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <util/generic/maybe.h>

namespace NGeosearch::NTestUtils {

    class TMockCluster: public ICluster {
    public:
        TMockCluster();
        virtual ~TMockCluster();

        MOCK_METHOD(TCateg, CurCateg, (const TGroupingIndex&), (const, override));
        MOCK_METHOD(TStringBuf, CurCategStr, (const TGroupingIndex&), (override));
        MOCK_METHOD(int, GroupsOnPageCount, (const TGroupingIndex&), (const, override));
        MOCK_METHOD(int, DocsInGroupCount, (const TGroupingIndex&), (const, override));
        MOCK_METHOD(int, DocsInHeadGroupsCount, (const TGroupingIndex&), (const, override));
        MOCK_METHOD(ui64, TotalDocCount, (int), (override));
        MOCK_METHOD(int, GroupingSize, (const TGroupingIndex&), (override));
        MOCK_METHOD(ui64, GroupingDocCount, (int, const TGroupingIndex&), (override));
        MOCK_METHOD(ui64, GroupingGroupCount, (int, const TGroupingIndex&), (override));
        MOCK_METHOD(int, HitCount, (), (override));
        MOCK_METHOD(TCateg, GroupCateg, (int, const TGroupingIndex&), (override));
        MOCK_METHOD(TStringBuf, GroupCategStr, (int, const TGroupingIndex&), (override));
        MOCK_METHOD(ui64, GroupDocCount, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(TRelevance, GroupRelevance, (int, const TGroupingIndex&), (override));
        MOCK_METHOD(int, GroupPriority, (int, const TGroupingIndex&), (override));
        MOCK_METHOD(size_t, GroupSize, (int, const TGroupingIndex&), (override));
        MOCK_METHOD(bool, HasDocInfo, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(int, DocPriority, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(int, DocInternalPriority, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(TRelevance, DocRelevance, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(TMaybe<TRelevPredict>, DocRelevPredict, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(ui32, DocHitCount, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(const char*, PassageBreaks, (int, int, const TGroupingIndex&), (override));
        MOCK_METHOD(int, DocCategCount, (int, int, const TGroupingIndex&, const char*), (override));
        MOCK_METHOD(TCateg, DocCategId, (int, int, const TGroupingIndex&, const char*, unsigned), (override));
        MOCK_METHOD(int, ReqAttrFunc, (const char*), (const, override));
        MOCK_METHOD(unsigned, GetSearchPropertyCount, (unsigned), (const, override));
        MOCK_METHOD(TStringBuf, GetSearchPropertyName, (unsigned, unsigned), (const, override));
        MOCK_METHOD(TStringBuf, GetSearchPropertyValue, (unsigned, const char*), (const, override));
        MOCK_METHOD(unsigned, GetMainSearchPropertyCount, (), (const, override));
        MOCK_METHOD(TStringBuf, GetMainSearchPropertyName, (unsigned), (const, override));
        MOCK_METHOD(TStringBuf, GetMainSearchPropertyValue, (const char*), (const, override));
    };

} // namespace NGeosearch::NTestUtils
