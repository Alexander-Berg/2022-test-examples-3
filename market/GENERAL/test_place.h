#pragma once

#include <market/report/src/place/search_place.h>
#include <market/report/library/iterator/iterator.h>

class Context;

namespace NMarketReport {

class TReportCgiParams;
class TReportConfig;
class TCollectionSelector;

namespace NRecom {

namespace NDetails {

class TTestPlace : public TSearchPlace {
    using TBase = TSearchPlace;

public:
    using TBase::TBase;
    THolder<NOutput::IRenderer> Output(Context& ctx) const final;

protected:
    bool IsSearchNeeded() const final;
    void SelectCollections(TCollectionSelector& collectionSelector) const override;

private:
    TVector<Document> Explore(const TVector<THyperCategoryId>& hids, Context& ctx) const;
};


class TTestCategoryPlace : public TSearchPlace {
    using TBase = TSearchPlace;

public:
    using TBase::TBase;
    THolder<NOutput::IRenderer> Output(Context& ctx) const final;

protected:
    bool IsSearchNeeded() const final;
    void SelectCollections(TCollectionSelector&) const final;
};

} // namespace NDetails

} // namespace NRecom

} // namespace NMarketReport
