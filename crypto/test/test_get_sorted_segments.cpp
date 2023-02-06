#include <crypta/siberia/bin/custom_audience/suggester/bin/service/lib/suggester_server/get_sorted_segments.h>

#include <library/cpp/iterator/enumerate.h>
#include <library/cpp/iterator/zip.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NSiberia::NCustomAudience::NSuggester;

namespace {
    void CompareItems(
        const ::google::protobuf::RepeatedPtrField<TItem>& reference,
        const ::google::protobuf::RepeatedPtrField<TItem>& result
    ) {
        EXPECT_EQ(reference.size(), result.size());
        for (const auto& [i, items]: Enumerate(Zip(reference, result))) {
            const auto& [referenceItem, resultItem] = items;
            EXPECT_EQ(referenceItem.GetText(), resultItem.GetText()) << "Text is different at " << i;
            EXPECT_EQ(referenceItem.GetDescription(), resultItem.GetDescription()) << "Description is different at " << i;
        }
    }
}

TEST(GetSortedSegments, Simple) {
    TString requestText("рестораны");
    TString nonPrefixRequestText = "сверх" + requestText;
    TString titleName(requestText);
    titleName.to_title();

    TVector<TDatabaseState::TSegmentItem> segments;

    TDatabaseState::TSegmentItem item;

    const auto& addItem = [&](const TString& name, const TString& description) {
        item.LangInfo["ru"] = TDatabaseState::TSegmentItem::TLangSpecificInfo(name, description);

        auto* newExport = item.Exports.Add();
        newExport->SetKeywordId(602);
        newExport->SetSegmentId(123);

        segments.push_back(item);
    };

    addItem("name", "description: " + requestText);
    addItem("name", nonPrefixRequestText);
    addItem(nonPrefixRequestText, "description");
    addItem("name: " + requestText, "description");
    addItem(titleName, "description");

    const auto maxItems = 5;
    const auto& result = GetSortedSegments(segments, requestText, maxItems, NRawData::TExport::all);

    ::google::protobuf::RepeatedPtrField<TItem> reference;
    const auto& addReferenceItem = [&](const TString& name, const TString& description) {
        auto& item = *reference.Add();
        item.SetText(name);
        item.SetDescription(description);
    };

    addReferenceItem("name: " + requestText, "description");
    addReferenceItem(titleName, "description");
    addReferenceItem("name", "description: " + requestText);
    addReferenceItem(nonPrefixRequestText, "description");
    addReferenceItem("name", nonPrefixRequestText);

    CompareItems(reference, result);
}

TEST(GetSortedSegments, FilterByCampaign) {
    const TString requestText("restaurants");
    const ui64 keywordId = 601;
    const auto maxItems = 5;

    TVector<TDatabaseState::TSegmentItem> segments;

    TDatabaseState::TSegmentItem item;
    item.LangInfo["ru"] = TDatabaseState::TSegmentItem::TLangSpecificInfo(requestText, requestText);

    const auto& addExport = [&](ui64 keywordId, ui64 segmentId) {
        auto* newExport = item.Exports.Add();
        newExport->SetKeywordId(keywordId);
        newExport->SetSegmentId(segmentId);
    };

    const i64 allSegment = 100;
    const i64 perfSegment = 200;

    addExport(keywordId, allSegment);
    item.CampaignTypes = {NRawData::TExport::performance, NRawData::TExport::media};
    segments.push_back(item);

    item.Exports.Clear();
    addExport(keywordId, perfSegment);
    item.CampaignTypes = {NRawData::TExport::performance};
    segments.push_back(item);

    const auto& checkSegment = [](const auto& result, const auto& index, const auto& segmentId) {
        EXPECT_EQ(1, result.at(index).GetExports().size());
        EXPECT_EQ(segmentId, result.at(index).GetExports().at(0).GetSegmentId());
    };

    const auto& perfResult = GetSortedSegments(segments, requestText, maxItems, NRawData::TExport::performance);
    ASSERT_EQ(2, perfResult.size());
    checkSegment(perfResult, 0, allSegment);
    checkSegment(perfResult, 1, perfSegment);

    const auto& mediaResult = GetSortedSegments(segments, requestText, maxItems, NRawData::TExport::media);
    ASSERT_EQ(1, mediaResult.size());
    checkSegment(mediaResult, 0, allSegment);
}

TEST(GetSortedSegments, Locale) {
    TVector<TDatabaseState::TSegmentItem> segments;

    TDatabaseState::TSegmentItem item;

    const auto& addItem = [&](const TString& ruName, const TString& enName) {
        item.LangInfo["ru"] = TDatabaseState::TSegmentItem::TLangSpecificInfo(ruName, "");
        item.LangInfo["en"] = TDatabaseState::TSegmentItem::TLangSpecificInfo(enName, "");

        auto* newExport = item.Exports.Add();
        newExport->SetKeywordId(602);
        newExport->SetSegmentId(123);

        segments.push_back(item);
    };

    addItem("Рестораны", "Restaurants");
    addItem("Столовые", "Diners");

    const auto maxItems = 5;
    const auto& ruResult = GetSortedSegments(segments, "сто", maxItems, NRawData::TExport::all, "ru");

    ::google::protobuf::RepeatedPtrField<TItem> reference;
    const auto& addReferenceItem = [&](const TString& name) {
        auto& item = *reference.Add();
        item.SetText(name);
    };

    addReferenceItem("Столовые");
    addReferenceItem("Рестораны");
    CompareItems(reference, ruResult);

    reference.Clear();
    addReferenceItem("Restaurants");

    const auto& enResult = GetSortedSegments(segments, "rest", maxItems, NRawData::TExport::all, "en");
    CompareItems(reference, enResult);

    const auto& badResult = GetSortedSegments(segments, "сто", maxItems, NRawData::TExport::all, "en");
    EXPECT_TRUE(badResult.empty());
}
